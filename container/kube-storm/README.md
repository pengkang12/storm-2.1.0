### Storm example

You will setup an [Apache ZooKeeper](http://zookeeper.apache.org/)
service, a Storm master service (a.k.a. Nimbus server), and a set of
Storm workers (a.k.a. supervisors).

### Sources
Source is freely available at:
* Docker image - https://github.com/kevin2333/EdgeStreamProcessing/docker-storm
* Docker Trusted Build - https://registry.hub.docker.com/search?q=kevin2333/storm


## Build all with one script

setup zookeep, storm nimbus, and storm ui, storm worker

```
bash scripts/setup.sh
```

### Check to see if ZooKeeper is running

```sh
$ kubectl get pods
NAME        READY     STATUS    RESTARTS   AGE
zookeeper   1/1       Running   0          43s
```

### Check to see if ZooKeeper is accessible

```console
$ kubectl get services
NAME              CLUSTER_IP       EXTERNAL_IP       PORT(S)       SELECTOR               AGE
zookeeper         10.254.139.141   <none>            2181/TCP      name=zookeeper         10m
kubernetes        10.0.0.2         <none>            443/TCP       <none>                 1d

$ echo ruok | nc 10.254.139.141 2181; echo
imok
```

## Step Two: Start your Nimbus service

### Check to see if Nimbus is running and accessible

```sh
$ kubectl get services
NAME                LABELS                                    SELECTOR            IP(S)               PORT(S)
kubernetes          component=apiserver,provider=kubernetes   <none>              10.254.0.2          443
zookeeper           name=zookeeper                            name=zookeeper      10.254.139.141      2181
nimbus              name=nimbus                               name=nimbus         10.254.115.208      6627

$ sudo docker run -it -w /opt/apache-storm kevin2333/storm-nimbus sh -c '/configure.sh 10.254.139.141 10.254.115.208; ./bin/storm list'
...
No topologies running.
```

## Step Three: Start your Storm workers

The Storm workers (or supervisors) do the heavy lifting in a Storm cluster. They run your stream processing topologies and are managed by the Nimbus service.

The Storm workers need both the ZooKeeper and Nimbus services to be running.

Use the [`storm-worker-controller.yaml`](storm-worker-controller.yaml) file to create a
[deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/) that manages the worker pods.

```sh
$ kubectl create -f examples/storm/storm-worker-controller.yaml
```

### Check to see if the workers are running

One way to check on the workers is to get information from the
ZooKeeper service about how many clients it has.

```sh
$  echo stat | nc 10.254.139.141 2181; echo
Zookeeper version: 3.4.6--1, built on 10/23/2014 14:18 GMT
Clients:
 /192.168.48.0:44187[0](queued=0,recved=1,sent=0)
 /192.168.45.0:39568[1](queued=0,recved=14072,sent=14072)
 /192.168.86.1:57591[1](queued=0,recved=34,sent=34)
 /192.168.8.0:50375[1](queued=0,recved=34,sent=34)

Latency min/avg/max: 0/2/2570
Received: 23199
Sent: 23198
Connections: 4
Outstanding: 0
Zxid: 0xa39
Mode: standalone
Node count: 13
```

There should be one client from the Nimbus service and one per
worker. Ideally, you should get ```stat``` output from ZooKeeper
before and after creating the replication controller.

(Pull requests welcome for alternative ways to validate the workers)

## tl;dr

```kubectl create -f zookeeper.json```

```kubectl create -f zookeeper-service.json```

Make sure the ZooKeeper Pod is running (use: ```kubectl get pods```).

```kubectl create -f storm-nimbus.json```

```kubectl create -f storm-nimbus-service.json```

Make sure the Nimbus Pod is running.

```kubectl create -f storm-worker-controller.yaml```

test
```
bin/storm jar examples/storm-starter/storm-starter-2.1.0.jar org.apache.storm.starter.WordCountTopology wordCountTopology
```

## pay attention

If we want to runing an application into different pods. We must modify pod's /etc/hosts file to write the information about host and ip. Otherwise, the application can't recognize the hostname. 
