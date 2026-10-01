package pl.hackyeah.pokazzapytaj.vision;

import org.springframework.stereotype.Service;
import pl.hackyeah.pokazzapytaj.vision.VisionProvider.ModelCall;
import pl.hackyeah.pokazzapytaj.vision.model.CompareResult;
import pl.hackyeah.pokazzapytaj.vision.model.LocateResult;
import pl.hackyeah.pokazzapytaj.vision.model.VisionResponse;

import java.io.IOException;

/** Combines image preprocessing with the model call and measures timings. Will also be used by EvalRunner (stage 4). */
@Service
public class VisionService {

    private final ImagePreprocessor preprocessor;
    private final VisionProvider provider;

    public VisionService(ImagePreprocessor preprocessor, VisionProvider provider) {
        this.preprocessor = preprocessor;
        this.provider = provider;
    }

    public VisionResponse<LocateResult> locate(byte[] image, String question) throws IOException {
        long start = System.nanoTime();
        PreparedImage prepared = preprocessor.prepare(image);
        ModelCall<LocateResult> call = provider.locate(prepared, question);
        return toResponse(call, start, prepared);
    }

    public VisionResponse<CompareResult> compare(byte[] before, byte[] after, String expectedStep) throws IOException {
        long start = System.nanoTime();
        PreparedImage preparedBefore = preprocessor.prepare(before);
        PreparedImage preparedAfter = preprocessor.prepare(after);
        ModelCall<CompareResult> call = provider.compare(preparedBefore, preparedAfter, expectedStep);
        return toResponse(call, start, preparedAfter);
    }

    private static <T> VisionResponse<T> toResponse(ModelCall<T> call, long startNanos, PreparedImage image) {
        long totalMs = (System.nanoTime() - startNanos) / 1_000_000;
        return new VisionResponse<>(call.result(), call.model(), call.latencyMs(), totalMs,
                call.inputTokens(), call.outputTokens(), image.width(), image.height());
    }
}
