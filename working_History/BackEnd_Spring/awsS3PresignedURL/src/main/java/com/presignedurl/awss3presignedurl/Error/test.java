package com.presignedurl.awss3presignedurl.Error;

import org.junit.Test;

public class test {
    @Test
    public void test(){
        if(true){
            throw new customException(errorEnum.FIRST_ERROR);
        }
    }
}
