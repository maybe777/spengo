package com.voskhod.spnego;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.nio.file.Paths;

@SpringBootApplication
@EnableWebSecurity
@ComponentScan("com.voskhod")
public class SpnegoApplication {

	static {
		System.setProperty("java.security.krb5.conf",
				Paths.get("/etc/krb5.conf")
						.normalize().toAbsolutePath().toString());
		System.setProperty("sun.security.krb5.debug", "true");
	}

	public static void main(String[] args) {
		SpringApplication.run(SpnegoApplication.class, args);
	}

}
