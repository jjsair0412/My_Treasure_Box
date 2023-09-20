package com.example.indexinitbatch.elasticIndexing.Service.Batch.Processor;

import com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto.InfoDto;
import com.example.indexinitbatch.elasticIndexing.Entity.Index.InfoDtoIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class IndexProcessorImpl implements IndexProcessor {

    /**
     * Reader에서 반환받은 InfoDto 엔티티를 InfoDtoIndex 엔티티로 변환 합니다.
     */
    @Bean
    @Override
    public ItemProcessor<List<InfoDto>, InfoDtoIndex> processor() {
        log.info("call processor");
        return infoDtos -> {

            log.info("infoDto size: "+ infoDtos.size());
            InfoDto resultDto = infoDtos.get(0);

            return InfoDtoIndex.builder()
                    .firstInfoId(resultDto.getFirstInfoId())
                    .name(resultDto.getName())
                    .age(resultDto.getAge())
                    .categories(resultDto.getCategoryRepos())
                    .keyword(resultDto.getKeywords())
                    .build();
        };
    }
}
