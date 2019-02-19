package ch.frankel.blog.springfu.migrationdemo

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@SpringBootApplication
@EnableR2dbcRepositories
class MigrationDemoApplication

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

@RestController
class PersonController(private val personRepository: PersonRepository) {

    @GetMapping("/person")
    fun readAll() = personRepository.findAll()

    @GetMapping("/person/{id}")
    fun readOne(@PathVariable id: Long) = personRepository.findById(id)
}

class Person(@Id val id: Long, val firstName: String, val lastName: String, val birthdate: LocalDate? = null)

interface PersonRepository : ReactiveCrudRepository<Person, Long>