#!bin/bash

result=$(echo "$(df -h | awk '/data/ {print $5}')" | sed 's/%//g')

if [ $result -gt 50 ]
then
    TOMCAT_LOG=/data/logs/instance/*.log
    find $TOMCAT_LOG -mtime +5 -type f -exec rm -f {} \;
    echo instance1${CNT} log delete comm
fi