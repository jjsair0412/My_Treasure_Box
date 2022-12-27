package com.example.customcontrollercode;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.customcontrollercode.CrCrdModel.V1Helloworld;
import com.example.customcontrollercode.CrCrdModel.V1HelloworldList;
import com.google.protobuf.Api;

import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.generic.GenericKubernetesApi;

@SpringBootApplication
public class CustomControllerCodeApplication {

    
    public static void main(String[] args) {
        SpringApplication.run(CustomControllerCodeApplication.class, args);
    }

    @Bean
    SharedInformerFactory sharedInformerFactory(ApiClient apiClient){
        return new SharedInformerFactory(apiClient);
    }


    @Bean
    SharedIndexInformer<V1Helloworld> sharedIndexInformer(SharedInformerFactory sharedInformerFactory,
            ApiClient apiClient) {
        System.out.println("hello im sharedIndexInformer method");
        GenericKubernetesApi<V1Helloworld, V1HelloworldList> api = new GenericKubernetesApi<>(V1Helloworld.class,
                V1HelloworldList.class,
                "jjsair0412.example.com", // CRD Group
                "v1", // CRD version
                "helloworlds", // CRD Plural name
                apiClient);
        return sharedInformerFactory.sharedIndexInformerFor(api, V1Helloworld.class, 0);
    }

    

    // AppsV1Api가 k8s api server에 request를 보내어 k8s resource 관리 수행
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
                    appsV1Api.createNamespacedDeployment(
                            request.getNamespace(),
                            v1Deployment,
                            "true",
                            null,
                            "",
                            "");
                } catch (ApiException e) {
                    createErrorCode("createNamespacedDeployment",e);
                    System.out.println("Creating resource failed");
                    if (e.getCode() == 409) { // 생성되어있다면 409 에러 발생 . 생성되어있는데 다시한번 resourceInstance가 들어왔다는건 update
                        
                        System.out.println("Updating resource...");
                        try {
                            appsV1Api.replaceNamespacedDeployment(
                                    request.getName(),
                                    request.getNamespace(),
                                    v1Deployment,
                                    null,
                                    null,
                                    "",
                                    "");
                            
                        } catch (ApiException ex) {
                            createErrorCode("replaceNamespacedDeployment",ex);
                            throw new RuntimeException(ex);
                        }
                    } else {
                        throw new RuntimeException(e);
                    }
                    
                }
                return new Result(false);
            }else{
                try {
                    System.out.println("delete deployment resource...");
                    appsV1Api.deleteNamespacedDeployment(
                            request.getName(), 
                            request.getNamespace(), 
                            null, 
                            null, 
                            null, 
                            null, 
                            null, 
                            null
                            );
                } catch (ApiException de) {
                    createErrorCode("deleteNamespacedDeployment",de);
                    throw new RuntimeException(de);
                }
            }
            return new Result(false);

        };
    }

    // controller , ExcutorService , ApplicationRunner 코드 분석 필요
    // // 아마 셋이서 맞물려서 kube-api-server와 통신하는듯
    @Bean
    Controller controller(SharedInformerFactory shareformerFactory,
            Reconciler reconsiler,
            SharedIndexInformer<V1Helloworld> shareIndexInformer) {
        return ControllerBuilder
                .defaultBuilder(shareformerFactory)
                .watch(contrWatchQueue -> ControllerBuilder
                        .controllerWatchBuilder(V1Helloworld.class, contrWatchQueue)
                        .withResyncPeriod(Duration.of(1, ChronoUnit.SECONDS))
                        .build())
                // .withWorkerCount(2)
                .withReconciler(reconsiler)
                .withReadyFunc(shareIndexInformer::hasSynced)
                .withName("My controller")
                .build();
    }

    @Bean
    ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    ApplicationRunner runner(ExecutorService executorService,
            SharedInformerFactory sharedInformerFactory,
            Controller controller) {
        return args -> executorService.execute(() -> {
            sharedInformerFactory.startAllRegisteredInformers();
            controller.run();
        });
    }


    @Bean
    ApiClient myApiClient() throws IOException {
      ApiClient apiClient = new ApiClient();
      System.out.println("basepath is + "+ apiClient.getBasePath());
      return apiClient.setHttpClient(
          apiClient.getHttpClient().newBuilder().readTimeout(Duration.ZERO).build());
    }

    // AppsV1Api로 kube api server와 통신 . v1api bean required Bean.
    @Bean
    AppsV1Api appsV1Api(ApiClient apiClient) {
        System.out.println("hello im appsV1Api method");
        apiClient.setBasePath("http://127.0.0.1:" + 8001); // kube-api server와 통신하는부분 . kubernetes service가 kube-api
                                                           // server의 service이기 때문에 , 걔를 nodeport로 열어준다.
        // apiClient.setDebugging(true); // kube api server와 통신시 debugging option 설정
        System.out.println("final basepath is + "+ apiClient.getBasePath());
        return new AppsV1Api(apiClient);
    }



    private static V1Deployment createDeployment(V1Helloworld resourceInstance) {
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
    
        
        List<V1Container> podContainers = new ArrayList<V1Container>();
        

        podContainers.add(0, createContainers(resourceInstance));

        V1ObjectMeta podMeta = new V1ObjectMeta();
        V1PodTemplateSpec podTemplateSpec = new V1PodTemplateSpec();

        podMeta.labels( // pod label 지정 ( required )
                Map.of(
                        "app", applanguageInfo,
                        "message", resourceInstance.getSpec().getMessage()
                    )
                );

        podMeta.setName("hello" + "-" + UUID.randomUUID()); // pod name 지정 ( required )

        podTemplateSpec.setMetadata(podMeta);
        podTemplateSpec.spec(new V1PodSpec()
                                    .containers(podContainers)
                            ); 
        return podTemplateSpec;
    }

    // pod container 정보 생성
    public static V1Container createContainers(V1Helloworld resourceInstance){
        V1Container container = new V1Container();
        container.setImage(resourceInstance.getSpec().getImage());
        container.setName(resourceInstance.getSpec().getAppId());

        return new V1Container();
    }

    private void createErrorCode(String name , ApiException e){
        System.err.println("Exception when calling AppsV1Api#"+name);
        System.err.println("Status code: " + e.getCode());
        System.err.println("Reason: " + e.getResponseBody()); 
        System.err.println("Response headers: " + e.getResponseHeaders());
    }
}
