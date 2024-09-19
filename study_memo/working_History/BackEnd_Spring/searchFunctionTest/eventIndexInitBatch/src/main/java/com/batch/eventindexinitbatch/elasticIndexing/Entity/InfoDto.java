package com.batch.eventindexinitbatch.elasticIndexing.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class InfoDto {
    int firstInfoId;
    String name;
    int age;
    String category;
}
