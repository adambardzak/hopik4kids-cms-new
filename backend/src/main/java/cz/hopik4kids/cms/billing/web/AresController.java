package cz.hopik4kids.cms.billing.web;

import cz.hopik4kids.cms.kernel.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Looks up a company in the Czech ARES registry by IČO (prd §6A.5 convenience).
 * Uses the public ARES REST API (no auth). Returns a normalized subset for the supplier form.
 */
@RestController
@RequestMapping("/admin/api/billing/ares")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT')")
public class AresController {

    private static final String ARES_URL =
            "https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/ekonomicke-subjekty/{ico}";

    private final RestClient rest = RestClient.create();

    public record AresResult(String ico, String name, String address, String dic) {
    }

    @GetMapping("/{ico}")
    public AresResult lookup(@PathVariable String ico) {
        String normalized = ico.replaceAll("[^0-9]", "");
        if (normalized.length() != 8) {
            throw ApiException.badRequest("INVALID_ICO", "IČO musí mít 8 číslic");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = rest.get()
                    .uri(ARES_URL, normalized)
                    .retrieve()
                    .body(Map.class);
            if (body == null) {
                throw ApiException.notFound("Subjekt nenalezen");
            }
            String name = str(body.get("obchodniJmeno"));
            String dic = str(body.get("dic"));
            String address = str(body.get("sidlo") instanceof Map<?, ?> s
                    ? s.get("textovaAdresa") : null);
            return new AresResult(normalized, name, address, dic);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ARES_ERROR",
                    "Nepodařilo se načíst údaje z ARESu (IČO nenalezeno nebo služba nedostupná)");
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
