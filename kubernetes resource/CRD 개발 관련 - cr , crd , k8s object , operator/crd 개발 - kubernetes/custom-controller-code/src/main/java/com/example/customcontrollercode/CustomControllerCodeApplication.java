package com.example.customcontrollercode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.customcontrollercode.CrCrdModel.jjsairK8sOjbectV1;
import com.example.customcontrollercode.CrCrdModel.jjsairK8sOjbectV1List;

import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.generic.GenericKubernetesApi;

@SpringBootApplication
public class CustomControllerCodeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomControllerCodeApplication.class, args);
	}

	@Bean
	SharedIndexInformer<jjsairK8sOjbectV1> sharedIndexInformer(SharedInformerFactory sharedInformerFactory, ApiClient apiClient) {
	GenericKubernetesApi<jjsairK8sOjbectV1, jjsairK8sOjbectV1List> api = new GenericKubernetesApi<>(jjsairK8sOjbectV1.class,
            V1MyCrdList.class,
            "com.amrut.prabhu",
            "v1",
            "my-crds",
            apiClient);
    return sharedInformerFactory.sharedIndexInformerFor(api, V1MyCrd.class, 0);
}

}
