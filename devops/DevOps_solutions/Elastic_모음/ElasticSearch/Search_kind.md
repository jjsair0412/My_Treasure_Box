# Search Kinds in ElasticSearch
## 위치기반 검색
ElasticSearch에 데이터를 색인할 때, Geo Type인 값을 기반으로 위치기반검색하는 방안입니다.

geo_point 필드 타입은 위도와 경도를 쌍으로 가집니다.

위치 기반 쿼리를 이용해 반경 내 쿼리, 위치 기반 집계, 위치별 정렬 등을 사용할 수 있습니다.

아래 쿼리를 수행하면, ***location에 등록된 위도 경도를 기반으로 직선거리를 계산하여 가까운 순(asc)으로 정렬됩니다.***
- 또한 검색결과는 10개(size: 10) 로 제한됩니다.

```bash
# 왕십리역 기준으로 가까운순 정렬
GET /my-index/_search
{
    "query": {
        "bool": {
            "must": {
                "match_all": {}
            }
        }
    },
    "from": 0,
    "size": 10,
    "sort": [
        {
            "_geo_distance": {
                "location": {
                    "lat": 37.561185980712,
                    "lon": 127.03648929262
                },
                "order": "asc",
                "unit": "m",
                "mode": "min"
            }
        }
    ],
    "aggs": {}
}

```