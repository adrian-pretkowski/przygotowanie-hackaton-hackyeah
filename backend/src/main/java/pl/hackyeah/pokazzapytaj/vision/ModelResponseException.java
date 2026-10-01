package pl.hackyeah.pokazzapytaj.vision;

/** The model responded, but the response is unusable (refusal, truncation, no JSON). */
public class ModelResponseException extends RuntimeException {

    public ModelResponseException(String message) {
        super(message);
    }
}
