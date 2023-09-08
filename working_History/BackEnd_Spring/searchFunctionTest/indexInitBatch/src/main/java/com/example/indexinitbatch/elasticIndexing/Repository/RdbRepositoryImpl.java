package com.example.indexinitbatch.elasticIndexing.Repository;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Repository
public class RdbRepositoryImpl implements RdbRepository{

    private final JdbcTemplate template;

    @Override
    public List<InfoDto> selectInformation() {
        String sql = "select firstInfo.firstInfoId, name, age, category from firstInfo join category_table on firstInfo.firstInfoId = category_table.firstInfoId;";
        List<InfoDto> result = template.query(sql, (rs, rowNum) -> {
            InfoDto resultRes = InfoDto.builder()
                    .firstInfoId(rs.getInt("firstInfoId"))
                    .name(rs.getString("name"))
                    .age(rs.getInt("age"))
                    .category(rs.getString("category"))
                    .build();
            return resultRes;
            }
        );

        return result;
    }
}
