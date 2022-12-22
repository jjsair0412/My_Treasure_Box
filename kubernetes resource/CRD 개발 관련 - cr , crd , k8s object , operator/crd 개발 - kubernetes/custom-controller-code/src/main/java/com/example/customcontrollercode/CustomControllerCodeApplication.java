package com.example.customcontrollercode;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.customcontrollercode.CrCrdModel.V1Helloworld;
import com.example.customcontrollercode.CrCrdModel.V1HelloworldList;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.util.generic.GenericKubernetesApi;

@SpringBootApplication
public class CustomControllerCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomControllerCodeApplication.class, args);
    }

    @Bean
    SharedIndexInformer<V1Helloworld> sharedIndexInformer(SharedInformerFactory sharedInformerFactory,
            ApiClient apiClient) {
        GenericKubernetesApi<V1Helloworld, V1HelloworldList> api = new GenericKubernetesApi<>(V1Helloworld.class,
                V1HelloworldList.class,
                "jjsair0412.example.com", // CRD Group
                "v1", // CRD version
                "helloworlds", // CRD Plural name
                apiClient);
        return sharedInformerFactory.sharedIndexInformerFor(api, V1Helloworld.class, 0);
    }

    // AppsV1Api가 k8s api server와 통신하여 k8s resource 관리 수행
    @Bean
    Reconciler reconciler(SharedIndexInformer<V1Helloworld> shareIndexInformer,
            AppsV1Api appsV1Api) {
        return request -> {
            String key = request.getNamespace() + "/" + request.getName();
            

            V1Helloworld resourceInstance = shareIndexInformer
                    .getIndexer()
                    .getByKey(key);

            if (resourceInstance != null) {

                V1Deployment v1Deployment = createDeployment(resourceInstance);
                System.out.println("Creating resource deployment...");

                try {
                    appsV1Api.createNamespacedDeployment(request.getNamespace(), 
                            v1Deployment, 
                            "true", 
                            null, 
                            "", 
                            "");
                } catch (ApiException e) {
                    System.out.println("Creating resource failed");
                    if (e.getCode() == 409) {
                        System.out.println("Updating resource...");
                        try {
                            
                            appsV1Api.replaceNamespacedDeployment(craeteDeploymentName(resourceInstance), 
                                    request.getNamespace(), 
                                    v1Deployment, 
                                    "true", 
                                    null, 
                                    "", 
                                    "");
                                    
                        } catch (ApiException ex) {
                            throw new RuntimeException(ex);
                        }
                    } else {
                        throw new RuntimeException(e);
                    }
                }
                return new Result(false);
            }
            return new Result(false);

        };
    }
    
    private static V1Deployment createDeployment(V1Helloworld resourceInstance) {
        V1Deployment deploymentSet = new V1Deployment(); 
        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec();

        String applanguageInfo = resourceInstance.getSpec().getLanguage().toString();

        deploymentSpec.template(podTemplate(resourceInstance, applanguageInfo)); // pod template 들어감
        deploymentSpec.replicas(resourceInstance.getSpec().getReplicas());

        deploymentSet.metadata(new V1ObjectMeta()
                                    .name(craeteDeploymentName(resourceInstance))
                                    .labels(Map.of("app",applanguageInfo))
                                    .labels(Map.of("message",resourceInstance.getSpec().getMessage()))
                                );
        
        deploymentSpec.selector(new V1LabelSelector()
                                    .matchLabels(Map.of("app", applanguageInfo))
                                    .matchLabels(Map.of("message",resourceInstance.getSpec().getMessage()))    
                                );
        
        deploymentSet.setSpec(deploymentSpec);
        
        
        return deploymentSet;
    }


    // deployment pod template 생성
    private static V1PodTemplateSpec podTemplate(V1Helloworld resourceInstance , String applanguageInfo){

        String imageName = resourceInstance.getSpec().getImage();

        V1ObjectMeta podMeta = new V1ObjectMeta();
        V1PodTemplateSpec podTemplateSpec = new V1PodTemplateSpec();


        podMeta.labels(Map.of("app", applanguageInfo)); // label app : applanguageInfo 
        podMeta.labels(Map.of("message",resourceInstance.getSpec().getMessage()));

        podMeta.setName(createDeployment(resourceInstance)+"-"+UUID.randomUUID());
        podTemplateSpec.spec(new V1PodSpec().containers(Arrays.asList(new V1Container().name(imageName)))); // image : CR로 생성한 이미지

        return podTemplateSpec;
    }

    // deployment name 생성
    public static String craeteDeploymentName(V1Helloworld resourceInstance){
        return "jjs"+resourceInstance.getSpec().getAppId();
    }
}
