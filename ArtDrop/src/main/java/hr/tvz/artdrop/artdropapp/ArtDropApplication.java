package hr.tvz.artdrop.artdropapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArtDropApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtDropApplication.class, args);
    }

}
