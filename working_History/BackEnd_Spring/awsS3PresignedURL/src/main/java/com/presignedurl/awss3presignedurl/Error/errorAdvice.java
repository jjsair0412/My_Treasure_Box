package com.presignedurl.awss3presignedurl.Error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class errorAdvice {

    @ExceptionHandler({customException.class}) // customException 발생 시 동작
    public ResponseEntity<?> bindException(HttpServletRequest request, final customException e) {
        return makeErrorResponse(e.errorEnum.getStatus(), request.getRequestURI(),e.getMessage());
    }

    private ResponseEntity<?> makeErrorResponse(HttpStatus status , String requestPath, String message) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("path", requestPath);
        errorDetails.put("code", status.value());
        errorDetails.put("message", message);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("error", errorDetails);

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }
}
