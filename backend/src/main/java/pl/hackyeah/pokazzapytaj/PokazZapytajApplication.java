package pl.hackyeah.pokazzapytaj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PokazZapytajApplication {

	public static void main(String[] args) {
		SpringApplication.run(PokazZapytajApplication.class, args);
	}

}
