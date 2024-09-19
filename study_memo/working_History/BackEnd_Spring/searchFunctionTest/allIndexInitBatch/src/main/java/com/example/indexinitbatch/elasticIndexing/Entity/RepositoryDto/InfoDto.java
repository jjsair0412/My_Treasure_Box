package com.example.indexinitbatch.elasticIndexing.Entity.RepositoryDto;

import com.example.indexinitbatch.elasticIndexing.Entity.Index.CategoryIndex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class InfoDto {
    int firstInfoId;
    String name;
    int age;
    List<CategoryIndex> categoryRepos;
    List<String> keywords;
}
