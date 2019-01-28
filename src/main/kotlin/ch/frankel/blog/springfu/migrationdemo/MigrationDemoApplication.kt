package ch.frankel.blog.springfu.migrationdemo

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.function.DatabaseClient
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.RouterFunctions.nest
import org.springframework.web.reactive.function.server.RouterFunctions.route
import java.time.LocalDate

@SpringBootApplication
class MigrationDemoApplication {

    @Bean
    fun routes(client: DatabaseClient): RouterFunction<ServerResponse> {
        val handler = PersonHandler(PersonRepository(client))
        return nest(
            path("/person"),
            route(
                GET("/{id}"),
                HandlerFunction(handler::readOne))
                .andRoute(
                    method(HttpMethod.GET),
                    HandlerFunction(handler::readAll))
        )
    }
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

class PersonRepository(private val client: DatabaseClient) {

    fun findAll() = client.select().from(Person::class.java).fetch().all()

    fun findById(id: Long) = client.execute()
        .sql("SELECT * FROM PERSON WHERE ID = $1")
        .bind(0, id)
        .`as`(Person::class.java)
        .fetch()
        .one()
}