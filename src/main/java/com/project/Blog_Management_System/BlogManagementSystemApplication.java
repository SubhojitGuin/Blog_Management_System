package com.project.Blog_Management_System;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BlogManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogManagementSystemApplication.class, args);
	}

}
