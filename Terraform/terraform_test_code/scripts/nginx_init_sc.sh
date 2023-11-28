#!/bin/bash
sudo apt-get update -y
sudo apt-get install nginx -y

## nginx restart ##
sudo systemctl start nginx