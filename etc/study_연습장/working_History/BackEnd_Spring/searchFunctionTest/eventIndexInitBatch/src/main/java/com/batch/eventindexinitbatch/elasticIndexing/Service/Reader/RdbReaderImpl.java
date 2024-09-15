package com.batch.eventindexinitbatch.elasticIndexing.Service.Reader;

import com.batch.eventindexinitbatch.elasticIndexing.Entity.InfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;



@Slf4j
@RequiredArgsConstructor
@Repository
@Configuration
public class RdbReaderImpl implements RdbReader {

    // jdbcTemplate 사용
    private final JdbcTemplate template;

    @Bean
    @Override
    public JdbcPagingItemReader<InfoDto> selectInformation() {
        try {

            JdbcPagingItemReader<InfoDto> reader = new JdbcPagingItemReader<>();
            reader.setDataSource(template.getDataSource());
            reader.setPageSize(3); // 3 rows per page , 한번에 읽을 페이지 숫자를 3개로 지정

            // SQL for paging
            SqlPagingQueryProviderFactoryBean pagingQueryProvider = new SqlPagingQueryProviderFactoryBean();
            // datasource init

            /**
             * 수행 쿼리 :
             * select firstInfo.firstInfoId, name, age, category from firstInfo join category_table on firstInfo.firstInfoId = category_table.firstInfoId;
             */
            pagingQueryProvider.setDataSource(template.getDataSource());
            pagingQueryProvider.setSelectClause("select firstInfo.firstInfoId, name, age, category");
            pagingQueryProvider.setFromClause("from firstInfo join category_table on firstInfo.firstInfoId = category_table.firstInfoId");

            /**
             * firstInfo.firstInfoId 컬럼을 기준 으로 페이징 처리함
             */
            pagingQueryProvider.setSortKey("firstInfo.firstInfoId");

            /**
             * 생성된 페이징 쿼리를 JdbcPagingItemReader에 설정
             */
            reader.setQueryProvider(pagingQueryProvider.getObject());

            /**
             * JdbcPagingItemReader 인스턴스에 select 결과를 주입
             */
            reader.setRowMapper((rs, rowNum) -> InfoDto.builder()
                    .firstInfoId(rs.getInt("firstInfoId"))
                    .name(rs.getString("name"))
                    .age(rs.getInt("age"))
                    .category(rs.getString("category"))
                    .build()
            );


            /**
             * JdbcPagingItemReader의 모든 속성이 올바르게 설정되었는지 확인 - 필수
             */
            reader.afterPropertiesSet();

            /**
             * 생성한 JdbcPagingItemReader() 인스턴스 반환함
             */
            return reader;
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

}
