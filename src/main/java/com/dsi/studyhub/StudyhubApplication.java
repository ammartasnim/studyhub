package com.dsi.studyhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class StudyhubApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyhubApplication.class, args);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("admin123:   " + encoder.encode("admin123"));


    }

}
