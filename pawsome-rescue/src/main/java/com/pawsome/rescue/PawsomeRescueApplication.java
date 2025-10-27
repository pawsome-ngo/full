package com.pawsome.rescue;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PawsomeRescueApplication {

	public static void main(String[] args) {
		SpringApplication.run(PawsomeRescueApplication.class, args);
	}

	// Add this bean to enable multipart file uploads
	@Bean
	public MultipartConfigElement multipartConfigElement() {
		return new MultipartConfigElement("");
	}

}
