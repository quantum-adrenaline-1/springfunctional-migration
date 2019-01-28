package ch.frankel.blog.springfu.migrationdemo

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.function.DatabaseClient
import org.springframework.fu.kofu.r2dbc.r2dbcH2
import org.springframework.fu.kofu.web.server
import org.springframework.fu.kofu.webApplication
import org.springframework.web.reactive.function.server.*
import java.util.*

val app = webApplication {
    beans {
        bean<DataConfiguration>()
        bean<PersonRepository>()
        bean {
            routes(ref())
        }
    }
    r2dbcH2()
    listener<ApplicationReadyEvent> {
        ref<PersonRepository>().initialize()
    }
    server {
        port = 8080
        codecs {
            jackson()
        }
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
    app.run(args)
}

class Person(@Id val id: Long, val firstName: String, val lastName: String, val birthdate: String? = null)

class PersonRepository(private val client: DatabaseClient) {

    fun initialize() {
        client.execute().sql(
            """CREATE TABLE IF NOT EXISTS PERSON (
  ID            BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  FIRST_NAME    VARCHAR(50) NOT NULL,
  LAST_NAME     VARCHAR(50) NOT NULL,
  BIRTHDATE     DATE
);""").then()
            .then(save(Person(1, "John", "Doe", "1970-01-01")))
            .then(save(Person(2, "Jane", "Doe", "1970-01-01")))
            .then(save(Person(3, "Brian", "Goetz")))
            .block()
    }

    fun save(person: Person) = client.insert()
        .into(Person::class.java)
        .using(person)
        .then()

    fun findAll() = client.select().from(Person::class.java).fetch().all()

    fun findById(id: Long) = client.execute()
        .sql("SELECT * FROM PERSON WHERE ID = $1")
        .bind(0, id)
        .`as`(Person::class.java)
        .fetch()
        .one()
}