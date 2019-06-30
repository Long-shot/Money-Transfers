import api.HttpException
import api.RestAPIServer
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import dao.InMemoryIndexedDao
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import model.Account
import model.Transaction
import transfer.InMemoryTransferEngine

fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
            setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        }
    }

    routing {
        install(StatusPages) {
            exception<HttpException> { cause ->
                call.respond(cause.code, cause.description)
            }
        }
        val accountsDao = InMemoryIndexedDao<Account>()
        val transactionsDao = InMemoryIndexedDao<Transaction>()
        val transferEngine = InMemoryTransferEngine(transactionsDao)

        RestAPIServer(accountsDao, transactionsDao, transferEngine).apply {
            registerAccountAPIs()
            registerTransactionAPIs()
        }
    }
}

fun main() {
    embeddedServer(Jetty, 8080, watchPaths = listOf("ApplicationKt"), module = Application::module).start(wait = true)
}


