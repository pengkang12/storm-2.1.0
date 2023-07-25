#!/bin/bash

mosquitto_passwd -b /etc/mosquitto/passwd admin password 

exec /usr/sbin/mosquitto -c /etc/mosquitto/mosquitto.conf
