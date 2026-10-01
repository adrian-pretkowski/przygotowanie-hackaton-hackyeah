package pl.hackyeah.pokazzapytaj.vision;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.hackyeah.pokazzapytaj.vision.model.CompareResult;
import pl.hackyeah.pokazzapytaj.vision.model.LocateResult;
import pl.hackyeah.pokazzapytaj.vision.model.VisionResponse;

import java.io.IOException;

@RestController
@RequestMapping("/api/vision")
@Validated
public class VisionController {

    private final VisionService visionService;

    public VisionController(VisionService visionService) {
        this.visionService = visionService;
    }

    /** Single frame + question → what to do and where (bbox). */
    @PostMapping(path = "/locate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisionResponse<LocateResult> locate(@RequestPart("image") MultipartFile image,
                                               @RequestParam("question") @NotBlank String question) throws IOException {
        return visionService.locate(image.getBytes(), question);
    }

    /** Before / after frames + step description → whether the step was completed. */
    @PostMapping(path = "/compare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VisionResponse<CompareResult> compare(@RequestPart("before") MultipartFile before,
                                                 @RequestPart("after") MultipartFile after,
                                                 @RequestParam("expectedStep") @NotBlank String expectedStep) throws IOException {
        return visionService.compare(before.getBytes(), after.getBytes(), expectedStep);
    }
}
