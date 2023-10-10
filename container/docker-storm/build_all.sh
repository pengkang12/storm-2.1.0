# Apache Storm images for Docker
#docker image rm -f `docker image ls | grep kevin2333 | awk '{print $3}'`
#docker image rm -f `docker image ls | grep none | awk '{print $3}'`

docker build -t kevin2333/storm-base base
base_tag=`docker image ls | grep kevin2333/storm | grep latest | grep base | awk '{print $3}'`
docker tag $base_tag kevin2333/storm-base:latest
docker push kevin2333/storm-base:latest

# 
docker build -t kevin2333/storm-nimbus nimbus
docker build -t kevin2333/storm-worker worker
docker build -t kevin2333/storm-ui ui 

nimbus_tag=`docker image ls | grep kevin2333/storm | grep latest | grep nimbus | awk '{print $3}'`
ui_tag=`docker image ls | grep kevin2333/storm | grep latest | grep ui | awk '{print $3}'`
worker_tag=`docker image ls | grep kevin2333/storm | grep latest | grep worker | awk '{print $3}'`

docker tag $nimbus_tag kevin2333/storm-nimbus:latest
docker tag $ui_tag kevin2333/storm-ui:latest
docker tag $worker_tag kevin2333/storm-worker:latest

docker push kevin2333/storm-nimbus:latest
docker push kevin2333/storm-ui:latest
docker push kevin2333/storm-worker:latest
