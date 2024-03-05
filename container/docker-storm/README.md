### Apache Storm images for Docker

* Linux version: ubuntu 16.04

### Attention
* to build Apache storm image, you need to copy the whole content _$HOME/storm_ to _base_ directory

### Build Container one by one
* ```docker build -t <name>/storm-base base```
* ```docker build -t <name>/storm-nimbus nimbus```
* ```docker build -t <name>/storm-worker worker```

### Build all by script, when you use this script, please change username to your username. 
* ```bash build_all.sh```