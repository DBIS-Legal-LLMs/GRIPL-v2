package de.mertendieckmann.griplbackend

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@OpenAPIDefinition(
	info = Info(title = "GRIPL API", description = "GRIPL Backend API"),
	servers = [Server(url = "\${app.backend.base-url:http://localhost:8000}", description = "Base URL of the API")]
)
@SpringBootApplication
@ConfigurationPropertiesScan
class GriplBackendApplication

fun main(args: Array<String>) {
	runApplication<GriplBackendApplication>(*args)
}
