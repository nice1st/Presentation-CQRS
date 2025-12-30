package demo.cqrs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DemoCqrsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoCqrsApplication.class, args);
    }
}
