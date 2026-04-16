package com.sample.app.ai;

import org.springframework.boot.SpringApplication;

public class TestSampleAppSpringAiApplication {

	public static void main(String[] args) {
		SpringApplication.from(SampleAppSpringAiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
