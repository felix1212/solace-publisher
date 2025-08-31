/*
 *  Change log:
 *  0.0.1 - Initial working one
 *  0.0.2 - Added OTel custom instrumentation
 *  0.0.2.1 - Added attributes
 */

package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PublisherApplication {

	public static void main(String[] args) {
		SpringApplication.run(PublisherApplication.class, args);
	}

}
