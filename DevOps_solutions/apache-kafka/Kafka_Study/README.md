# Kafka study
Udemy 강의 [【한글자막】 Apache Kafka 시리즈 – 초보자를 위한 아파치 카프카 강의 v3](https://www.udemy.com/course/apache-kafka-korean/) 를 듣고 내용을 정리한 폴더 입니다.

## Kafka를 왜 쓸까
**데이터 통합** 문제를 해결하기 위해 사용합니다.

Source System에서 Target System으로 데이터를 이동해야 할 때 , 만약 둘다 개수가 많이 없다면 단순하지만 , 조금만 들어나면 데이터 통합 부분이 엄청 늘어날것입니다.
- Source System이 4개 , Target System이 6개라면 총 24개의 데이터 통합 부분이 필요합니다.

이때 중간에 Kafka를 넣어서 , 이런 데이터 통합 작업을 담당해서 작업합니다.

Source Sytem은 Web site나 Pricing Data 등 여러가지가 될 수 있고 , Target System은 database나 데이터 메트릭서버가 될 수 있습니다.

## kafka 기본 이론
카프카에대한 이론적인 내용들을 정리해 두었습니다.

성능개선 및 코드에서 설명할 이론들은 , 코드에 주석 및 README로 정리했습니다.
- [kafka 기본이론](./Kafka_기본이론.md)

## kafka programming
java 언어를 통해 kafka를 어떻게 핸들링할것인가에 대한 내용을 정리해 두었습니다.

- [kafka programming](./Kafka_Programming/)