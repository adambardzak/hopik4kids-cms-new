package cz.hopik4kids.cms.kernel.pdf;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

/**
 * Provides a Unicode-capable font (DejaVu Sans) for PDFs so Czech diacritics render correctly.
 * The default OpenPDF Helvetica uses Latin-1 and drops háčky/čárky.
 */
public final class PdfFonts {

    private static final BaseFont REGULAR = load("/fonts/DejaVuSans.ttf");
    private static final BaseFont BOLD = load("/fonts/DejaVuSans-Bold.ttf");

    private PdfFonts() {
    }

    public static Font regular(float size) {
        return new Font(REGULAR, size);
    }

    public static Font regular(float size, java.awt.Color color) {
        Font f = new Font(REGULAR, size);
        f.setColor(color);
        return f;
    }

    public static Font bold(float size) {
        return new Font(BOLD, size);
    }

    public static Font bold(float size, java.awt.Color color) {
        Font f = new Font(BOLD, size);
        f.setColor(color);
        return f;
    }

    private static BaseFont load(String path) {
        try (InputStream in = new ClassPathResource(path.substring(1)).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, bytes, null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load PDF font " + path, e);
        }
    }
}
