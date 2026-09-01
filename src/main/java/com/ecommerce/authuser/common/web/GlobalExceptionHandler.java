package com.ecommerce.authuser.common.web;

import com.ecommerce.authuser.auth.exception.*;
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

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID_CREDENTIALS",
                "Email/số điện thoại hoặc mật khẩu không đúng."
        );
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountSuspended(AccountSuspendedException ex) {
        return buildError(
                HttpStatus.FORBIDDEN,
                "AUTH_ACCOUNT_SUSPENDED",
                "Tài khoản hiện không thể sử dụng."
        );
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountLocked(AccountLockedException ex) {
        return buildError(
                HttpStatus.LOCKED,
                "AUTH_ACCOUNT_LOCKED",
                "Tài khoản đang tạm khóa. Vui lòng thử lại sau."
        );
    }

    @ExceptionHandler(AdminMfaRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleAdminMfaRequired(AdminMfaRequiredException ex) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "AUTH_MFA_REQUIRED",
                "Vui lòng xác thực 2FA."
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "AUTH_TOKEN_INVALID",
                "Refresh token không hợp lệ."
        );
    }

    @ExceptionHandler(ExpiredRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleExpiredRefreshToken(ExpiredRefreshTokenException ex) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "AUTH_TOKEN_EXPIRED",
                "Refresh token đã hết hạn."
        );
    }

    @ExceptionHandler(ReusedRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleReusedRefreshToken(ReusedRefreshTokenException ex) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "AUTH_REFRESH_REUSED",
                "Refresh token đã được sử dụng trước đó."
        );
    }

    @ExceptionHandler(MfaStepUpRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaStepUpRequired(MfaStepUpRequiredException ex) {
        return buildError(
                HttpStatus.PRECONDITION_REQUIRED,
                "RBAC_MFA_REQUIRED",
                "Yêu cầu xác thực lại trước khi đăng xuất tất cả phiên."
        );
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidVerificationToken(InvalidVerificationTokenException ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "AUTH_VERIFICATION_INVALID",
                "Token xác thực không hợp lệ hoặc đã hết hạn."
        );
    }

    @ExceptionHandler(VerificationAlreadyCompleteException.class)
    public ResponseEntity<ApiErrorResponse> handleVerificationAlreadyComplete(VerificationAlreadyCompleteException ex) {
        return buildError(
                HttpStatus.CONFLICT,
                "AUTH_VERIFICATION_ALREADY_COMPLETE",
                "Email đã được xác thực."
        );
    }

    @ExceptionHandler(ResendLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleResendLimitExceeded(ResendLimitExceededException ex) {
        return buildError(
                HttpStatus.TOO_MANY_REQUESTS,
                "AUTH_RESEND_LIMIT_EXCEEDED",
                "Đã vượt quá số lần gửi lại email xác thực cho phép."
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
