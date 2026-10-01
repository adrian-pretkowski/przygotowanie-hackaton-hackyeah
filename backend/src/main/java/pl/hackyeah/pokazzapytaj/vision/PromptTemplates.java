package pl.hackyeah.pokazzapytaj.vision;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Prompt templates from {@code resources/prompts}. Placeholders use the {@code {{name}}} format.
 * Kept in files so the wording can be iterated on without code changes.
 */
@Component
public class PromptTemplates {

    public String render(String name, Map<String, Object> values) {
        String text = load(name);
        for (var e : values.entrySet()) {
            text = text.replace("{{" + e.getKey() + "}}", String.valueOf(e.getValue()));
        }
        return text;
    }

    public String load(String name) {
        try {
            return new ClassPathResource("prompts/" + name).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Brak szablonu promptu: " + name, e);
        }
    }
}
