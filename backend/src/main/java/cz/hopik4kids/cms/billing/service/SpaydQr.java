package cz.hopik4kids.cms.billing.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Czech QR payment (SPAYD / "Short Payment Descriptor") generator (prd §6A.5).
 * Produces the SPD string and renders it as a QR PNG.
 */
public final class SpaydQr {

    private SpaydQr() {
    }

    /** Build the SPD 1.0 payment string. Amount in CZK, IBAN without spaces. */
    public static String spayd(String iban, int amountCzk, String variableSymbol, String message) {
        StringBuilder sb = new StringBuilder("SPD*1.0");
        sb.append("*ACC:").append(iban.replace(" ", ""));
        sb.append("*AM:").append(amountCzk).append(".00");
        sb.append("*CC:CZK");
        if (variableSymbol != null && !variableSymbol.isBlank()) {
            sb.append("*X-VS:").append(variableSymbol.replaceAll("[^0-9]", ""));
        }
        if (message != null && !message.isBlank()) {
            // SPAYD message: uppercase ASCII, limited length.
            String msg = message.replaceAll("[*]", " ");
            if (msg.length() > 60) msg = msg.substring(0, 60);
            sb.append("*MSG:").append(msg);
        }
        return sb.toString();
    }

    /** Render a payload as a QR PNG (square, given pixel size). */
    public static byte[] qrPng(String payload, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(
                    new String(payload.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                    BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("QR generation failed", e);
        }
    }
}
