package com.ecommerce.authuser.common.web;

import com.ecommerce.authuser.auth.exception.EmailAlreadyExistsException;
import com.ecommerce.authuser.auth.exception.PhoneAlreadyExistsException;
import com.ecommerce.authuser.common.id.UuidV7Generator;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
        return buildError(
                HttpStatus.CONFLICT,
                "AUTH_EMAIL_EXISTS",
                "Email đã được sử dụng."
        );
    }

    @ExceptionHandler(PhoneAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handlePhoneExists(PhoneAlreadyExistsException ex) {

        return buildError(
                HttpStatus.CONFLICT,
                "AUTH_PHONE_EXISTS",
                "Số điện thoại đã được sử dụng."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Object details =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                new ValidationDetail(
                                        error.getField(),
                                        error.getDefaultMessage()
                                )
                        )
                        .toList();

        return buildError(
                HttpStatus.BAD_REQUEST,
                "AUTH_INVALID_INPUT",
                "Dữ liệu đầu vào không hợp lệ.",
                details
        );
    }

    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status,
            String code,
            String message
    ) {
        return buildError(
                status,
                code,
                message,
                null
        );
    }

    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status,
            String code,
            String message,
            Object details
    ) {
        String traceId = UuidV7Generator.generate().toString();

        ApiErrorResponse body =
                new ApiErrorResponse(
                        new ApiErrorResponse.ErrorData(
                                code,
                                message,
                                details,
                                traceId
                        )
                );

        return ResponseEntity
                .status(status)
                .body(body);
    }

    private record ValidationDetail(
            String field,
            String message
    ) {
    }
}
