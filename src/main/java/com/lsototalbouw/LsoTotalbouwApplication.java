package com.lsototalbouw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class LsoTotalbouwApplication {

    public static void main(String[] args) {
        SpringApplication.run(LsoTotalbouwApplication.class, args);
    }
}
