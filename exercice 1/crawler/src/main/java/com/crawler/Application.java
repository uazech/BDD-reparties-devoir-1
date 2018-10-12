package com.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) throws Exception {
		if (args.length != 0) {
			System.setProperty("spring.batch.job.names", args[0]);

		}
		SpringApplication.run(Application.class, args);
	}
}