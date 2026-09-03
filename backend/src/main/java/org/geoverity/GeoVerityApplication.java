package org.geoverity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GeoVerityApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeoVerityApplication.class, args);
    }
}
