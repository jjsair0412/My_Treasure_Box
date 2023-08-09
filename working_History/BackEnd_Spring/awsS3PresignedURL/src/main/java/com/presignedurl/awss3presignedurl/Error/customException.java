package com.presignedurl.awss3presignedurl.Error;
import com.presignedurl.awss3presignedurl.Error.errorEnum;


public class customException extends RuntimeException {
    public errorEnum errorEnum;

    public customException(errorEnum e) {
        super(e.getErrorMessage());
        this.errorEnum = e;
    }
}
