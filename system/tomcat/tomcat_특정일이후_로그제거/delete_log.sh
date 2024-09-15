#!/bin/bash

TOMCAT_LOG=/log/catalina.out.*
DATE='date +%Y_%m_%d'
find $TOMCAT_LOG -mtime +10 -type f -exec rm -f {} \;
echo instance log delete comm