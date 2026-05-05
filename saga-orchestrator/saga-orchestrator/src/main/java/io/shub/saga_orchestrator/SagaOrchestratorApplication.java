package io.shub.saga_orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SagaOrchestratorApplication {

	public static void main(String[] args) {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
		SpringApplication.run(SagaOrchestratorApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
		return new com.fasterxml.jackson.databind.ObjectMapper();
	}
}
