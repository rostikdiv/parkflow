package com.parkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ParkflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkflowApplication.class, args);
	}

}
