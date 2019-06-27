import api.HttpException
import api.RestAPIServer
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import com.fasterxml.jackson.databind.*
import dao.InMemoryIndexedDao
import io.ktor.features.StatusPages
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*

fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        install(StatusPages) {
            exception<HttpException> { cause ->
                call.respond(cause.code, cause.description)
            }
        }
        RestAPIServer(InMemoryIndexedDao()).apply {
            registerAccountAPIs()
            registerTransactionAPIs()
        }
    }
}

fun main() {
    embeddedServer(Jetty, 8080, watchPaths = listOf("ApplicationKt"), module = Application::module).start(wait = true)
}


