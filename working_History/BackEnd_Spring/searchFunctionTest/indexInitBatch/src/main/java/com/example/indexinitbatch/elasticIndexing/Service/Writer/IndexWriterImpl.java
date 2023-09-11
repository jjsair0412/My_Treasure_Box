package com.example.indexinitbatch.elasticIndexing.Service.Writer;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDtoIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Configuration
@Slf4j
public class IndexWriterImpl implements IndexWriter{

    private final ElasticsearchTemplate elasticsearchTemplate;
    @Bean
    @Override
    public ItemWriter<InfoDtoIndex> elasticSearchWriter() {
        return items -> {
            try{
                for (InfoDtoIndex item : items) {
                    InfoDtoIndex save = elasticsearchTemplate.save(item);
                    log.info("save.getCategory() : " +save.getCategory());
                }
            }catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        };
    }
}
