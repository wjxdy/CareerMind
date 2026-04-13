package com.careermind;

import com.careermind.config.LLMConfig;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LLMConfig.class)
public class CareerMindApplication {

    static {
        Dotenv dotenv = Dotenv.configure()
                .directory("../")
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(e -> {
            if (System.getProperty(e.getKey()) == null && System.getenv(e.getKey()) == null) {
                System.setProperty(e.getKey(), e.getValue());
            }
        });
    }

    public static void main(String[] args) {
        SpringApplication.run(CareerMindApplication.class, args);
    }
}
