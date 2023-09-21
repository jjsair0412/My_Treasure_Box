package com.example.indexinitbatch.elasticIndexing.Service.Batch.Writer;

import com.example.indexinitbatch.elasticIndexing.Entity.Index.InfoDtoIndex;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;

public interface IndexWriter {
    ItemWriter<InfoDtoIndex> elasticSearchWriter();
}
