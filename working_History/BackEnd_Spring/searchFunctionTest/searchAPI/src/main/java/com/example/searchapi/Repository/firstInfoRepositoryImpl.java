package com.example.searchapi.Repository;

import com.example.searchapi.Entity.Insert.insertFirstInfoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class firstInfoRepositoryImpl implements firstInfoRepository{

    @Override
    public int insertFirstInfo(insertFirstInfoEntity entity) {
        return 0;
    }
}
