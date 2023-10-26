package jinseong.customcontroller.customcontrollerlatest;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import jinseong.customcontroller.customcontrollerlatest.CrCrdModel.V1Helloworld;
import jinseong.customcontroller.customcontrollerlatest.CrCrdModel.V1HelloworldList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectKubernetesResources {

    private final BeanFactory beanFactory;


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


    // AppsV1Api가 Kube-Api-Server에 request 를 보내어 k8s resource 관리 수행
    @Bean
    Reconciler reconciler(SharedIndexInformer<V1Helloworld> shareIndexInformer,
                          AppsV1Api appsV1Api,
                          CoreV1Api coreV1Api) {
        return request -> {
            String key = request.getNamespace() + "/" + request.getName();

            V1Helloworld resourceInstance = shareIndexInformer
                    .getIndexer()
                    .getByKey(key);

            if (resourceInstance != null) {
                V1Service v1Service = beanFactory.resources().creatService(resourceInstance);
                V1Deployment v1Deployment = beanFactory.resources().createDeployment(resourceInstance);
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
                    createErrorCode("createNamespacedDeployment", e);
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
                            createErrorCode("replaceNamespacedDeployment", ex);
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
                    createErrorCode("createNamespacedService", service);
                }
                return new Result(false);
            } else {
                System.out.println("delete deployment resource...");
                try {
                    // deployment 제거
                    appsV1Api.deleteNamespacedDeployment(
                            request.getName(),
                            request.getNamespace(),
                            null,
                            null,
                            0,
                            null,
                            null,
                            null
                    );
                } catch (ApiException e) {
                    createErrorCode("deleteNamespacedDeployment", e);
                }
                try {
                    // service 제거
                    coreV1Api.deleteNamespacedService(
                            request.getName(),
                            request.getNamespace(),
                            null, // delete options
                            null, // grace period seconds
                            0, // orphan dependents
                            null, // propagation policy
                            null, // preconditions
                            null  // dry run
                    );
                } catch (ApiException e) {
                    createErrorCode("deleteNamespacedService", e);
                }
            }
            return new Result(false);

        };
    }

    private void createErrorCode(String name, ApiException e) {
        System.err.println("Exception when calling AppsV1Api#" + name);
        System.err.println("Status code: " + e.getCode());
        System.err.println("Reason: " + e.getResponseBody());
        System.err.println("Response headers: " + e.getResponseHeaders());
    }
}
