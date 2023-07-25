FROM ubuntu:16.04

MAINTAINER Peng Kang<123kangpeng@gmail.edu>

RUN apt-get update -y

RUN apt-get install mosquitto -y

COPY passwd /etc/mosquitto/
COPY config-run.sh /usr/sbin/
RUN chown root.root /usr/sbin/config-run.sh
RUN chmod 700 /usr/sbin/config-run.sh

EXPOSE 1883

WORKDIR /usr/sbin

ENTRYPOINT ["/usr/sbin/config-run.sh"]
