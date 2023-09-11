package com.example.indexinitbatch.elasticIndexing.Service.Reader;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDto;
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

    private final JdbcTemplate template;

    @Bean
    @Override
    public JdbcPagingItemReader<InfoDto> selectInformation() {
        try {

            log.info("call selectInformation");
            JdbcPagingItemReader<InfoDto> reader = new JdbcPagingItemReader<>();
            reader.setDataSource(template.getDataSource());
            reader.setPageSize(3); // 3 rows per page

            // SQL for paging
            SqlPagingQueryProviderFactoryBean pagingQueryProvider = new SqlPagingQueryProviderFactoryBean();
            pagingQueryProvider.setDataSource(template.getDataSource());
            pagingQueryProvider.setSelectClause("select firstInfo.firstInfoId, name, age, category");
            pagingQueryProvider.setFromClause("from firstInfo join category_table on firstInfo.firstInfoId = category_table.firstInfoId");
            pagingQueryProvider.setSortKey("firstInfo.firstInfoId"); // 페이징 기준 컬럼

            reader.setQueryProvider(pagingQueryProvider.getObject());

            reader.setRowMapper((rs, rowNum) -> InfoDto.builder()
                    .firstInfoId(rs.getInt("firstInfoId"))
                    .name(rs.getString("name"))
                    .age(rs.getInt("age"))
                    .category(rs.getString("category"))
                    .build()
            );

            reader.afterPropertiesSet();

            return reader;
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

}
