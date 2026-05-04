package com.dsi.studyhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class StudyhubApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyhubApplication.class, args);



    }

}
