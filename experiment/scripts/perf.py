#!/bin/python3
import requests
import sys
import time
import os
import json

app=sys.argv[1]

import redis

# step 2: define our connection information for Redis
# Replaces with your configuration information
redis_host = "master"
redis_port = 6379
redis_password = ""

bucket = [1, 2, 4, 8, 16,
 32, 64, 128, 256, 512,
 1024, 2048, 4096, 8192, 16384, 
32768, 60000, 120000]

def get_power(number):
     for i in range(len(bucket)):
         if number <= bucket[i]: 
             return i 
     

def calculate_latency(appName="ETLTopologySYS"):
    """calculate latency from Redis data
    https://blog.bramp.net/post/2018/01/16/measuring-percentile-latency/
    """
    successRate = 0.0
    throughput = 0 
    tail_latency = 0
    average = 0
    try:
        r = redis.StrictRedis(host=redis_host, port=redis_port, password=redis_password, decode_responses=True)
        # calculate latency for 1 minute.
        timestamp = int(time.time())
        remain = timestamp % 60 
	    timestamp = timestamp - remain - 120
        timestamp = str(timestamp) 

        start = timeit.default_timer()
        spout = r.hgetall(appName+"_spout_"+timestamp)
        stop = timeit.default_timer()
        print('Transfer array for storing tuple start time: ', stop - start)  
        start = timeit.default_timer()
 
        # use pipeline to improve redis effiency. 
        sink = r.hgetall(appName+"_sink_"+timestamp)
	    timestamp2 = str(int(timestamp) + 60)
        sink2 = r.hgetall(appName+"_sink_"+timestamp2) 
        stop = timeit.default_timer()
        print('Transferarray for storing tuple end time: ', stop - start)  
        start = timeit.default_timer()
        
        total_latency = 0
        latency_bucket = [ 0 for i in range(len(bucket))]
	    for key, value in spout.items():
            word = key.split("_")
	    # message example: MSGID_1923232323"
            if word[1] in sink:
		# start data and end data at same minute interval
                new_latency = int(sink[word[1]])-int(value)
	    elif word[1] in sink2:
		# end data at next minute interval.
                new_latency = int(sink2[word[1]])-int(value) + 60000 
            else:
                continue
            if new_latency >= 60000 or new_latency <= 0:
                continue
            total_latency += new_latency
            index = get_power(new_latency)
            latency_bucket[index] += 1

        stop = timeit.default_timer()
        print('calculate end to end time: ', stop - start)  
        count = 0
        throughput = len(spout)
        if throughput > 0:
            successRate = sum(latency_bucket[:11])
            average = total_latency / sum(latency_bucket) if sum(latency_bucket) > 0 else 60000 

        tail_latency = 60000
        if throughput == 0:
	    tail_latency = 0
	else:
            for i in range(len(bucket)):
            	count += latency_bucket[i]
		latency_bucket[i] = count
                if count*1.0/throughput >=0.95:
		    if i == 0:
                        tail_latency = 1
                    elif i < len(bucket):
                    	tail_latency = bucket[i-1] + (bucket[i] - bucket[i-1])*(0.95*throughput - latency_bucket[i-1])/(latency_bucket[i] - latency_bucket[i-1]+1)
       		    else:
			tail_latency = 60000
		    break
        print("some result: ", tail_latency, throughput, latency_bucket)
        # delete the data from redis	
        keys = r.keys(appName+"_sink_"+timestamp)
        keys = r.keys(appName+"_spout_"+timestamp)
        r.delete(*keys)

    except Exception as e:
        print(e)
    return tail_latency, throughput, successRate, average


url = "http://localhost:8081/api/v1/topology/"

def statistic_info(app_id):
    result = {}

    r = requests.get(url+app_id)
    data = r.json()
    #print(data)
    print("\nstart experiment------------------------------")
    total_execute = 0
    total_capacity = 0 
    bolts_capacity = {}
    sink_data = {}
    for each in data['bolts']:
        # sink may influence our results. Therefore, we don't use sink as metrics.  
        if 'sink' in each['boltId']:
            sink_data = each
            continue
        total_capacity += float(each['capacity'])
        bolts_capacity[each['boltId']] = float(each['capacity'])
    #collect container cpu usage for each minute. we should calculate cpu usage, then run collect_container_cpu.py to produce new data for next minute. 
    cpu = {}
    count = 0
   
    # calculate cpu usage for application's worker .
    app_cpu = {}
    capacity_ratio = {}
    for each in data['workers']:
        capacity = 0
        print("{0} {1}".format(each['host'], each['componentNumTasks']))
        for component in each['componentNumTasks'].keys():
            if component in bolts_capacity:
                capacity += bolts_capacity[component]
        if total_capacity == 0:
            return

    print("The name of application is {0}, count is {1}".format(data['name'], count))
      
    result['latency'], result['throughput'], result['successRate'], result['average'] = calculate_latency(data['name'])
    # result['throughput'] = sink_data["executed"]
    with open('/tmp/skopt_input_{0}.txt'.format(data['name']), 'a+') as f: 
        f.write(json.dumps(result)+"\n")
    print("result is ", result)

    switch = False
    for each in data['topologyStats']:
        if each['window'] == "600":
            switch = True
    if switch== False:
        pass 

def getTopologySummary():
    r = requests.get(url+"summary")
    data = r.json()
    for each in data['topologies']:
        if app in each['id']:
            statistic_info(each['id'])

import timeit

start = timeit.default_timer()


getTopologySummary()
#os.system("python BO/bayesian_optimization.py")
if app == "IoT":
    #os.system("python BO/bayesian_optimization.py >> /tmp/bo.log")
    pass

stop = timeit.default_timer()

print('total running Time: ', stop - start)  
start = timeit.default_timer()


start = timeit.default_timer()

