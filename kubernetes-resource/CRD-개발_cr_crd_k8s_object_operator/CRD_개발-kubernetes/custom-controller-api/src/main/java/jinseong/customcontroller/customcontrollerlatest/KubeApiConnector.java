
package jinseong.customcontroller.customcontrollerlatest;

import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import jinseong.customcontroller.customcontrollerlatest.CrCrdModel.V1Helloworld;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class KubeApiConnector {

    // controller , ExcutorService , ApplicationRunner 코드 분석 필요
    // 아마 셋이서 맞물려서 kube-api-server와 통신하는듯
    @Bean
    Controller controller(SharedInformerFactory shareformerFactory,
                          Reconciler reconciler,
                          SharedIndexInformer<V1Helloworld> shareIndexInformer) {
        return ControllerBuilder
                .defaultBuilder(shareformerFactory)
                .watch(contrWatchQueue -> ControllerBuilder
                        .controllerWatchBuilder(V1Helloworld.class, contrWatchQueue)
                        .withResyncPeriod(Duration.of(1, ChronoUnit.SECONDS))
                        .build())
                // .withWorkerCount(2)
                .withReconciler(reconciler)
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
        apiClient.setBasePath("http://127.0.0.1:" + 8001); // kube-api server와 통신하는부분 .
        // kube-proxy로 kube-api 서버에 접근할 수 있게끔 오픈하거나, kube-api server 에 접근할 수 있는 권한을 생성하여 인증후 연결
        // apiClient.setDebugging(true); // kube api server와 통신시 debugging option 설정
        System.out.println("AppsV1Api's final basepath is + "+ apiClient.getBasePath());
        return new AppsV1Api(apiClient);
    }

    @Bean
    CoreV1Api coreV1Api(ApiClient apiClient){
        apiClient.setBasePath("http://127.0.0.1:" + 8001);
        System.out.println("CoreV1Api's final base path is + "+ apiClient.getBasePath());
        return new CoreV1Api(apiClient);
    }

}
