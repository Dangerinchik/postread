package com.postread;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class PostreadApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostreadApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Устанавливаем системный часовой пояс для всего приложения
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        System.out.println("Spring boot application running in Europe/Moscow timezone : " + TimeZone.getDefault().getID());
    }

}
