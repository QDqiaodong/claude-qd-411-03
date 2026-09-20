package com.orchard.grove;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.orchard.grove.mapper")
public class GroveApplication {
    public static void main(String[] args) {
        SpringApplication.run(GroveApplication.class, args);
    }
}
