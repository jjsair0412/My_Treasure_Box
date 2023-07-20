package io.Conduktor.demos.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoKeys {

    private static final Logger log = LoggerFactory.getLogger(ProducerDemo.class.getSimpleName());

    public static void main(String[] args) {
        log.info("I am a Kafka Producer");

        // 1. create Producer Properties
        // 여기에 Kafka Cluster에 연결할 필요충분조건들을 Key-value로 넣어줍니다.
        // ex ) bootstrap server info , ssl info
        Properties properties = new Properties();
//        properties.setProperty("bootstrap.servers","192.168.50.10:9092"); // bootstrap 서버 정보 . 브로커와 연결

        // 해당 예제에서는 conduktor 에서 제공하는 무료 Kafka cluster와 연결할것이기 때문에 아래처럼 정보가 변경됩니다.
        properties.setProperty("bootstrap.servers","cluster.playground.cdkt.io:9092");
        properties.setProperty("security.protocol","SASL_SSL");
        properties.setProperty("sasl.jaas.config","org.apache.kafka.common.security.plain.PlainLoginModule required username=\"2Vk9J8JVRgNXIMYniYLLsX\" password=\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2F1dGguY29uZHVrdG9yLmlvIiwic291cmNlQXBwbGljYXRpb24iOiJhZG1pbiIsInVzZXJNYWlsIjpudWxsLCJwYXlsb2FkIjp7InZhbGlkRm9yVXNlcm5hbWUiOiIyVms5SjhKVlJnTlhJTVluaVlMTHNYIiwib3JnYW5pemF0aW9uSWQiOjc0OTA1LCJ1c2VySWQiOjg3MTYwLCJmb3JFeHBpcmF0aW9uQ2hlY2siOiJiODU5OTkxZS1lNjM2LTRmNzQtYjk5MC1mNmI5ODFlZWQyOTMifX0.1YNj8W65H9X35pumdidNE2AjHzPAFnK5KUtzdwm69oc\";");
        properties.setProperty("sasl.mechanism","PLAIN");

        // kafka broker로 전달되기 전 2진 직렬화시켜줄 serializer 정보 입력
        // 해당 예제에선 key, value 모두 StringSerializer로 String값을 2진수 바이트 코드로 변경하기위해
        // StringSerializer 사용
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer",StringSerializer.class.getName());


        // 2. create the Producer
        // Kafka Producer 객체 생성
        // serializer 정보와 동일하게 key-value 데이터 타입을 지정하고 , 프로퍼티로 위에 만들어준 properties 객체주입
        KafkaProducer<String,String> producer = new KafkaProducer<>(properties);

        // 배치 2번돌기
        for (int j = 0; j<2; j++ ){
            log.info(j+" 번째 batch 입니다.");
            // 한번 돌때마다 topic에 10번 보내기
            for (int i = 0; i<10; i++){

                // key , value , 대상 topic 변수로 지정
                String topic = "demo_java";
                String key = "id_" +i;
                String value = "hello world i : " +i;


                // create a Producer Record
                // Kafka로 보낼 레코드값 주입
                // 해당 예제에선 topic과 해당 topic의 넣을 value값 주입, 수많은 옵션 있음. 필요에따라 다른것 쓰기
                // key를 포함한 value가 있는 옵션으로 변경
                ProducerRecord<String, String> producerRecord =
                        new ProducerRecord<>(topic,key,value);

                // 3. send data -- 비동기식
                // 데이터 보내기
                // 이때 Callback 파라미터를 넣을 수 있습니다.
                producer.send(producerRecord, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception e) {
                        // 성공적으로 메세지가 보내지거나 exception이 발생할 때 마다 해당 익명함수 호출
                        if (e == null) {
                            // 레코드가 성공적으로 보내졌을 때
                            log.info("key : "+key+" | partition : "+metadata.partition());

                        } else {
                            log.error("Error while producing", e);
                        }
                    }
                });
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 4. flush and close producer
        // 프로듀서에게 모든 데이터 보내고 완료될때까지 기다림 -- 동기식
        producer.flush();

        // flush와 producer 종료
        producer.close();
    }
}
