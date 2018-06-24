package ch.frankel.blog.springfu.migrationdemo

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.RouterFunctions.nest
import org.springframework.web.reactive.function.server.RouterFunctions.route
import java.time.LocalDate

@SpringBootApplication
@EnableR2dbcRepositories
class MigrationDemoApplication {

    @Bean
    fun routes(repository: PersonRepository): RouterFunction<ServerResponse> {
        val handler = PersonHandler(repository)
        return nest(
            path("/person"),
                route(
                        GET("/{id}"),
                        HandlerFunction { handler.readOne(it) })
                .andRoute(
                        method(HttpMethod.GET),
                        HandlerFunction { handler.readAll(it) })
    )}
}

@Configuration
class DataConfiguration : AbstractR2dbcConfiguration() {

    override fun connectionFactory() = H2ConnectionFactory(H2ConnectionConfiguration.builder()
        .inMemory("testdb")
        .username("sa")
        .build())
}

class PersonHandler(private val personRepository: PersonRepository) {
    fun readAll(request: ServerRequest) = ServerResponse.ok().body(personRepository.findAll())
    fun readOne(request: ServerRequest) = ServerResponse.ok().body(personRepository.findById(request.pathVariable("id").toLong()))

}

fun main(args: Array<String>) {
    runApplication<MigrationDemoApplication>(*args)
}

class Person(@Id val id: Long, val firstName: String, val lastName: String, val birthdate: LocalDate? = null)

interface PersonRepository : ReactiveCrudRepository<Person, Long>