package io.conduktor.demos.kafka.opensearch;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
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
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
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
                // 데이터가 없을 경우 해당 라인을 3000초동안 차단함
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));

                int recordCount = records.count();
                log.info("Received " + recordCount + " record(s)");

                // 레코드를 받은 직후에 , 대량 요청을 생성하기 위해서 BulkRequest 사용
                BulkRequest bulkRequest = new BulkRequest(); // 대량요청 옵션

                for (ConsumerRecord<String, String> record : records) {

                    // opensearch에 동일한 데이터가 들어가면 안되기 때문에 , 각 데이터에 ID값을 지정해서 아래 두가지 방법 중 하나를 선택해서 사용해야 함
                    // 1번 방법
                    // kafka 레코드 좌포값을 사용해서 , ID 값 정의 - 중복데이터 방지 방안
//                    String id = record.topic() + "_" + record.partition() + "_" + record.offset();


                    try{
                        // 2번 방법 ( best usecase ) - 중복데이터 방지 방안
                        // kafka에서 받아오는 데이터 값 자체에 ID값을 정의해서 , 그걸 사용하는 방법
                        // json값에서 id값 뽑아옴
                        // meta.id 에 있음
                        String id = extractId(record.value());
                        // 레코드 한개씩 뽑아서 오픈서치로 보내기
                        // Opensearch client 에게 json 데이터를 보내겠다고 알림
                        // indexRequest 에 위 unique한 id값을 추가해서 보냄
                        IndexRequest indexRequest = new IndexRequest("wikimedia")
                                .source(record.value(), XContentType.JSON)
                                .id(id); // 해당 객체는 , 데이터가 있을 때 마다 오픈서치에 삽입하게 되는데 , 이렇게되면 대량요청에대해 대응하기 힘듬,.

                        bulkRequest.add(indexRequest);

                        // Opensearch client로 오픈서치에게 json 데이터 보내겠다는 요청보냄.
                        IndexResponse response = openSearchClient.index(indexRequest, RequestOptions.DEFAULT);
                        log.info("Inserted 1 document into Opensearch");
//                        log.info(response.getId());
                    }catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                if (bulkRequest.numberOfActions() > 0 ) { // bulkRequest 개수가 0 이상일 때만 , 대량 요청 전달 (opensearch로)
                    BulkResponse bulkresponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                    log.info("inserted : "+bulkresponse.getItems().length + " record(s)");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e ) {
                        e.printStackTrace();
                    }

                    // auto.commit.offset 옵션이 false 일 경우 , 위의 for문 (batch) 이 전체사용된 이후 오프셋을 커밋하게끔 코드로 구성
                    // 여기선 그니까 , kafka topic에 쌓여있는 데이터를 모두 소비한 다음 offset을 커밋하게끔 함.
                    consumer.commitSync(); // offset commit 옵션
                    log.info("offset이 커밋되었습니다. !!!!");

                }


            }


        }catch (WakeupException e) {
            log.info("consumer가 shutdownd을 시작함");
        } catch (Exception e) {
            log.info("consumer가 예상 못한 에러발생");
            e.printStackTrace();
        } finally {
            // WakeupException에 걸리거나 Exception 에 걸리면 consumer를 종료
            // openSearchClient 또한 같이 close() 시킴
            // close하면서 offset도 commit함
            consumer.close();
            openSearchClient.close();
            log.info("consumer가 우아하게 종료됨..");
        }



        // 구성된 모든것 close
    }

    // 동일한 id를 두번 요청하면 , 덮어씌우는 기능하는 메서드
    private static String extractId(String json) {
        // gson libray 사용해서 json 파싱
        String asString = JsonParser.parseString(json)
                .getAsJsonObject()
                .get("meta")
                .getAsJsonObject()
                .get("id")
                .getAsString();
        return asString;
    }

    // opensearch와 연동하는 메서드
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

    // kafka consumer 생성 메서드
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

        // consumer offset auto.commit 옵션 enable , disable 지정
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,"false");

        // offset을 어디서부터 pull 할지 설정하는 부분
        // none/earliest/latest 세 옵션값 가능
        // none : 컨슈머 그룹이 설정되지 않으면 동작하지 않음..  application 설정 전 consumer group부터 설정해야 함
        // earliest : --from-beginning 옵션에 해당하는 옵션 . 처음부터 끝까지 다 poll.
        // latest : 가장 최신으로 cluster에 들어간 애를 poll .
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest");

        return new KafkaConsumer<>(properties);

    }
}
