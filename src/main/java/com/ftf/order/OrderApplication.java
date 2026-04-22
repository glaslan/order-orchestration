package com.ftf.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderApplication {

	public static final boolean DEBUG_MODE = true;

	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}

}
