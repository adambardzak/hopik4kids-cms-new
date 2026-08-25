package cz.hopik4kids.cms.kernel.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Holds the AES key for at-rest field encryption. Populated once from configuration.
 * JPA {@link jakarta.persistence.AttributeConverter}s are not Spring-managed, so the key
 * is exposed statically after Spring initialises this bean.
 */
@Component
public class EncryptionKeyHolder {

    private static byte[] key;

    public EncryptionKeyHolder(@Value("${app.personal-id-encryption-key}") String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        if (decoded.length != 32) {
            throw new IllegalStateException(
                    "app.personal-id-encryption-key must be a Base64-encoded 32-byte (AES-256) key");
        }
        key = decoded;
    }

    static byte[] key() {
        if (key == null) {
            throw new IllegalStateException("Encryption key not initialised");
        }
        return key;
    }
}
