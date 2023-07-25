#!/bin/sh

/configure.sh ${ZOOKEEPER_SERVICE_HOST:-$1} ${NIMBUS_SERVICE_HOST:-$2}
# update redis server ip

cat >> conf/storm.yaml <<EOF
supervisor.scheduler.meta:
  tags: $TAG_NAME
EOF

exec bin/storm supervisor
