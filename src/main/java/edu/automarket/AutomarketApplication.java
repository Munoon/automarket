package edu.automarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = ReactiveUserDetailsServiceAutoConfiguration.class)
public class AutomarketApplication {

	static void main(String[] args) {
		SpringApplication.run(AutomarketApplication.class, args);
	}

}
