package com.tt.tripbook.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A signed access token and the account it belongs to")
public record AuthResponse(
        @Schema(description = "Send it as 'Authorization: Bearer <token>'") String token,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Seconds until the token expires", example = "3600") long expiresIn,
        UserResponse user) {
}
