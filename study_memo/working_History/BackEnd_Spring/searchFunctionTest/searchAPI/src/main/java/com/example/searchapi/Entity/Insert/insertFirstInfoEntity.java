package com.example.searchapi.Entity.Insert;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class insertFirstInfoEntity {
    private String name;
    private int age;
    private List<String> keywords;
}
