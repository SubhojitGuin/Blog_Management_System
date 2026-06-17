package com.project.Blog_Management_System.Security;

import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JWTServiceTest {

    private static final String SECRET = "this-is-a-very-strong-test-secret-key-1234567890";

    private JWTService buildServiceWithSecret() {
        JWTService jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "jwtSecretKey", SECRET);
        return jwtService;
    }

    private SecretKey getSecretKey() {
        return hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private UserEntity buildUser() {
        UserEntity user = TestEntityFactory.testUser("jwt");
        user.setId(UUID.randomUUID());
        user.setTokenVersion(3);
        user.setRoles(Set.copyOf(user.getRoles()));
        return user;
    }

    @Nested
    @DisplayName("generateAccessToken(UserEntity)")
    class GenerateAccessToken {

        @Test
        @DisplayName("creates an access token containing user id username roles and token version")
        void createsAccessTokenWithExpectedClaims() {
            JWTService jwtService = buildServiceWithSecret();
            UserEntity user = buildUser();

            String token = jwtService.generateAccessToken(user);

            var claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertEquals(user.getId().toString(), claims.getSubject());
            assertEquals(user.getUsername(), claims.get("username", String.class));
            assertEquals(user.getRoles().toString(), claims.get("roles", String.class));
            assertEquals(user.getTokenVersion(), claims.get("tokenVersion", Integer.class));
            assertNotNull(claims.getIssuedAt());
            assertNotNull(claims.getExpiration());
            assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
        }

        @Test
        @DisplayName("throws WeakKeyException when configured secret key is too short")
        void throwsWhenSecretKeyIsTooShort() {
            JWTService jwtService = new JWTService();
            ReflectionTestUtils.setField(jwtService, "jwtSecretKey", "short-key");

            assertThrows(WeakKeyException.class, () -> jwtService.generateAccessToken(buildUser()));
        }
    }

    @Nested
    @DisplayName("generateRefreshToken(UserEntity)")
    class GenerateRefreshToken {

        @Test
        @DisplayName("creates a refresh token containing user id and token version")
        void createsRefreshTokenWithExpectedClaims() {
            JWTService jwtService = buildServiceWithSecret();
            UserEntity user = buildUser();

            String token = jwtService.generateRefreshToken(user);

            var claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertEquals(user.getId().toString(), claims.getSubject());
            assertEquals(user.getTokenVersion(), claims.get("tokenVersion", Integer.class));
            assertNotNull(claims.getIssuedAt());
            assertNotNull(claims.getExpiration());
            assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
            assertNull(claims.get("username"));
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken(String)")
    class GetUserIdFromToken {

        @Test
        @DisplayName("returns user id from a valid signed token")
        void returnsUserIdFromValidToken() {
            JWTService jwtService = buildServiceWithSecret();
            UserEntity user = buildUser();
            String token = jwtService.generateAccessToken(user);

            UUID result = jwtService.getUserIdFromToken(token);

            assertEquals(user.getId(), result);
        }

        @Test
        @DisplayName("throws exception when token is malformed")
        void throwsWhenTokenIsMalformed() {
            JWTService jwtService = buildServiceWithSecret();

            assertThrows(Exception.class, () -> jwtService.getUserIdFromToken("not-a-jwt"));
        }

        @Test
        @DisplayName("throws ExpiredJwtException when token is expired")
        void throwsWhenTokenIsExpired() {
            JWTService jwtService = buildServiceWithSecret();
            UserEntity user = buildUser();

            String expiredToken = Jwts.builder()
                    .subject(user.getId().toString())
                    .claim("tokenVersion", user.getTokenVersion())
                    .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                    .expiration(new Date(System.currentTimeMillis() - 1_000))
                    .signWith(getSecretKey())
                    .compact();

            assertThrows(ExpiredJwtException.class, () -> jwtService.getUserIdFromToken(expiredToken));
        }
    }

    @Nested
    @DisplayName("getTokenVersionFromToken(String)")
    class GetTokenVersionFromToken {

        @Test
        @DisplayName("returns token version from a valid token")
        void returnsTokenVersionFromValidToken() {
            JWTService jwtService = buildServiceWithSecret();
            UserEntity user = buildUser();
            String token = jwtService.generateRefreshToken(user);

            int result = jwtService.getTokenVersionFromToken(token);

            assertEquals(user.getTokenVersion(), result);
        }

        @Test
        @DisplayName("throws exception when token is signed with a different key")
        void throwsWhenTokenIsSignedWithDifferentKey() {
            JWTService jwtService = buildServiceWithSecret();
            UserEntity user = buildUser();

            SecretKey differentKey = hmacShaKeyFor("a-different-very-strong-secret-key-123456789".getBytes(StandardCharsets.UTF_8));
            String tokenWithDifferentSignature = Jwts.builder()
                    .subject(user.getId().toString())
                    .claim("tokenVersion", user.getTokenVersion())
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(differentKey)
                    .compact();

            assertThrows(Exception.class, () -> jwtService.getTokenVersionFromToken(tokenWithDifferentSignature));
        }
    }
}

