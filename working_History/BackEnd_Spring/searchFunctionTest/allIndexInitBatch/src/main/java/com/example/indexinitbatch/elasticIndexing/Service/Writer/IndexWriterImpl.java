package com.example.indexinitbatch.elasticIndexing.Service.Writer;

import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import com.example.indexinitbatch.elasticIndexing.Entity.InfoDtoIndex;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Configuration
@Slf4j
public class IndexWriterImpl implements IndexWriter{

    /**
     * ElasticSearch와 상호 작용하기위한 ElasticsearchTemplate 객체 주입
     */
    private final ElasticsearchTemplate elasticsearchTemplate;
    @Bean
    @Override
    public ItemWriter<InfoDtoIndex> elasticSearchWriter() {
        return items -> {
            try{
                /**
                 * Processor 계층에서 변환된 items 를 받아서 , elasticsearchTemplate.bulkIndex() 메서드로
                 *
                 * elasticSearch에 색인 합니다.
                 */

                Gson gson = new Gson();
                List<IndexQuery> queries = new ArrayList<>();

                for (InfoDtoIndex item : items) {
                    IndexQuery indexQuery = new IndexQuery();
                    indexQuery.setId(String.valueOf(item.getFirstInfoId()));
                    indexQuery.setSource(gson.toJson(item));
                    queries.add(indexQuery);

                }

                if ( queries.size() > 0 ) {
                    elasticsearchTemplate.bulkIndex(queries, InfoDtoIndex.class);
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
