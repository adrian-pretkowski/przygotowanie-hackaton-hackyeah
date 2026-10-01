package pl.hackyeah.pokazzapytaj.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {

    /**
     * API key from {@code anthropic.api-key} (backend/.env or the ANTHROPIC_API_KEY variable), never in the repo.
     * When empty, the SDK looks up credentials in the environment itself.
     */
    @Bean(destroyMethod = "close")
    AnthropicClient anthropicClient(@Value("${anthropic.api-key:}") String apiKey) {
        AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder().fromEnv();
        if (!apiKey.isBlank()) {
            builder.apiKey(apiKey);
        }
        return builder.build();
    }
}
