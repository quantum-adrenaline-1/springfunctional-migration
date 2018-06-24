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
    fun routes(repository: PersonRepository) = nest(
        path("/person"),
        route(
            GET("/{id}"),
            HandlerFunction { ServerResponse.ok().body(repository.findById(it.pathVariable("id").toLong())) })
            .andRoute(
                method(HttpMethod.GET),
                HandlerFunction { ServerResponse.ok().body(repository.findAll()) })
    )
}

@Configuration
class DataConfiguration : AbstractR2dbcConfiguration() {

    override fun connectionFactory() = H2ConnectionFactory(H2ConnectionConfiguration.builder()
        .inMemory("testdb")
        .username("sa")
        .build())
}

fun main(args: Array<String>) {
    runApplication<MigrationDemoApplication>(*args)
}

class Person(@Id val id: Long, val firstName: String, val lastName: String, val birthdate: LocalDate? = null)

interface PersonRepository : ReactiveCrudRepository<Person, Long>