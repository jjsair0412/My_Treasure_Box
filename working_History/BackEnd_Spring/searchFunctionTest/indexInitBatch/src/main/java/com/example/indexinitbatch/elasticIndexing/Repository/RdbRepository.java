package com.example.indexinitbatch.elasticIndexing.Repository;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDto;

import java.util.List;

public interface RdbRepository {
    List<InfoDto> selectInformation();
}
