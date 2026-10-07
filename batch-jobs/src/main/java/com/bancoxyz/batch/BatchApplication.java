package com.bancoxyz.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BatchApplication {

	public static void main(String[] args) {
		// El código de salida refleja el resultado de los jobs (0 = todos completados):
		// así Docker puede reiniciar el contenedor si un fallo crítico persiste
		System.exit(SpringApplication.exit(SpringApplication.run(BatchApplication.class, args)));
	}

}
