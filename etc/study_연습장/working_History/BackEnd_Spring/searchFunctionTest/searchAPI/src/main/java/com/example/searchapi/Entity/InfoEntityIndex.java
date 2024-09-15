package com.example.searchapi.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class InfoEntityIndex {

        private int firstInfoId;
        private String name;
        private int age;
        private String main_category;
        private String sub_category;
        private List<String> keywords;
}
