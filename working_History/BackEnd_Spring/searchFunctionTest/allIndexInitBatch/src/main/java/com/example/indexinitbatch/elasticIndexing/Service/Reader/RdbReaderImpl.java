package com.example.indexinitbatch.elasticIndexing.Service.Reader;

import com.example.indexinitbatch.elasticIndexing.Entity.Index.CategoryIndex;
import com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto.InfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Repository
@Configuration
public class RdbReaderImpl implements RdbReader {

    // jdbcTemplate 사용
    private final JdbcTemplate template;

    @Bean
    @Override
    public JdbcPagingItemReader<List<InfoDto>> selectInformation() {
        try {

            JdbcPagingItemReader<List<InfoDto>> reader = new JdbcPagingItemReader<>();
            reader.setDataSource(template.getDataSource());
            reader.setPageSize(3); // 3 rows per page , 한번에 읽을 페이지 숫자를 3개로 지정

            // SQL for paging
            SqlPagingQueryProviderFactoryBean pagingQueryProvider = new SqlPagingQueryProviderFactoryBean();
            // datasource init

            /**
             * 수행 쿼리 :
             *
             * SELECT f.firstInfoId, f.name, f.age, m.main_category, s.sub_category
             * FROM firstInfo f
             *   JOIN tb_ref r ON f.firstInfoId = r.firstInfoId
             *   JOIN tb_main_category m ON r.main_category_seq = m.main_category_seq
             *   JOIN tb_sub_category s ON r.sub_category_seq = s.sub_category_seq
             *
             */
            pagingQueryProvider.setDataSource(template.getDataSource());
            pagingQueryProvider.setSelectClause("SELECT f.firstInfoId, f.name, f.age, m.main_category, s.sub_category");
            pagingQueryProvider.setFromClause("FROM firstInfo f JOIN tb_ref r ON f.firstInfoId = r.firstInfoId JOIN tb_main_category m ON r.main_category_seq = m.main_category_seq JOIN tb_sub_category s ON r.sub_category_seq = s.sub_category_seq");

            /**
             * firstInfo.firstInfoId 컬럼을 기준 으로 페이징 처리함
             */
            pagingQueryProvider.setSortKey("f.firstInfoId");

            /**
             * 생성된 페이징 쿼리를 JdbcPagingItemReader에 설정
             */
            reader.setQueryProvider(pagingQueryProvider.getObject());

            /**
             * JdbcPagingItemReader 인스턴스에 select 결과를 주입
             */

            reader.setRowMapper((rs, rowNum) -> {
                List<InfoDto> dtos = new ArrayList<>();
                List<CategoryIndex> categoryRepos = new ArrayList<>();

                categoryRepos.add(
                        CategoryIndex.builder()
                                .main_category(rs.getString("main_category"))
                                .sub_category(rs.getString("sub_category"))
                                .build()
                );

                dtos.add(
                    InfoDto.builder()
                            .firstInfoId(rs.getInt("firstInfoId"))
                            .name(rs.getString("name"))
                            .age(rs.getInt("age"))
                            .categoryRepos(categoryRepos)
                            .build()
                );
                return dtos;
            });


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
