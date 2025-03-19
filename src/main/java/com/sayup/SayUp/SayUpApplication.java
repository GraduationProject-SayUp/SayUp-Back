package com.sayup.SayUp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class SayUpApplication {

	public static void main(String[] args) {
		SpringApplication.run(SayUpApplication.class, args);
	}

}
