# Apache Storm base image for Docker

download apache storm apache-storm-2.1.0.tar.gz

This image is the base for nimbus and worker images, and a CLI
environment for interacting with a Storm cluster.

# Build

* ```docker build -t <name>/storm-base .```

# Use

* ```docker run -it <name>/storm-base sh```
* ```cd /opt/apache-storm```
* ```/configure.sh <zookeeper_ip> <nimbus_ip>```
* ```./bin/storm ...```
