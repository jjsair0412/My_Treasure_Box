package com.example.indexinitbatch.elasticIndexing.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "info_index")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class InfoDtoIndex {

    @Id
    private int firstInfoId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Integer)
    private int age;

    @Field(type = FieldType.Text)
    private String category;
}