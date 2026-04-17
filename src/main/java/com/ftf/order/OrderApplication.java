package com.ftf.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderApplication {

	public static void main(String[] args) {

		// Sync the Database first
		HelperFunctions helper = new HelperFunctions();
		helper.SyncDB("http://134.122.40.121:5180/api/inventory_intelligence/inventory/all_items");
		SpringApplication.run(OrderApplication.class, args);
	}

}
