FROM ubuntu:16.04

MAINTAINER Peng Kang<peng.kang@utsa.edu>

RUN apt-get update && apt-get install -y --no-install-recommends apt-utils

RUN apt-get install openjdk-8-jre-headless -y

RUN apt-get install curl tar vim -y

RUN apt-get install python -y

RUN mkdir -p /home/cc
ADD storm /home/cc/storm

ADD configure.sh /
