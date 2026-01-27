package com.school.ppmg.student_clubs_system_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.school.ppmg.student_clubs_system_client.client")
public class StudentClubsSystemClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudentClubsSystemClientApplication.class, args);
	}

}
