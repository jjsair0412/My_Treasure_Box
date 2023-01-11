package com.example.customcontrollercode;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.customcontrollercode.CrCrdModel.V1Helloworld;
import com.example.customcontrollercode.CrCrdModel.V1HelloworldList;

import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreApi;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.proto.V1.Service;
import io.kubernetes.client.proto.V1.ServiceOrBuilder;
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

    @Bean
    Resources resources(){
        return new Resources();
    }


    // AppsV1Api가 k8s api server에 request를 보내어 k8s resource 관리 수행
    @Bean
    Reconciler reconciler(SharedIndexInformer<V1Helloworld> shareIndexInformer,
            AppsV1Api appsV1Api
            ,CoreV1Api coreV1Api) {
        return request -> {
            String key = request.getNamespace() + "/" + request.getName();
            
            V1Helloworld resourceInstance = shareIndexInformer
                    .getIndexer()
                    .getByKey(key);

            
            if (resourceInstance != null) {
                V1Service v1Service = resources().creatService(resourceInstance);
                V1Deployment v1Deployment = resources().createDeployment(resourceInstance);
                System.out.println("Creating resource deployment...");
                
                try {
                    // deployment 생성
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

                try {
                    coreV1Api.createNamespacedService(
                            request.getNamespace(), 
                            v1Service, 
                            "true", 
                            null, 
                            "", 
                            "");
                } catch (ApiException service) {
                    createErrorCode("createNamespacedService",service);
                }
                return new Result(false);
            }else{
                System.out.println("delete deployment resource..."); // delete는 k8s에서 메타데이터로 리소스들을 관리하기 때문에 , crd로 생성시켜준 이름과 resource 이름이 동일하다면 만들지 않아도 k8s resource가 삭제 된다.
            }
            return new Result(false);

        };
    }

    // controller , ExcutorService , ApplicationRunner 코드 분석 필요
    // 아마 셋이서 맞물려서 kube-api-server와 통신하는듯
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
        apiClient.setBasePath("http://127.0.0.1:" + 8001); // kube-api server와 통신하는부분 . kubernetes service가 kube-api
                                                           // server의 service이기 때문에 , 걔를 nodeport로 열어준다.
        // apiClient.setDebugging(true); // kube api server와 통신시 debugging option 설정
        System.out.println("AppsV1Api's final basepath is + "+ apiClient.getBasePath());
        return new AppsV1Api(apiClient);
    }

    @Bean
    CoreV1Api coreV1Api(ApiClient apiClient){
        apiClient.setBasePath("http://127.0.0.1:" + 8001);
        System.out.println("CoreV1Api's final basepath is + "+ apiClient.getBasePath());
        return new CoreV1Api(apiClient);
    }

    
    private void createErrorCode(String name , ApiException e){
        System.err.println("Exception when calling AppsV1Api#"+name);
        System.err.println("Status code: " + e.getCode());
        System.err.println("Reason: " + e.getResponseBody()); 
        System.err.println("Response headers: " + e.getResponseHeaders());
    }
}
