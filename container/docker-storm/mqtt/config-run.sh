#!/bin/bash

mosquitto_passwd -b /etc/mosquitto/passwd admin password 

exec /usr/sbin/mosquitto -v -c /etc/mosquitto/mosquitto.conf
