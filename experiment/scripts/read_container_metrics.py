import datetime
import time
import requests  
import sys

PROMETHEUS = 'http://localhost:9090/'

def timeToTimestamp(string):
    element = datetime.datetime.strptime(string,"%H:%M %d/%m/%Y")
    timestamp = datetime.datetime.timestamp(element)
    return timestamp

dataTime = {
    # the time should be minus 6 hours.
    "Beaver": ["17:10 04/11/2023",
               "19:10 04/11/2023"],
    "Amnis":  ["01:20 19/10/2023",
               "03:20 19/10/2023"],
    "Coda":   ["03:50 19/10/2023",
               "05:50 19/10/2023"],
    "RStorm": ["06:20 19/10/2023", 
               "08:20 19/10/2023"],
    "Storm":  ["08:50 19/10/2023",
               "10:50 19/10/2023"],
}

def request(name, query):
    params = {
        "query": query,
        "start": timeToTimestamp(dataTime[name][0]),
        "end": timeToTimestamp(dataTime[name][1]),
        "step": 60, 
    }
    response =requests.get(PROMETHEUS + '/api/v1/query_range', params=params)
    results = response.json()['data']['result']
     
    pod_name = "pod"
    for result in results:
        if "instance" in query:
            pod_name = "instance"
        metric = result['metric'][pod_name]
        values = result['values']
        values = [float(b) for a, b in values]
        print(metric+"::"+str(values))
        print(len(values))

def readMetrics(name):
    query_lst = [
        'sum (rate (container_cpu_usage_seconds_total{pod=~"storm-.*", namespace="default", pod!~"storm-master.*|storm-ui.*"}[5m])) by (pod)',
        'sum (rate (container_network_transmit_bytes_total{pod=~"storm-.*", namespace="default", pod!~"storm-master.*|storm-ui.*"}[5m])) by (pod)',
        'sum (rate (container_network_receive_bytes_total{pod=~"storm-.*", namespace="default", pod!~"storm-master.*|storm-ui.*"}[5m])) by (pod)',
        'sum (rate (container_memory_usage_bytes{pod=~"storm-.*", namespace="default", pod!~"storm-master.*|storm-ui.*"}[5m])) by (pod)',
        'instance:node_cpu:ratio{ instance!="master"}',
        'instance:node_network_receive_bytes:rate:sum{ instance!="master"}',
        'instance:node_network_transmit_bytes:rate:sum{ instance!="master"}',
        'instance:node_memory_utilisation:ratio{ instance!="master"}',
]
    for query in query_lst:
        request(name, query)

NAME = sys.argv[1]
START_TIME= sys.argv[2]
END_TIME=sys.argv[3]
if START_TIME and END_TIME:
    dataTime[name][0] = START_TIME
    dataTime[name][1] = END_TIME

readMetrics(NAME)
