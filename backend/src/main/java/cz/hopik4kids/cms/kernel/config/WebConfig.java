package cz.hopik4kids.cms.kernel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Serves uploaded media files from the local storage dir at {@code /media/**} (prd §5.6). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String storageDir;

    public WebConfig(@Value("${app.media.storage-dir}") String storageDir) {
        this.storageDir = storageDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(storageDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/media/**").addResourceLocations(location);
    }
}
