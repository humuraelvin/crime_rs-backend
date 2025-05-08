package com.crime.reporting.crime_reporting_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

@SpringBootApplication(exclude = {
	RedisAutoConfiguration.class,
	RedisRepositoriesAutoConfiguration.class
})
public class CrimeReportingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrimeReportingBackendApplication.class, args);
	}
}
