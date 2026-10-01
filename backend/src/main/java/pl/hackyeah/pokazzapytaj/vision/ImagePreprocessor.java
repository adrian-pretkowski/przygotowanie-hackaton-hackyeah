package pl.hackyeah.pokazzapytaj.vision;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.stereotype.Component;
import pl.hackyeah.pokazzapytaj.config.VisionProperties;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Normalizes the image before sending it to the model:
 * 1) rotation according to EXIF orientation (phone photos),
 * 2) scaling the longer side down to {@code vision.max-image-side},
 * 3) JPEG compression.
 * Coordinates returned by the model refer to the image AFTER this processing.
 */
@Component
public class ImagePreprocessor {

    private final VisionProperties props;

    public ImagePreprocessor(VisionProperties props) {
        this.props = props;
    }

    public PreparedImage prepare(byte[] input) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(input));
        if (image == null) {
            throw new IllegalArgumentException("Nieobsługiwany format obrazu");
        }
        image = applyExifOrientation(image, readExifOrientation(input));
        image = scaleDown(image, props.maxImageSide());
        return new PreparedImage(toJpeg(image, props.jpegQuality()), image.getWidth(), image.getHeight());
    }

    private static int readExifOrientation(byte[] input) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(input));
            ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception ignored) {
            // no EXIF (e.g. a canvas frame) - leave the image unchanged
        }
        return 1;
    }

    /** Handles 90/180/270 rotations (orientations 3, 6, 8); mirrored orientations are ignored. */
    private static BufferedImage applyExifOrientation(BufferedImage src, int orientation) {
        int w = src.getWidth();
        int h = src.getHeight();
        AffineTransform t = new AffineTransform();
        boolean swap;
        switch (orientation) {
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); swap = false; }
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); swap = true; }
            case 8 -> { t.translate(0, w); t.rotate(-Math.PI / 2); swap = true; }
            default -> { return src; }
        }
        BufferedImage dst = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, t, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage scaleDown(BufferedImage src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min(1.0, (double) maxSide / Math.max(w, h));
        int nw = (int) Math.round(w * scale);
        int nh = (int) Math.round(h * scale);
        // always redraw to TYPE_INT_RGB - drops the alpha channel (PNG), which JPEG does not support
        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    private static byte[] toJpeg(BufferedImage image, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
