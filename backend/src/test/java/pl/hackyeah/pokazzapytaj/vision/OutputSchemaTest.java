package pl.hackyeah.pokazzapytaj.vision;

import com.anthropic.models.messages.StructuredOutputConfig;
import org.junit.jupiter.api.Test;
import pl.hackyeah.pokazzapytaj.vision.model.CompareResult;
import pl.hackyeah.pokazzapytaj.vision.model.LocateResult;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** The SDK validates the JSON schema generated from records locally - catches unsupported types without an API call. */
class OutputSchemaTest {

    @Test
    void locateResultSchemaIsSupported() {
        assertDoesNotThrow(() -> StructuredOutputConfig.<LocateResult>builder().format(LocateResult.class).build());
    }

    @Test
    void compareResultSchemaIsSupported() {
        assertDoesNotThrow(() -> StructuredOutputConfig.<CompareResult>builder().format(CompareResult.class).build());
    }
}
