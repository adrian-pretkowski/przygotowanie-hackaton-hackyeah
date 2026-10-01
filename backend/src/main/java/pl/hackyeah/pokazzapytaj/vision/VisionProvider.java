package pl.hackyeah.pokazzapytaj.vision;

import pl.hackyeah.pokazzapytaj.vision.model.CompareResult;
import pl.hackyeah.pokazzapytaj.vision.model.LocateResult;

/**
 * Abstraction over a multimodal model. Allows comparing different models / providers
 * on the same test set (stage 4 of the plan).
 */
public interface VisionProvider {

    ModelCall<LocateResult> locate(PreparedImage image, String question);

    ModelCall<CompareResult> compare(PreparedImage before, PreparedImage after, String expectedStep);

    /** Result of a single model call together with metrics. */
    record ModelCall<T>(T result, String model, long latencyMs, long inputTokens, long outputTokens) {
    }
}
