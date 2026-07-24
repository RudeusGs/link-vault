package com.linkvault;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LinkVaultApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LinkVaultApiApplication.class, args);
	}

}