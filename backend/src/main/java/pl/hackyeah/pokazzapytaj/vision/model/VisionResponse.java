package pl.hackyeah.pokazzapytaj.vision.model;

/**
 * Result returned to the frontend: model response + metrics needed to evaluate the hypotheses (H4: latency).
 *
 * @param imageWidth  width of the image sent to the model (after scaling)
 * @param imageHeight height of the image sent to the model (after scaling)
 */
public record VisionResponse<T>(
        T result,
        String model,
        long modelLatencyMs,
        long totalLatencyMs,
        long inputTokens,
        long outputTokens,
        int imageWidth,
        int imageHeight
) {
}
