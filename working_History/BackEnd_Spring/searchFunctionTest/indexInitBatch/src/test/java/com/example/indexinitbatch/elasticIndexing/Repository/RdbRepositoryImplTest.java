package com.example.indexinitbatch.elasticIndexing.Repository;

import com.example.indexinitbatch.elasticIndexing.Entity.InfoDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RdbRepositoryImplTest {

    @Autowired RdbRepository repository;
    @Test
    void selectInformation() {
        List<InfoDto> infoDto = repository.selectInformation();

        for(int i = 0; i<infoDto.size(); i++) {
            System.out.println("infoDto.getFirstInfoId() : "+infoDto.get(i).getFirstInfoId());
            System.out.println("infoDto.getName() : "+infoDto.get(i).getName());
            System.out.println("infoDto.getAge() : "+infoDto.get(i).getAge());
            System.out.println("infoDto.getCategory() : "+infoDto.get(i).getCategory());
        }

    }
}