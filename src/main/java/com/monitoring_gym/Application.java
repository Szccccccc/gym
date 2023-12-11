package com.monitoring_gym;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Application {
    public static ConfigurableApplicationContext ctx;

    public static void main(String[] args)
    {
        ctx = SpringApplication.run(Application.class,args);
    }
}
