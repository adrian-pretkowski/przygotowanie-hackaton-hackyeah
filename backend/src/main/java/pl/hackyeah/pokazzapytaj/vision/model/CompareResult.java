package pl.hackyeah.pokazzapytaj.vision.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Model answer to "was the step completed" based on before / after frames. */
public record CompareResult(
        @JsonPropertyDescription("true if the AFTER image (labelled 'PO') shows the result of the expected step")
        boolean stepCompleted,
        @JsonPropertyDescription("Short description in Polish of what changed between the images")
        String observedChange,
        @JsonPropertyDescription("Answer confidence from 0.0 to 1.0")
        double confidence
) {
}
