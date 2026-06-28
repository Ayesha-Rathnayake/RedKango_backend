package com.example.backend.service.util;

import com.example.backend.dto.CommonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseHandler {

    public static <T> ResponseEntity<CommonResponse<T>> generateResponse(
            HttpStatus status,
            String message,
            String responseStatus,
            T data
    ) {
        CommonResponse<T> response = new CommonResponse<>(
                message,
                responseStatus,
                data
        );

        return new ResponseEntity<>(response, status);
    }

    public static <T> ResponseEntity<CommonResponse<T>> generateErrorResponse(
            HttpStatus status,
            String message,
            String responseStatus,
            T data
    ) {
        CommonResponse<T> response = new CommonResponse<>(
                message,
                responseStatus,
                data
        );

        return new ResponseEntity<>(response, status);
    }
}