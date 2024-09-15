package com.example.indexinitbatch.elasticIndexing.Service.Batch.Writer;

//@Configuration
//@RequiredArgsConstructor
//@Slf4j
//public class IndexWriterImpl implements IndexWriter{
//
//    /**
//     * ElasticSearch와 상호 작용하기위한 재 정의된 ElasticsearchTemplate 객체 주입
//     */
//
//    private final Version_8_ElasticTemplateGlobalConfigImpl ElasticTemplateGlobalConfigImpl;
//
//    @Bean
//    @Override
//    public ItemWriter<InfoDtoIndex> elasticSearchWriter() {
//        return items -> {
//            try{
//
//                ElasticsearchTemplate customElasticTemplate = ElasticTemplateGlobalConfigImpl.customElasticTemplate();
//
//                /**
//                 * Processor 계층에서 변환된 items 를 받아서 , elasticsearchTemplate.bulkIndex() 메서드로
//                 *
//                 * elasticSearch에 색인 합니다.
//                 */
//
//                Gson gson = new Gson();
//                // bulk 사용하기 위해서 indexQuery 리스트 생성
//                List<IndexQuery> queries = new ArrayList<>();
//
//                for (InfoDtoIndex dtoIndex : items) {
//                    log.info("data json : "+gson.toJson(dtoIndex));
//                    IndexQuery indexQuery = new IndexQuery();
//                    indexQuery.setId(String.valueOf(dtoIndex.getFirstInfoId()));
//                    // 벌크 json에 DTO 객체를 넣기 위해서 Json으로 변경
//                    indexQuery.setSource(gson.toJson(dtoIndex));
//                    queries.add(indexQuery);
//                }
//
//                if ( queries.size() > 0 ) {
//                    // customElasticTemplate.bulkIndex()로 색인
//                    // 파라미터로 IndexQuery List, 색인될 DTO class파일 주입
////                    customElasticTemplate.bulkIndex(queries, InfoDtoIndex.class);
//                }
//
//                System.out.println("bulkIndex completed.");
//
////                ElasticsearchTemplate save 사용
////                for (InfoDtoIndex item : items) {
////                    InfoDtoIndex save = elasticsearchTemplate.save(item);
////                    log.info("save.getCategory() : " +save.getCategory());
////                }
//            }catch (Exception e) {
//                e.printStackTrace();
//                System.out.println("bulkIndex fail ! : "+e.getMessage());
//                throw new RuntimeException(e.getMessage());
//            }
//        };
//    }
//}
