package com.imct.alphaclass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class AlphaclassApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(AlphaclassApplication.class, args);
	}

	
	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AlphaclassApplication.class);
    }
}
