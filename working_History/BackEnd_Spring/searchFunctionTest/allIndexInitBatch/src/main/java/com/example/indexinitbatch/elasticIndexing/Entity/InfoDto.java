package com.example.indexinitbatch.elasticIndexing.Entity;

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
