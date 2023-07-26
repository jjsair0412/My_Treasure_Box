package io.conduktor.demos.kafka.opensearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.OpenSearchClient;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OpenSearchConsumer {

    public static void main(String[] args) throws IOException {
        Logger log = LoggerFactory.getLogger(OpenSearchConsumer.class.getSimpleName());

        // Opensearch client 구성
        RestHighLevelClient openSearchClient = createOpenSearchClient();


        // Kafka client 구성
        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        // 이렇게 구성하면 , try 조건구문안에 있는 객체가 성공하던 실패하던 close() 메서드 호출함.
        // 따라서 오픈서치 클라이언트나 컨슈머나 둘중에 하나가 성공하던 실패하던 무조건 close 호출
        try (openSearchClient; consumer){

            boolean indexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT);
            log.info("test:",indexExists);
            if (!indexExists) {
                // 오픈서치 인덱스가 존재하지 않는다면 , 인덱스를 생성해야 함, CreateIndexRequest 로 생성. 파라미터로 인덱스이름 받음
                CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
                // opensearchClient로 실제 해당 인덱스생성 쿼리를 날려야함.
                openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                log.info("wikimedia index has been created");
            } else {
                log.info("wikimedia index already exits");
            }

            // 컨슈머가 파티션을 구독해야함
            consumer.subscribe(Collections.singleton("wikimedia.recentchage"));

            // 메인 코드 로직 수행
            // 데이터 소비 코드
            while(true) {
                // 데이터가 없을 경우 해당 줄을 3000초동안 차단함
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));

                int recordCount = records.count();
                log.info("Received " + recordCount + " record(s)");

                for (ConsumerRecord<String, String> record : records) {

                    try{
                        // 레코드 한개씩 뽑아서 오픈서치로 보내기
                        // Opensearch client 에게 json 데이터를 보내겠다고 알림
                        IndexRequest indexRequest = new IndexRequest("wikimedia")
                                .source(record.value(), XContentType.JSON);

                        // Opensearch client로 오픈서치에게 json 데이터 보내겠다는 요청보냄.
                        IndexResponse response = openSearchClient.index(indexRequest, RequestOptions.DEFAULT);
                        log.info("Inserted 1 document into Opensearch");
                        log.info(response.getId());
                    }catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            }


        }



        // 구성된 모든것 close
    }


    public static RestHighLevelClient createOpenSearchClient() {
        String connString = "http://localhost:9200";

        // we build a URI from the connection string
        RestHighLevelClient restHighLevelClient;
        URI connUri = URI.create(connString);
        // extract login information if it exists
        String userInfo = connUri.getUserInfo();

        if (userInfo == null) {
            // REST client without security
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

        } else {
            // REST client with security
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                            .setHttpClientConfigCallback(
                                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));


        }

        return restHighLevelClient;
    }

    private static KafkaConsumer<String, String> createKafkaConsumer(){

        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "consumer-opensearch-demo";

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers); // bootstrap 서버 정보 . 브로커와 연결

        // consumer 설정 코드 생성
        // properties에 등록
        // Kafka cluster에서 pull한 데이터가 String이기 때문에 , 2진 바이트코드로 바뀐 String 데이터를 다시 StringDeserializer 로 String 화 시킵니다.
        // 당연한 말이지만 , 데이터가 어떤게 들어오느냐에 따라서 해당 값이 달라져야만 합니다.
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());

        // consumer group id 값을 지정해야합니다.
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,groupId);

        // offset을 어디서부터 pull 할지 설정하는 부분
        // none/earliest/latest 세 옵션값 가능
        // none : 컨슈머 그룹이 설정되지 않으면 동작하지 않음..  application 설정 전 consumer group부터 설정해야 함
        // earliest : --from-beginning 옵션에 해당하는 옵션 . 처음부터 끝까지 다 poll.
        // latest : 가장 최신으로 cluster에 들어간 애를 poll .
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest");

        return new KafkaConsumer<>(properties);

    }
}
