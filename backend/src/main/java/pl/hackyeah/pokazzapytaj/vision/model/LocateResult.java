package pl.hackyeah.pokazzapytaj.vision.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Model answer to "where / what to do" for a single frame. The JSON schema is generated from this record. */
public record LocateResult(
        @JsonPropertyDescription("Type of device or document in the image, e.g. router, laptop screen, coffee machine, form")
        String deviceType,
        @JsonPropertyDescription("Short answer in Polish for a senior: 1-2 simple sentences on what to do now")
        String answer,
        @JsonPropertyDescription("Type of action the highlighted element is for")
        Action action,
        Target target,
        ExtractedText extractedText,
        @JsonPropertyDescription("Answer confidence from 0.0 to 1.0")
        double confidence,
        @JsonPropertyDescription("true when the image is unclear or the element is not visible and the user must show it closer / differently")
        boolean needsBetterView,
        @JsonPropertyDescription("Warning for dangerous actions (mains power, gas, water, high temperature); empty string when none")
        String safetyWarning
) {

    public enum Action { CLICK, PRESS, TURN, ROTATE, READ, TYPE, NONE }

    public record Target(
            @JsonPropertyDescription("true if the target element is visible in the image")
            boolean visible,
            @JsonPropertyDescription("Short element name in Polish, e.g. 'pole Hasło', 'przycisk WPS', 'naklejka na spodzie'")
            String label,
            @JsonPropertyDescription("Bounding box of the element on a 0-1000 scale relative to image width and height; zeros when not visible")
            BBox bbox
    ) {
    }

    public record BBox(
            @JsonPropertyDescription("Left edge, 0-1000") int x,
            @JsonPropertyDescription("Top edge, 0-1000") int y,
            @JsonPropertyDescription("Width, 0-1000") int w,
            @JsonPropertyDescription("Height, 0-1000") int h
    ) {
    }

    public record ExtractedText(
            @JsonPropertyDescription("Login page address (e.g. 192.168.0.1); empty string when none") String url,
            @JsonPropertyDescription("Wi-Fi network name (SSID); empty string when none") String ssid,
            @JsonPropertyDescription("Password (Wi-Fi or admin) read from the image; empty string when none") String password,
            @JsonPropertyDescription("Other relevant text read from the image; empty string when none") String other
    ) {
    }
}
