#!/bin/bash

LOCAL_MANIFEST_FILE=~/tmp/jjscrd/mycrd.yml

mkdir -p /tmp/java && cd /tmp/java

docker run \
  --rm \
  -v "$LOCAL_MANIFEST_FILE":"$LOCAL_MANIFEST_FILE" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$(pwd)":"$(pwd)" \
  -ti \
  --network host \
  ghcr.io/kubernetes-client/java/crd-model-gen:v1.0.6 \
  /generate.sh \
  -u "$LOCAL_MANIFEST_FILE" \
  -n com.example.jjsair0412 \
  -p com.example.customcontrollercode \
  -o "$(pwd)"
