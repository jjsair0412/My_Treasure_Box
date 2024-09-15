package com.example.indexinitbatch.elasticIndexing.Entity.Index;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryIndex {
    @Field(type = FieldType.Text)
    private String main_category;

    @Field(type = FieldType.Text)
    private String sub_category;
}
