package pl.hackyeah.pokazzapytaj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vision")
public record VisionProperties(
        String model,
        String effort,
        long maxTokens,
        int maxImageSide,
        float jpegQuality,
        boolean refusalFallback
) {
}
