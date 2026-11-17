package com.cet6;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.cet6.mapper")
@EnableScheduling
public class Cet6Application {

    public static void main(String[] args) {
        SpringApplication.run(Cet6Application.class, args);
    }
}
