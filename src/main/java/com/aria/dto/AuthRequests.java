package com.aria.dto;
import jakarta.validation.constraints.*;
public final class AuthRequests {
    private AuthRequests() {}
    public record Login(@NotBlank @Email @Size(max=255) String email, @NotBlank @Size(max=72) String password) {}
    public record Register(@NotBlank @Email @Size(max=255) String email,
                           @NotBlank @Size(min=12,max=72) String password,
                           @NotBlank @Size(max=255) String fullName,
                           @Size(max=255) String website,
                           @NotBlank @Size(max=64) String captchaId,
                           @NotBlank @Size(max=16) String captchaAnswer) {}
    public record PasswordChange(@NotBlank @Size(max=72) String currentPassword,
                                 @NotBlank @Size(min=12,max=72) String newPassword) {}
}
