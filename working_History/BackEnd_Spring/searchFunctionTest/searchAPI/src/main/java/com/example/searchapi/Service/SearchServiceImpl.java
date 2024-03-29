package com.example.searchapi.Service;

import com.example.searchapi.Entity.CategoriesEntity;
import com.example.searchapi.Entity.InfoEntityIndex;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    @Override
    public List<InfoEntityIndex> matchAll(String indexName) {

        // SearchRequest 객체 생성, 메게변수 없이 생성할 경우 모든 인덱스를 대상으로 문서를 검색함
        SearchRequest searchRequest = new SearchRequest(indexName);

        //
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        // firstInfoId 컬럼 기준으로 정렬
        return getInfoEntityIndices(searchRequest, searchSourceBuilder);
    }

    @Override
    public List<InfoEntityIndex> matchKeyword(String indexName, String keyword) {
        SearchRequest searchRequest = new SearchRequest(indexName);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        MatchQueryBuilder categoryQuery = QueryBuilders.matchQuery("keyword", keyword)
                .fuzziness(Fuzziness.AUTO)
                .prefixLength(3)
                .maxExpansions(10);

        searchSourceBuilder.query(categoryQuery);
        // firstInfoId 컬럼 기준으로 정렬
        return getInfoEntityIndices(searchRequest, searchSourceBuilder);
    }



    private List<InfoEntityIndex> getInfoEntityIndices(SearchRequest searchRequest, SearchSourceBuilder searchSourceBuilder) {

        searchRequest.source(searchSourceBuilder);

        List<InfoEntityIndex> resultMap = new ArrayList<>();

        Gson gson = new Gson();
        try (RestHighLevelClient client = createConnection()) {

            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            for (SearchHit hit : response.getHits()) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();

                System.out.println("sourceAsMap.get(\"categories\").toString() : " + sourceAsMap.get("categories").toString());
                System.out.println("count : " + sourceAsMap.get("firstInfoId"));
                resultMap.add(
                        InfoEntityIndex.builder()
                                .firstInfoId((int) sourceAsMap.get("firstInfoId"))
                                .name((String) sourceAsMap.get("name"))
                                .age((int) sourceAsMap.get("age"))
                                .keywords((List<String>) sourceAsMap.get("keyword"))
                                .build()
                );
            }


            return resultMap;

        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }


    private RestHighLevelClient createConnection() {
        // elasticSearch 로그인정보 기입
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("test", "test1234"));

        // elasticSearch Host 주소와 위에 생성한 로그인정보 파라미터로 넣어서 RestHighLevelClient 객체 새성
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("127.0.0.1", 9200, "http")
                ).setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))

        );
    }
}
