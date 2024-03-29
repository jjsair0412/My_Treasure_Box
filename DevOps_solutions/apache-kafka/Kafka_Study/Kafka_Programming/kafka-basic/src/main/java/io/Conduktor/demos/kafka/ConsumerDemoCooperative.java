package io.Conduktor.demos.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoCooperative {

    private static final Logger log = LoggerFactory.getLogger(ConsumerDemoCooperative.class.getSimpleName());

    public static void main(String[] args) {
        log.info("I am Kafka Consumer");

        String groupId = "my-java-appilcation";
        String topic = "demo_java";

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

        // consumer 설정 코드 생성
        // properties에 등록
        // Kafka cluster에서 pull한 데이터가 String이기 때문에 , 2진 바이트코드로 바뀐 String 데이터를 다시 StringDeserializer 로 String 화 시킵니다.
        // 당연한 말이지만 , 데이터가 어떤게 들어오느냐에 따라서 해당 값이 달라져야만 합니다.
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        // consumer group id 값을 지정해야합니다.
        properties.setProperty("group.id",groupId);

        // offset을 어디서부터 pull 할지 설정하는 부분
        // none/earliest/latest 세 옵션값 가능
        // none : 컨슈머 그룹이 설정되지 않으면 동작하지 않음..  application 설정 전 consumer group부터 설정해야 함
        // earliest : --from-beginning 옵션에 해당하는 옵션 . 처음부터 끝까지 다 poll.
        // latest : 가장 최신으로 cluster에 들어간 애를 poll .
        properties.setProperty("auto.offset.reset","earliest");

        // consumer 생성
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // 안전종료 옵션 추가
        // 1. 메인 thread의 reference를 가져옵니다.
        final Thread mainThread = Thread.currentThread();

        // 2. shotdown hook을 추가합니다
        Runtime.getRuntime().addShutdownHook(new Thread(){
             public void run() {
                 // consumer.wakeup() 메서드를 사용하는데, 얘는 consuer.poll() 메서드가 수행된 이후 알아서 wakeup() 예외를 던집니다.
                log.info("종료를 감지함. consumer.wakeup() 메서드를 호출하고 나갈 예정...");
                consumer.wakeup();

                // 메인프로그램이 끝날때 까지 해당 hook이 살아있어야함.
                 // 메인 스레드에 합류해서 메인 스레드의 코드 실행을 허용해야 합니다.
                 // 하단의 try catch로 묶인 poll 부분
                 try {
                     mainThread.join();
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                 }
             }
        });


        try{

            // topic 구독
            // topic들을 배열로 받아와서 여러 토픽을 구독할 수 있습니다.
            consumer.subscribe(Arrays.asList(topic));

            // data poll해오기
            while (true) {

                // Kafka의 부하를 막기 위해서 해당 옵션 필요.
                // Kafka에 데이터가 있으면 바로 받아오는데 , 없으면 1000ms  , 1초동안 기다림.

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                // records 컬렉션에 있는 모든값을 하나씩 가져옴
                for (ConsumerRecord<String, String> record : records) {
                    log.info("KEY : "+record.key() + " Value : " + record.value());
                    log.info("Partition : "+record.partition() + " Offset : " + record.offset());
                }
            }

        } catch (WakeupException e) {
            log.info("consumer가 shutdownd을 시작함");
        } catch (Exception e) {
            log.info("consumer가 예상 못한 에러발생");
            e.printStackTrace();
        } finally {
            // WakeupException에 걸리거나 Exception 에 걸리면 consumer를 종료
            // close하면서 offset도 commit함
            consumer.close();

            log.info("consumer가 우아하게 종료됨..");
        }
    }
}
