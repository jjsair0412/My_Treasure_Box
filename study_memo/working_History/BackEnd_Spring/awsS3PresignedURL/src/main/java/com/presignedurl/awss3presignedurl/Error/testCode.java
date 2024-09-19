package com.presignedurl.awss3presignedurl.Error;

import org.junit.Test;

public class testCode {
    @Test
    public void test(){
        if(true){
            throw new customException(errorEnum.FIRST_ERROR);
        }
    }
}
