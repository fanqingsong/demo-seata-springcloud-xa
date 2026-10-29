package com.sly.seata.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) throws Exception {
		SchemaInitializer.prepare();
		SpringApplication.run(Application.class, args);
	}
}
