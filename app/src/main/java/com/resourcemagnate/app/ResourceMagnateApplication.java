package com.resourcemagnate.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResourceMagnateApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceMagnateApplication.class, args);
	}

}
