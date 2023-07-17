package io.Conduktor.demos.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemo {

    private static final Logger log = LoggerFactory.getLogger(ProducerDemo.class.getSimpleName());

    public static void main(String[] args) {
        log.info("hello kafka!");

        // 1. create Producer Properties
        // 여기에 Kafka Cluster에 연결할 필요충분조건들을 Key-value로 넣어줍니다.
        // ex ) bootstrap server info , ssl info
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers","192.168.50.10:9092"); // bootstrap 서버 정보 . 브로커와 연결

        // kafka broker로 전달되기 전 2진 직렬화시켜줄 serializer 정보 입력
        // 해당 예제에선 key, value 모두 StringSerializer로 String값을 2진수 바이트 코드로 변경하기위해
        // StringSerializer 사용
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer",StringSerializer.class.getName());

        // 2. create the Producer
        // Kafka Producer 객체 생성
        // serializer 정보와 동일하게 key-value 데이터 타입을 지정하고 , 프로퍼티로 위에 만들어준 properties 객체주입
        KafkaProducer<String,String> producer = new KafkaProducer<>(properties);

        // create a Producer Record
        // Kafka로 보낼 레코드값 주입
        // 해당 예제에선 topic과 해당 topic의 넣을 value값 주입, 수많은 옵션 있음. 필요에따라 다른것 쓰기
        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>("demo_java","hello world");

        // 3. send data -- 비동기식
        // 데이터 보내기
        producer.send(producerRecord);

        // 4. flush and close producer
        // 프로듀서에게 모든 데이터 보내고 완료될때까지 기다림 -- 동기식
        producer.flush();
        
        // flush와 producer 종료
        producer.close();
    }
}
