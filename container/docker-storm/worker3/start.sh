#!/bin/sh

/configure.sh ${ZOOKEEPER_SERVICE_HOST:-$1} ${NIMBUS_SERVICE_HOST:-$2}

cat >> /opt/apache-storm/conf/storm.yaml <<EOF
    - 6701
    - 6702
EOF

exec bin/storm supervisor
