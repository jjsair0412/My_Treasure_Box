package com.example.searchapi.Service;

import com.example.searchapi.Entity.InfoEntityIndex;

import java.util.List;

public interface SearchService {
    // index 전문검색
    List<InfoEntityIndex> matchAll(String indexName);

    // 카테고리별 검색
    List<InfoEntityIndex> matchCategory(String indexName, String categoryName);
}
