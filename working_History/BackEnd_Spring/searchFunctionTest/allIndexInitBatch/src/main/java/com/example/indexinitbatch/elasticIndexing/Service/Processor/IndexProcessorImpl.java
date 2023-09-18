package com.example.indexinitbatch.elasticIndexing.Service.Processor;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDto;
import com.example.indexinitbatch.elasticIndexing.Entity.InfoDtoIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class IndexProcessorImpl implements IndexProcessor{

    /**
     *
     * Reader에서 반환받은 InfoDto 엔티티를 InfoDtoIndex 엔티티로 변환 합니다.
     */
    @Bean
    @Override
    public ItemProcessor<InfoDto, InfoDtoIndex> processor() {
        log.info("call processor");
        return InfoDto -> InfoDtoIndex.builder()
                .firstInfoId(InfoDto.getFirstInfoId())
                .name(InfoDto.getName())
                .age(InfoDto.getAge())
                .category(InfoDto.getCategory())
                .build();
    }
}
