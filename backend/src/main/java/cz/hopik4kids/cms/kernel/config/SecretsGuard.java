package cz.hopik4kids.cms.kernel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Defence-in-depth: refuses to run with known insecure dev defaults when the {@code prod}
 * profile is active. The prod profile has no fallbacks (so missing env already fails fast);
 * this additionally catches the case where a dev value is explicitly passed in production.
 */
@Component
public class SecretsGuard implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SecretsGuard.class);

    private static final Set<String> INSECURE_VALUES = Set.of(
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",       // dev personal-id key
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWZnaGlq",   // dev jwt secret
            "changeme123"                                          // dev owner password
    );

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        boolean prod = Set.of(env.getActiveProfiles()).contains("prod");
        if (!prod) {
            return;
        }

        checkSecret(env, "app.personal-id-encryption-key");
        checkSecret(env, "app.jwt.secret");
        checkSecret(env, "app.bootstrap.owner-password");
    }

    private void checkSecret(Environment env, String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank() || INSECURE_VALUES.contains(value)) {
            log.error("Refusing to start: '{}' is missing or uses a known insecure dev default.", key);
            throw new IllegalStateException(
                    "Insecure or missing secret in prod profile: " + key
                            + ". Provide a strong value via environment variable.");
        }
    }
}
