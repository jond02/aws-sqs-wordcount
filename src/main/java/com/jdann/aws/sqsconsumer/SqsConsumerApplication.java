package com.jdann.aws.sqsconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SqsConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SqsConsumerApplication.class, args);
	}
}
