package com.presignedurl.awss3presignedurl.logic.openstackSwift.config;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class openstackConfig {
    // openstack userId
    @Value("${cloud.openstack.credentials.userId}") // openstack access key
    private String userId;
    // openstack userPwd
    @Value("${cloud.openstack.credentials.userPwd}")
    private String userPwd;

    @Value("${cloud.openstack.credentials.authUrl}")
    private String authUrl;  // e.g., http://localhost:5000/v3

    @Value("${cloud.openstack.credentials.domain}")
    private String domain;  // 대부분의 경우 'Default'를 사용합니다. 환경에 따라 다를 수 있습니다.

    @Value("${cloud.openstack.credentials.projectId}")
    private String projectId; // openstack projectID 입력

    public OSClient.OSClientV3 getOpenstackSwiftClient(){
        return OSFactory.builderV3()
                .endpoint(authUrl)
                .credentials(userId, userPwd, Identifier.byName(domain))
                .scopeToProject(Identifier.byId(projectId))
                .authenticate();
    }
}
