package com.sdu.dorm_system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.dorm_system.config.AppProperties;
import com.sdu.dorm_system.domain.UserAccount;
import com.sdu.dorm_system.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public JwtTokenService(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    public IssuedToken issueToken(UserAccount user) {
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = issuedAt.plusMinutes(appProperties.security().jwt().expirationMinutes());

        Map<String, Object> header = Map.of(
            "alg", "HS256",
            "typ", "JWT"
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getEmail());
        payload.put("role", user.getRole().name());
        payload.put("uid", user.getId().toString());
        payload.put("iat", issuedAt.toEpochSecond());
        payload.put("exp", expiresAt.toEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String unsignedToken = encodedHeader + "." + encodedPayload;
        String signature = sign(unsignedToken);

        return new IssuedToken(unsignedToken + "." + signature, expiresAt);
    }

    public JwtClaims parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw BusinessException.unauthorized("JWT token is missing");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw BusinessException.unauthorized("JWT token format is invalid");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            parts[2].getBytes(StandardCharsets.UTF_8)
        )) {
            throw BusinessException.unauthorized("JWT token signature is invalid");
        }

        Map<String, Object> payload = decodePayload(parts[1]);
        String subject = stringClaim(payload.get("sub"));
        long expiresAtEpoch = longClaim(payload.get("exp"));
        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(expiresAtEpoch), ZoneOffset.UTC);
        if (expiresAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw BusinessException.unauthorized("JWT token has expired");
        }

        return new JwtClaims(subject, expiresAt);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw BusinessException.conflict("JWT token could not be created");
        }
    }

    private Map<String, Object> decodePayload(String encodedPayload) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readValue(decodedBytes, MAP_TYPE);
        } catch (Exception exception) {
            throw BusinessException.unauthorized("JWT token payload is invalid");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                appProperties.security().jwt().secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            ));
            byte[] signatureBytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw BusinessException.conflict("JWT token signing failed");
        }
    }

    private String stringClaim(Object value) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw BusinessException.unauthorized("JWT token subject is invalid");
    }

    private long longClaim(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw BusinessException.unauthorized("JWT token expiration is invalid");
    }

    public record IssuedToken(
        String token,
        OffsetDateTime expiresAt
    ) {
    }

    public record JwtClaims(
        String subject,
        OffsetDateTime expiresAt
    ) {
    }
}
