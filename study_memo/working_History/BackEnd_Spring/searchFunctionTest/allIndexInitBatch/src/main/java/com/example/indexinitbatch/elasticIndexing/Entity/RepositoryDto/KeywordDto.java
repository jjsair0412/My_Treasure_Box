package com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class KeywordDto {
    private int firstInfoId;
    private String keyword;
}
