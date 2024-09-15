package com.batch.eventindexinitbatch.elasticIndexing.Service.Writer;

import com.batch.eventindexinitbatch.elasticIndexing.Config.ElasticTemplateGlobalConfig;
import com.batch.eventindexinitbatch.elasticIndexing.Entity.InfoDtoIndex;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class IndexWriterImpl implements IndexWriter{

    /**
     * ElasticSearch와 상호 작용하기위한 재 정의된 ElasticsearchTemplate 객체 주입
     */

    private final ElasticTemplateGlobalConfig elasticTemplateGlobalConfig;

    @Bean
    @Override
    public ItemWriter<InfoDtoIndex> elasticSearchWriter() {
        return items -> {
            try{

                ElasticsearchTemplate customElasticTemplate = elasticTemplateGlobalConfig.customElasticTemplate();

                /**
                 * Processor 계층에서 변환된 items 를 받아서 , elasticsearchTemplate.bulkIndex() 메서드로
                 *
                 * elasticSearch에 색인 합니다.
                 */

                Gson gson = new Gson();
                // bulk 사용하기 위해서 indexQuery 리스트 생성
                List<IndexQuery> queries = new ArrayList<>();


                for (InfoDtoIndex item : items) {
                    IndexQuery indexQuery = new IndexQuery();
                    indexQuery.setId(String.valueOf(item.getFirstInfoId()));
                    // 벌크 json에 DTO 객체를 넣기 위해서 Json으로 변경
                    indexQuery.setSource(gson.toJson(item));
                    queries.add(indexQuery);
                }

                if ( queries.size() > 0 ) {
                    // customElasticTemplate.bulkIndex()로 색인
                    // 파라미터로 IndexQuery List, 색인될 DTO class파일 주입
                    customElasticTemplate.bulkIndex(queries, InfoDtoIndex.class);
                }

                System.out.println("bulkIndex completed.");

//                ElasticsearchTemplate save 사용
//                for (InfoDtoIndex item : items) {
//                    InfoDtoIndex save = elasticsearchTemplate.save(item);
//                    log.info("save.getCategory() : " +save.getCategory());
//                }
            }catch (Exception e) {
                e.printStackTrace();
                System.out.println("bulkIndex fail ! : "+e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
        };
    }
}
