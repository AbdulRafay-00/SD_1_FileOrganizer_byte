package dev.rafay.fileorganizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FileOrganizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileOrganizerApplication.class, args);
    }
}
