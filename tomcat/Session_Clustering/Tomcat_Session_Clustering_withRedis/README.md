# Tomcat Session Clustering with Redis
## Precondition
해당 문서는 Redis 서버를 통해서 Tomcat Session clustering을 구현하는 방안에 대해 기술한 문서입니다.

## Testing Environment
Vagrant를 통해 3개 노드로 구성하였습니다.

```vagrantfile
# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure("2") do |config|
  config.vm.provider :virtualbox do |vb|
    vb.gui = true
  end
  config.vm.boot_timeout = 1000
  config.vm.define "redis" do |redis|

    redis.vm.box = "ubuntu/focal64"
    redis.vm.host_name = "redis"
    redis.vm.network "private_network", ip: "192.168.50.10"
    redis.vm.provider :virtualbox do |redisSpec|
      redisSpec.memory = 4096
      redisSpec.cpus = 4
    end
  end

  (1..2).each do |n|
    config.vm.define "tomcat-#{n}" do |tomcat|
      tomcat.vm.box = "ubuntu/focal64"
      tomcat.vm.host_name = "tomcat-#{n}"
      tomcat.vm.network "private_network", ip: "192.168.50.1#{n}"
      tomcat.vm.provider :virtualbox do |tomcatSpec|        
        tomcatSpec.memory = 4096
        tomcatSpec.cpus = 4
      end
    end
  end
end
```