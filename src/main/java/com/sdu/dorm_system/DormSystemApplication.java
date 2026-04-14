package com.sdu.dorm_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class DormSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(DormSystemApplication.class, args);
	}

}
