package com.yas.shipment;

import com.yas.shipment.config.ServiceUrlConfig;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(ServiceUrlConfig.class)
public class ShipmentApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShipmentApplication.class, args);
	}
	@Bean
	public ZipkinSpanExporter zipkinSpanExporter() {
		return ZipkinSpanExporter.builder()
				.setEndpoint("http://tempo:9411/api/v2/spans")
				.build();
	}
}
