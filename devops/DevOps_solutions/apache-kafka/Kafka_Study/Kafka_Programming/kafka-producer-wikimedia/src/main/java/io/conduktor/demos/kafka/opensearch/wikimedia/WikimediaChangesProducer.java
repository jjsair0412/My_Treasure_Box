package io.conduktor.demos.kafka.opensearch.wikimedia;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WikimediaChangesProducer {

    public static void main(String[] args) throws InterruptedException {
        String bootstrapServers = "127.0.0.1:9092";

        // create producer properties

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers",bootstrapServers);

        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer",StringSerializer.class.getName());

        // 안전한 프로듀서 설정 설정 = 2.8 버전 사용중이라 해야됨
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG,"all"); // -1 설정과 동일
        properties.setProperty(ProducerConfig.RETRIES_CONFIG,Integer.toString(Integer.MAX_VALUE));

        // 처리량을 늘리기 위한 프로듀서 설정
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG,"20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG,Integer.toString(30*1024));
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");

        // create the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);


        String topic = "wikimedia.recentchage";


        // 스트림에서 받아온 이벤트를 처리해서 프로듀서에 전송하는 이벤트 헨들러가 필요함
        // 등록한 url에 새로운 이벤트가 발생하면 , WikimediaChangeHandler 호출
        EventHandler eventHandler = new WikimediaChangeHandler(producer, topic);
        String url = "https://stream.wikimedia.org/v2/stream/recentchange";
        // 이벤트소스 빌더 생성
        EventSource.Builder builder = new EventSource.Builder(eventHandler, URI.create(url));
        EventSource eventSource = builder.build();

        // 다른 스레드에서 프로듀서 시작
        // 독립된 스레드로 분기되어 실행되기에 메인스레드 중단방지 필요
        eventSource.start();

        // 10분동안 동작한다음 스레드 중단하는 코드 작성
        TimeUnit.MINUTES.sleep(10);

    }
}
