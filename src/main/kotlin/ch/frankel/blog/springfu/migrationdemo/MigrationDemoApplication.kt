package ch.frankel.blog.springfu.migrationdemo

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.function.DatabaseClient
import org.springframework.web.reactive.function.server.*
import java.time.LocalDate
import java.util.*

@SpringBootApplication
class MigrationDemoApplication

fun beans() = beans {
    bean<DataConfiguration>()
    bean {
        ref<AbstractR2dbcConfiguration>().databaseClient()
    }
    bean {
        routes(ref())
    }
}

fun routes(client: DatabaseClient) = router {
    val handler = PersonHandler(PersonRepository(client))
    "/person".nest {
        GET("/{id}", handler::readOne)
        GET("/", handler::readAll)
    }
}

class DataConfiguration : AbstractR2dbcConfiguration() {

    override fun connectionFactory() = H2ConnectionFactory(H2ConnectionConfiguration.builder()
        .inMemory("testdb")
        .username("sa")
        .build())
}

fun AbstractR2dbcConfiguration.databaseClient() = databaseClient(
    reactiveDataAccessStrategy(
        r2dbcMappingContext(Optional.empty(), r2dbcCustomConversions()),
        r2dbcCustomConversions()),
    exceptionTranslator())

class PersonHandler(private val personRepository: PersonRepository) {
    fun readAll(request: ServerRequest) = ServerResponse.ok().body(personRepository.findAll())
    fun readOne(request: ServerRequest) = ServerResponse.ok().body(personRepository.findById(request.pathVariable("id").toLong()))

}

fun main(args: Array<String>) {
    runApplication<MigrationDemoApplication>(*args) {
        addInitializers(beans())
    }
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