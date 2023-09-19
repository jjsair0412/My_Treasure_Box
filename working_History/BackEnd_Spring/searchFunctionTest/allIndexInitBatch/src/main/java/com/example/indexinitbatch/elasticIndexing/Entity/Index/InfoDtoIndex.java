package com.example.indexinitbatch.elasticIndexing.Entity.Index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * @Document(indexName = "info_index")
 * - 이 어노테이션이 달려있으면 , 해당 클래스가 ElasticSearch의 doc을 나타내는 도메인 객체임을 선언
 * - index 이름 지정 , info_index에 색인 , info_index 인덱스가 없다면 NoSQL이기 때문에 생성됨
 *
 * @Id
 * - 해당 필드가 ElasticSearch 문서의 고유 ID를 나타낸다는것을 선언
 * - 해당 ID로 ElasticSearch doc 들을 식별하는데 사용함
 *
 * @Field
 * - 해당 필드가 ElasticSearch의 문서 내에서 어떻게 인덱싱될지를 선언함
 * - type은 해당 필드 속성 나타냄
 *  - FieldType.Text : 택스트 기반 필드
 *  - FieldType.Integer : 정수 타입 필드
 *  - FieldType.Nested : Nested 타입 필드
 *      - 카테고리는 계층 구조로 대 / 중 분류 카테고리들이 여러개 있을 수 있기 때문에 , List로 등록
 *
 */
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

    @Field(type = FieldType.Nested)
    private List<CategoryIndex> categories;

}