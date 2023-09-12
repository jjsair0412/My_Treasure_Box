package com.example.searchapi.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class InfoEntityIndex {

        private int firstInfoId;
        private String name;
        private int age;
        private String category;
}
