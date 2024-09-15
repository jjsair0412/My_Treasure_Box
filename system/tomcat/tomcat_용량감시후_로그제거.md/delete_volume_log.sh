#!bin/bash

VALUE=$(echo "$(df -h | awk '/data/ {print $5}')" | sed 's/%//g')

if [ $VALUE -gt 50 ]
then
    TOMCAT_LOG=/log/instance/*.log
    find $TOMCAT_LOG -mtime +1 -type f -exec rm -f {} \;
    echo instance log delete comm
fi