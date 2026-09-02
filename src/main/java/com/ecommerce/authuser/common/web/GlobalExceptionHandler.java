package com.ecommerce.authuser.common.web;

import com.ecommerce.authuser.auth.exception.*;
import com.ecommerce.authuser.auth.exception.mfa.*;
import com.ecommerce.authuser.auth.exception.password.InvalidPasswordInputException;
import com.ecommerce.authuser.auth.exception.password.InvalidPasswordRecoveryInputException;
import com.ecommerce.authuser.auth.exception.password.InvalidPasswordResetTokenException;
import com.ecommerce.authuser.common.id.UuidV7Generator;

import com.ecommerce.authuser.user.exception.profile.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;
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
                "Vui lòng xác thực 2FA.",
                java.util.Map.of(
                        "challenge_id",
                        ex.getChallengeId(),
                        "expires_at",
                        ex.getExpiresAt(),
                        "methods",
                        ex.getMethods()
                )
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
        Object details =
                ex.getChallengeId() == null
                        ? null
                        : java.util.Map.of(
                        "challenge_id",
                        ex.getChallengeId(),
                        "expires_at",
                        ex.getExpiresAt(),
                        "methods",
                        ex.getMethods()
                );

        return buildError(
                HttpStatus.PRECONDITION_REQUIRED,
                "RBAC_MFA_REQUIRED",
                "Vui lòng xác thực lại trước khi tiếp tục.",
                details
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
                "Thông tin này đã được xác thực."
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

    @ExceptionHandler(InvalidPhoneFormatException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPhoneFormat(InvalidPhoneFormatException ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "AUTH_INVALID_INPUT",
                "Số điện thoại không đúng định dạng."
        );
    }

    @ExceptionHandler(OtpRateLimitedException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpRateLimited(OtpRateLimitedException ex) {
        return buildError(
                HttpStatus.TOO_MANY_REQUESTS,
                "AUTH_OTP_RATE_LIMITED",
                "Vui lòng thử lại sau."
        );
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOtp(InvalidOtpException ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "AUTH_VERIFICATION_INVALID",
                "Mã xác thực không hợp lệ hoặc đã hết hạn."
        );
    }

    @ExceptionHandler(OtpAttemptsExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpAttemptsExceeded(OtpAttemptsExceededException ex) {
        return buildError(
                HttpStatus.TOO_MANY_REQUESTS,
                "AUTH_OTP_ATTEMPTS_EXCEEDED",
                "Bạn đã thử quá số lần cho phép."
        );
    }

    @ExceptionHandler(InvalidPasswordRecoveryInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPasswordRecoveryInput(InvalidPasswordRecoveryInputException ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "AUTH_INVALID_INPUT",
                "Thông tin chưa đúng."
        );
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPasswordResetToken(InvalidPasswordResetTokenException ex) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                "AUTH_RESET_INVALID",
                "Liên kết đặt lại mật khẩu không hợp lệ."
        );
    }

    @ExceptionHandler(InvalidPasswordInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPasswordInput(InvalidPasswordInputException ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "AUTH_INVALID_INPUT",
                "Mật khẩu mới chưa đáp ứng yêu cầu."
        );
    }

    @ExceptionHandler(MfaAlreadyEnabledException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaAlreadyEnabled(MfaAlreadyEnabledException ex) {
        return buildError(
                HttpStatus.CONFLICT,
                "AUTH_MFA_ALREADY_ENABLED",
                "2FA đã được bật."
        );
    }

    @ExceptionHandler(MfaSetupForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaSetupForbidden(MfaSetupForbiddenException ex) {
        return buildError(
                HttpStatus.FORBIDDEN,
                "RBAC_PERMISSION_DENIED",
                "Bạn không có quyền thực hiện thao tác này."
        );
    }

    @ExceptionHandler(InvalidMfaCodeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidMfaCode(InvalidMfaCodeException ex) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "AUTH_MFA_INVALID",
                "Mã 2FA không đúng."
        );
    }

    @ExceptionHandler(MfaChallengeExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaChallengeExpired(MfaChallengeExpiredException ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "AUTH_MFA_CHALLENGE_EXPIRED",
                "Phiên xác thực 2FA đã hết hạn."
        );
    }

    @ExceptionHandler(MfaAttemptsExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaAttemptsExceeded(MfaAttemptsExceededException ex) {
        return buildError(
                HttpStatus.TOO_MANY_REQUESTS,
                "AUTH_MFA_ATTEMPTS_EXCEEDED",
                "Bạn đã thử quá số lần cho phép."
        );
    }

    @ExceptionHandler(InvalidMfaVerifyRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidMfaVerifyRequest(InvalidMfaVerifyRequestException ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "AUTH_INVALID_INPUT",
                "Dữ liệu đầu vào không hợp lệ."
        );
    }

    @ExceptionHandler(MfaAuthenticationRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaAuthenticationRequired(MfaAuthenticationRequiredException ex) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "AUTH_TOKEN_INVALID",
                "Phiên đăng nhập không hợp lệ."
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "AUTH_INVALID_INPUT",
                "Dữ liệu đầu vào không hợp lệ."
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "AUTH_USER_NOT_FOUND",
                "Không tìm thấy tài khoản."
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
