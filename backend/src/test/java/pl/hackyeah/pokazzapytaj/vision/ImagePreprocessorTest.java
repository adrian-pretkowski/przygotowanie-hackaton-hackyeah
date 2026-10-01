package pl.hackyeah.pokazzapytaj.vision;

import org.junit.jupiter.api.Test;
import pl.hackyeah.pokazzapytaj.config.VisionProperties;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImagePreprocessorTest {

    private final ImagePreprocessor preprocessor =
            new ImagePreprocessor(new VisionProperties("test", "", 1000, 1000, 0.8f, false));

    @Test
    void scalesLongerSideDownKeepingAspectRatio() throws Exception {
        PreparedImage result = preprocessor.prepare(png(4000, 3000));

        assertEquals(1000, result.width());
        assertEquals(750, result.height());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.jpeg()));
        assertNotNull(decoded, "result should be a valid JPEG");
        assertEquals(1000, decoded.getWidth());
    }

    @Test
    void doesNotUpscaleSmallImages() throws Exception {
        PreparedImage result = preprocessor.prepare(png(640, 480));

        assertEquals(640, result.width());
        assertEquals(480, result.height());
    }

    @Test
    void rejectsNonImageInput() {
        assertThrows(IllegalArgumentException.class, () -> preprocessor.prepare("to nie obraz".getBytes()));
    }

    private static byte[] png(int w, int h) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }
}
