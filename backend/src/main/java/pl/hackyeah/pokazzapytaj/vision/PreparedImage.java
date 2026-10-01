package pl.hackyeah.pokazzapytaj.vision;

import java.util.Base64;

/** Image after orientation correction and scaling, ready to be sent to the model (JPEG). */
public record PreparedImage(byte[] jpeg, int width, int height) {

    public String base64() {
        return Base64.getEncoder().encodeToString(jpeg);
    }
}
