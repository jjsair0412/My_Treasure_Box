package com.example.indexinitbatch.elasticIndexing.Service.Batch.Reader;

import com.example.indexinitbatch.elasticIndexing.Entity.Index.CategoryIndex;
import com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto.KeywordDto;
import com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto.InfoDto;
import com.example.indexinitbatch.elasticIndexing.Service.Maker.removeDuplicateValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;


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
            reader.setPageSize(3); // 3 rows per page

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
            pagingQueryProvider.setSelectClause(
                    "select firstInfo.firstInfoId,\n" +
                            "       name,\n" +
                            "       age,\n" +
                            "       main.main_category,\n" +
                            "       sub.sub_category,\n" +
                            "       word.keyword"
            );
            pagingQueryProvider.setFromClause("" +
                    "from\n" +
                    "    firstInfo\n" +
                    "        JOIN\n" +
                    "    tb_ref ON firstInfo.firstInfoId = tb_ref.firstInfoId\n" +
                    "        JOIN\n" +
                    "    tb_main_category main ON tb_ref.main_category_seq = main.main_category_seq\n" +
                    "        JOIN\n" +
                    "    tb_sub_category sub on tb_ref.sub_category_seq = sub.sub_category_seq\n" +
                    "        JOIN\n" +
                    "    tb_keyword word on tb_ref.keyword_seq = word.keyword_seq"
            );

            /**
             * firstInfo.firstInfoId 컬럼을 기준 으로 정렬
             */
            pagingQueryProvider.setSortKey("firstInfo.firstInfoId");

            /**
             * 생성된 페이징 쿼리를 JdbcPagingItemReader에 설정
             */
            reader.setQueryProvider(pagingQueryProvider.getObject());

            /**
             * JdbcPagingItemReader 인스턴스에 select 결과를 주입
             */

            List<KeywordDto> keywordDtoList = collectFirstInfoId(template);

            reader.setRowMapper((rs, rowNum) -> {

                List<InfoDto> resultList = new ArrayList<>();
                List<CategoryIndex> categoryRepos = new ArrayList<>();
                List<String> keywordList = new ArrayList<>();


                for (KeywordDto keywordDto : keywordDtoList) {

                    // firstInfoId값과 현재 rowMapper가 조회한 firstInfoId값이 같다면
                    if (keywordDto.getFirstInfoId() == rs.getInt("firstInfoId")) {
                        // keywordList에 현재 keyword 추가
                        keywordList.add(keywordDto.getKeyword());
                    }
                }
                categoryRepos.add(
                        CategoryIndex.builder()
                                .main_category(rs.getString("main_category"))
                                .sub_category(rs.getString("sub_category"))
                                .build()
                );

                resultList.add(
                        InfoDto.builder()
                                .firstInfoId(rs.getInt("firstInfoId"))
                                .name(rs.getString("name"))
                                .age(rs.getInt("age"))
                                .categoryRepos(categoryRepos)
                                .keywords(keywordList)
                                .build()
                );

                return resultList;
            });


            /**
             * JdbcPagingItemReader의 모든 속성이 올바르게 설정되었는지 확인 - 필수
             */
            reader.afterPropertiesSet();

            /**
             * 생성한 JdbcPagingItemReader() 인스턴스 반환함
             */

            return reader;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    // firstInfoId 수집
    private List<KeywordDto> collectFirstInfoId(
            JdbcTemplate template
    ) {
        String sql = "" +
                "select\n" +
                "    firstInfo.firstInfoId,\n" +
                "    word.keyword\n" +
                "from\n" +
                "    firstInfo\n" +
                "        JOIN\n" +
                "    tb_ref ON firstInfo.firstInfoId = tb_ref.firstInfoId\n" +
                "        JOIN\n" +
                "    tb_keyword word on tb_ref.keyword_seq = word.keyword_seq;\n";


        List<KeywordDto> resultList = template.query(sql, (rs, rowNum) -> KeywordDto.builder()
                .firstInfoId(rs.getInt("firstInfoId"))
                .keyword(rs.getString("keyword"))
                .build());

        return resultList;
    }

}
