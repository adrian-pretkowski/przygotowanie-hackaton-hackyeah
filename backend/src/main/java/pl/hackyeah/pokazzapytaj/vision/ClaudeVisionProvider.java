package pl.hackyeah.pokazzapytaj.vision;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredOutputConfig;
import com.anthropic.models.messages.TextBlockParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.hackyeah.pokazzapytaj.config.VisionProperties;
import pl.hackyeah.pokazzapytaj.vision.model.CompareResult;
import pl.hackyeah.pokazzapytaj.vision.model.LocateResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** {@link VisionProvider} implementation backed by Claude (Anthropic Java SDK) with structured outputs. */
@Component
public class ClaudeVisionProvider implements VisionProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeVisionProvider.class);

    private final AnthropicClient client;
    private final VisionProperties props;
    private final PromptTemplates prompts;

    public ClaudeVisionProvider(AnthropicClient client, VisionProperties props, PromptTemplates prompts) {
        this.client = client;
        this.props = props;
        this.prompts = prompts;
    }

    @Override
    public ModelCall<LocateResult> locate(PreparedImage image, String question) {
        String text = prompts.render("locate.txt", Map.of(
                "width", image.width(),
                "height", image.height(),
                "question", question));
        return call(LocateResult.class, List.of(imageBlock(image), textBlock(text)));
    }

    @Override
    public ModelCall<CompareResult> compare(PreparedImage before, PreparedImage after, String expectedStep) {
        String text = prompts.render("compare.txt", Map.of("expectedStep", expectedStep));
        return call(CompareResult.class, List.of(
                textBlock("Obraz PRZED:"), imageBlock(before),
                textBlock("Obraz PO:"), imageBlock(after),
                textBlock(text)));
    }

    private <T> ModelCall<T> call(Class<T> type, List<ContentBlockParam> content) {
        StructuredOutputConfig.Builder<T> outputConfig = StructuredOutputConfig.<T>builder().format(type);
        if (props.effort() != null && !props.effort().isBlank()) {
            outputConfig.effort(OutputConfig.Effort.of(props.effort()));
        }

        StructuredMessageCreateParams.Builder<T> params = MessageCreateParams.builder()
                .model(props.model())
                .maxTokens(props.maxTokens())
                .system(prompts.load("system.txt"))
                .outputConfig(outputConfig.build())
                .addUserMessageOfBlockParams(content);

        if (props.refusalFallback()) {
            // Server-side fallback: on refusal (stop_reason=refusal) the API retries on a suitable model by itself.
            params.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }

        long start = System.nanoTime();
        StructuredMessage<T> response = client.messages().create(params.build());
        long latencyMs = (System.nanoTime() - start) / 1_000_000;

        StopReason stopReason = response.stopReason().orElse(null);
        long inputTokens = response.usage().inputTokens();
        long outputTokens = response.usage().outputTokens();
        log.debug("model={} type={} latency={}ms stop={} in={} out={}",
                props.model(), type.getSimpleName(), latencyMs, stopReason, inputTokens, outputTokens);

        if (StopReason.REFUSAL.equals(stopReason)) {
            throw new ModelResponseException("Model odmówił odpowiedzi (stop_reason=refusal)");
        }
        if (StopReason.MAX_TOKENS.equals(stopReason)) {
            throw new ModelResponseException("Odpowiedź ucięta (max_tokens) - zwiększ vision.max-tokens");
        }

        List<T> results = new ArrayList<>();
        response.content().forEach(block -> block.text().ifPresent(t -> results.add(t.text())));
        if (results.isEmpty()) {
            throw new ModelResponseException("Model nie zwrócił bloku z odpowiedzią JSON");
        }
        return new ModelCall<>(results.getFirst(), props.model(), latencyMs, inputTokens, outputTokens);
    }

    private static ContentBlockParam imageBlock(PreparedImage image) {
        return ContentBlockParam.ofImage(ImageBlockParam.builder()
                .source(Base64ImageSource.builder()
                        .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                        .data(image.base64())
                        .build())
                .build());
    }

    private static ContentBlockParam textBlock(String text) {
        return ContentBlockParam.ofText(TextBlockParam.builder().text(text).build());
    }
}
