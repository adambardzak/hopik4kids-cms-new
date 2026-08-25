package cz.hopik4kids.cms.kernel.crypto;

import jakarta.persistence.Converter;
import jakarta.persistence.AttributeConverter;

/**
 * Transparently encrypts/decrypts sensitive string columns at-rest (e.g. personalId / RČ).
 * Apply with {@code @Convert(converter = EncryptedStringConverter.class)}.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return AesGcmCipher.encrypt(attribute, EncryptionKeyHolder.key());
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return AesGcmCipher.decrypt(dbData, EncryptionKeyHolder.key());
    }
}
