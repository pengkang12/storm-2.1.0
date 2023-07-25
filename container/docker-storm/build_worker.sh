# Apache Storm images for Docker
docker build -t kevin2333/storm-worker3 worker3

worker_tag=`docker image ls | grep kevin2333/storm | grep latest | grep worker3 | awk '{print $3}'`

echo $worker_tag

docker tag $worker_tag kevin2333/storm-worker3:latest

docker push kevin2333/storm-worker3:latest

docker build -t kevin2333/storm-worker5 worker5

worker_tag=`docker image ls | grep kevin2333/storm | grep latest | grep worker5 | awk '{print $3}'`

echo $worker_tag

docker tag $worker_tag kevin2333/storm-worker5:latest

docker push kevin2333/storm-worker5:latest
