package com.example.indexinitbatch.elasticIndexing.Service.Processor;

import com.example.indexinitbatch.elasticIndexing.Entity.Index.CategoryIndex;
import com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto.InfoDto;
import com.example.indexinitbatch.elasticIndexing.Entity.Index.InfoDtoIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class IndexProcessorImpl implements IndexProcessor{

    /**
     *
     * Reader에서 반환받은 InfoDto 엔티티를 InfoDtoIndex 엔티티로 변환 합니다.
     */
    @Bean
    @Override
    public ItemProcessor<List<InfoDto>, InfoDtoIndex> processor() {
        log.info("call processor");
        return infoDtos -> {
            InfoDto firstDto = infoDtos.get(0);

            List<CategoryIndex> categoryIndices = new ArrayList<>();
            for (InfoDto infoDto : infoDtos) {
                categoryIndices.addAll(infoDto.getCategoryRepos());
            }

            return InfoDtoIndex.builder()
                    .firstInfoId(firstDto.getFirstInfoId())
                    .name(firstDto.getName())
                    .age(firstDto.getAge())
                    .categories(categoryIndices)
                    .build();
        };

    }
}
