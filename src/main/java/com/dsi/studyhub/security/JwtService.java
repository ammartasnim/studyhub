package com.dsi.studyhub.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Scanner;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expiration}")
    private long expirationMs;

    @Value("${supabase.url:https://rczlzgbgbqmmcejggyqa.supabase.co}")
    private String supabaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SecretKey getSigningKey() {
        byte[] keyBytes = HexFormat.of().parseHex(secretString);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        // Tries local JWT validation first, then falls back to Supabase JWKS.
        // Local JWT parsing
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
        }

        // Supabase JWT parsing
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 3) {
                throw new RuntimeException("Invalid JWT format");
            }

            String headerJson = new String(
                    Base64.getUrlDecoder().decode(parts[0]),
                    StandardCharsets.UTF_8
            );

            JsonNode headerNode = objectMapper.readTree(headerJson);
            String kid = headerNode.has("kid") ? headerNode.get("kid").asText() : null;

            if (kid == null) {
                throw new RuntimeException("No kid found in token header");
            }

            PublicKey publicKey = getSupabasePublicKey(kid);

            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Supabase JWT token: " + e.getMessage(), e);
        }
    }

    private PublicKey getSupabasePublicKey(String kid) {
        // Resolves an ES256 public key from Supabase JWKS by key id.
        try {
            String jwksJson;
            try (InputStream is = new URL(supabaseUrl + "/auth/v1/.well-known/jwks.json").openStream();
                 Scanner s = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                jwksJson = s.hasNext() ? s.next() : "";
            }
            JsonNode jwks = objectMapper.readTree(jwksJson);
            JsonNode keys = jwks.get("keys");
            for (JsonNode key : keys) {
                if (kid.equals(key.get("kid").asText())) {
                    String x = key.get("x").asText();
                    String y = key.get("y").asText();
                    byte[] xBytes = Base64.getUrlDecoder().decode(x);
                    byte[] yBytes = Base64.getUrlDecoder().decode(y);
                    BigInteger xBI = new BigInteger(1, xBytes);
                    BigInteger yBI = new BigInteger(1, yBytes);
                    ECPoint ecPoint = new ECPoint(xBI, yBI);
                    AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
                    parameters.init(new ECGenParameterSpec("secp256r1"));
                    ECParameterSpec ecParams = parameters.getParameterSpec(ECParameterSpec.class);

                    ECPublicKeySpec spec = new ECPublicKeySpec(ecPoint, ecParams);
                    KeyFactory kf = KeyFactory.getInstance("EC");
                    return kf.generatePublic(spec);
                }
            }
            throw new RuntimeException("No matching key found in JWKS for kid: " + kid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Supabase public key", e);
        }
    }

    public boolean isSupabaseToken(Claims claims) {
        String iss = claims.getIssuer();
        return iss != null && iss.contains("supabase");
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}
