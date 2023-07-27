#!/bin/sh

/configure.sh ${ZOOKEEPER_SERVICE_HOST:-$1} ${NIMBUS_SERVICE_HOST:-$2}
# update redis server ip
if [ $SLOT_NUM -eq 8 ]
then

cat >> conf/storm.yaml <<EOF
supervisor.slots.ports:
    - 6700
    - 6701
    - 6702
    - 6703
    - 6704
    - 6705
    - 6706
    - 6707
supervisor.scheduler.meta:
  tags: $TAG_NAME
EOF

else

cat >> conf/storm.yaml <<EOF
supervisor.slots.ports:
    - 6700
    - 6701
supervisor.scheduler.meta:
  tags: $TAG_NAME
EOF

fi

exec bin/storm supervisor
