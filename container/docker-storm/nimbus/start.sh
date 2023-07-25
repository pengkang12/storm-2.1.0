#!/bin/sh

/configure.sh ${ZOOKEEPER_SERVICE_HOST:-$1}

home_path="home/cc"

cat >> conf/storm.yaml <<EOF
storm.scheduler: "sys.cloud.tagawarescheduler.TagAwareScheduler"
EOF

cat >> riot-bench/modules/tasks/src/main/resources/tasks.properties <<EOF
IO.MQTT_PUBLISH.APOLLO_URL=tcp://${MOSQUITTO1_SERVICE_HOST:-$1}:1883
IO.MQTT_SUBSCRIBE.APOLLO_URL=tcp://${MOSQUITTO1_SERVICE_HOST:-$1}:1883
EOF

cat >> riot-bench/modules/tasks/src/main/resources/tasks_TAXI.properties <<EOF
IO.MQTT_PUBLISH.APOLLO_URL=tcp://${MOSQUITTO1_SERVICE_HOST:-$1}:1883
IO.MQTT_SUBSCRIBE.APOLLO_URL=tcp://${MOSQUITTO1_SERVICE_HOST:-$1}:1883
EOF

exec bin/storm nimbus
