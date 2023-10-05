package com.example.searchapi.Controller;

import com.example.searchapi.Entity.CommonResponseEntity;
import com.example.searchapi.Service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class SearchController {

    private final SearchService searchService;
    @GetMapping("/search/matchAll")
    public ResponseEntity<CommonResponseEntity> matchAll(@RequestParam("indexName") String indexName ) {
        return new ResponseEntity<>(
                CommonResponseEntity.builder()
                        .returnEntity(searchService.matchAll(indexName))
                        .message("testOK")
                        .build(), HttpStatus.OK
        );
    }

    @GetMapping("/search/matchCategory")
    public ResponseEntity<CommonResponseEntity> categorySearch(
            @RequestParam("indexName") String indexName,
            @RequestParam("keyword") String keyword
    ) {
        return new ResponseEntity<>(
                CommonResponseEntity.builder()
                        .returnEntity(searchService.matchKeyword(indexName,keyword))
                        .message("testOK")
                        .build(), HttpStatus.OK
        );
    }
}
