#!/bin/bash

# build Apache Storm images for Docker
# change to your username
username="kevin2333"

docker image rm -f `docker image ls | grep ${username} | awk '{print $3}'`
docker image rm -f `docker image ls | grep none | awk '{print $3}'`

docker build -t ${username}/storm-base base
base_tag=`docker image ls | grep ${username}/storm | grep latest | grep base | awk '{print $3}'`
docker tag $base_tag ${username}/storm-base:latest
docker push ${username}/storm-base:latest

# 
docker build -t ${username}/storm-nimbus nimbus
docker build -t ${username}/storm-worker worker
docker build -t ${username}/storm-ui ui 

nimbus_tag=`docker image ls | grep ${username}/storm | grep latest | grep nimbus | awk '{print $3}'`
ui_tag=`docker image ls | grep ${username}/storm | grep latest | grep ui | awk '{print $3}'`
worker_tag=`docker image ls | grep ${username}/storm | grep latest | grep worker | awk '{print $3}'`

docker tag $nimbus_tag ${username}/storm-nimbus:latest
docker tag $ui_tag ${username}/storm-ui:latest
docker tag $worker_tag ${username}/storm-worker:latest

docker push ${username}/storm-nimbus:latest
docker push ${username}/storm-ui:latest
docker push ${username}/storm-worker:latest
