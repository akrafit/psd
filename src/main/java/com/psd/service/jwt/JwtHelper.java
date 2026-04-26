package com.psd.service.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;


@Component
public class JwtHelper {

    @Value("${onlyoffice.jwt.secret}")
    private String secret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String createToken(Map<String, Object> payload) {
        try {
            // Заголовок JWT
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String headerBase64 = encodeBase64Url(headerJson.getBytes(StandardCharsets.UTF_8));

            // Тело JWT
            String payloadJson = objectMapper.writeValueAsString(payload);
            String payloadBase64 = encodeBase64Url(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Подпись
            String signature = sign(headerBase64 + "." + payloadBase64);

            return headerBase64 + "." + payloadBase64 + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("JWT generation failed", e);
        }
    }

    private String sign(String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return encodeBase64Url(hash);
    }

    private String encodeBase64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}

