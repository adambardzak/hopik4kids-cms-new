package cz.hopik4kids.cms.kernel.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Sanitizes user-provided rich-text HTML (article content from the WYSIWYG editor) before storage,
 * so the public site can render it via {@code dangerouslySetInnerHTML} without XSS risk.
 * Allows common formatting tags emitted by TipTap; strips scripts, event handlers, styles, etc.
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            // Relaxed already allows: a, b, blockquote, br, code, em, h1-h6, i, img, li, ol, p,
            // pre, strong, u, ul, tables, etc. Add a few extras and safe attributes.
            .addTags("hr", "s", "span")
            .addAttributes("a", "href", "title", "target", "rel")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addProtocols("a", "href", "http", "https", "mailto", "tel")
            .addProtocols("img", "src", "http", "https", "data");

    private HtmlSanitizer() {
    }

    /** Returns sanitized HTML, or null if input is null. Empty/whitespace-only input becomes null. */
    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        String cleaned = Jsoup.clean(html, "", SAFELIST, new Document.OutputSettings().prettyPrint(false));
        return cleaned.isBlank() ? null : cleaned;
    }
}
