package io.conduktor.demos.kafka.opensearch.wikimedia;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 스트림이 새로운 메세지를 확인할 때 마다 해당 객체 호출함
public class WikimediaChangeHandler implements EventHandler {

    KafkaProducer<String,String> kafkaProducer;
    String topic;
    private final Logger log = LoggerFactory.getLogger(WikimediaChangeHandler.class.getSimpleName());



    // WikimediaChangeHandler 생성자에 kafkaproducer와 topic을 받도록 클래스 생성
    // 해당 헨들러객체의 onMessage 메서드에서 받아온 이벤트를 카프카 브로커로 전송해야하기 때문.
    public WikimediaChangeHandler(KafkaProducer<String, String> kafkaProducer, String topic){
        this.kafkaProducer = kafkaProducer;
        this.topic= topic;
    }

    @Override
    public void onOpen() {
        // 스트림이 열렸을때 해당메서드 호출
    }

    @Override
    public void onClosed() {
        // 스트림이 닫혔을때 해당메서드 호출
        // 따라서 카프카 프로듀서 종료코드 입력
        kafkaProducer.close();

    }

    @Override
    public void onMessage(String event, MessageEvent messageEvent) {

        log.info(messageEvent.getData());

        // http 스트림에서 온 메시지를 스트림이 수신했다는 뜻
        // MessageEvent 파라미터가 받아온 메세지인데 , 얘의 실제 내용을 받아서 ProducerRecord 으로 받아온 topic으로 보내고
        // kafkaProducer.send 실행
        kafkaProducer.send(new ProducerRecord<>(topic, messageEvent.getData()));

    }

    @Override
    public void onComment(String comment) {
        // 중요 x
    }

    @Override
    public void onError(Throwable t) {
        // 에러발생한 경우 해야할 로직

        log.error("error in stream reading", t);

    }
}
