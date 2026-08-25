package cz.hopik4kids.cms.kernel.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Generates opaque one-time tokens and hashes them for at-rest storage (invitations, resets). */
public final class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    /** URL-safe random token to send to the user. */
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Deterministic SHA-256 hash of a token, stored in the DB (never store the raw token). */
    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }
}
