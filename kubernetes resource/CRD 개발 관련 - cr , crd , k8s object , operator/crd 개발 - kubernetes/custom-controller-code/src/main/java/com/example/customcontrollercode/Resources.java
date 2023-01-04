package com.example.customcontrollercode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.customcontrollercode.CrCrdModel.V1Helloworld;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceSpec;

public class Resources {


    public V1Deployment createDeployment(V1Helloworld resourceInstance) {
        V1Deployment deploymentSet = new V1Deployment();
        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec();
        String applanguageInfo = resourceInstance.getSpec().getLanguage().toString();

        deploymentSpec.template(podTemplate(resourceInstance, applanguageInfo)); // pod template 들어감
        deploymentSpec.replicas(resourceInstance.getSpec().getReplicas());

        deploymentSet.setMetadata( 
                new V1ObjectMeta() 
                        .name(resourceInstance.getMetadata().getName())
                        .labels( // deployment lable 지정
                                Map.of(
                                        "app", applanguageInfo,
                                        "message", resourceInstance.getSpec().getMessage()
                                    )
                                )
                    );

        deploymentSpec.selector(new V1LabelSelector() // deployment  pod selector 지정
                        .matchLabels(
                        Map.of(
                                "app", applanguageInfo,
                                "message", resourceInstance.getSpec().getMessage()
                            )
                        )
                        
                    );

        deploymentSet.setSpec(deploymentSpec);
        
        System.out.println("deployment spec init com");
        return deploymentSet;
    }

    // deployment pod template 생성
    private static V1PodTemplateSpec podTemplate(V1Helloworld resourceInstance, String applanguageInfo) {
        System.out.println("hello im V1PodTemplate");
        V1ObjectMeta podMeta = new V1ObjectMeta();
        V1PodTemplateSpec podTemplateSpec = new V1PodTemplateSpec();

        List<V1Container> podContainers = new ArrayList<V1Container>();
        

        podContainers.add(0,createContainers(resourceInstance));

        podMeta.labels( // pod label 지정 ( required )
                Map.of(
                        "app", applanguageInfo,
                        "message", resourceInstance.getSpec().getMessage()
                    )
                );

        podMeta.setName("hello" + "-" + UUID.randomUUID()); // pod name 지정 ( required )

        podTemplateSpec.setMetadata(podMeta);
        podTemplateSpec.spec(new V1PodSpec().containers(podContainers)); 


        return podTemplateSpec;
    }

        // pod container 정보 생성
    private static V1Container createContainers(V1Helloworld resourceInstance){
        V1Container container = new V1Container();
        container.setImage(resourceInstance.getSpec().getImage());
        container.setName(resourceInstance.getSpec().getAppId());
        
        return container;
    }

    private static V1Service creatService(V1Helloworld resourceInstance){
        V1Service service = new V1Service();
        V1ServiceSpec serviceSpec = new V1ServiceSpec();

        serviceSpec.setType("NodePort");

        service.setSpec(serviceSpec);
        return service;
    }

}
