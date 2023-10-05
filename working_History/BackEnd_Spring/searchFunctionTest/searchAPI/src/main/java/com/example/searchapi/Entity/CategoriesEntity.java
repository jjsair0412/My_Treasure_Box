package com.example.searchapi.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CategoriesEntity {
    private String main_category;
    private String sub_category;
}
