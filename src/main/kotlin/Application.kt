import api.RestAPIServer
import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import com.fasterxml.jackson.databind.*
import dao.InMemoryIndexedDao
import io.ktor.jackson.jackson
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
        RestAPIServer(InMemoryIndexedDao()).apply {
            registerAccountAPIs()
            registerTransactionAPIs()
        }
    }
}

fun main() {
    embeddedServer(Jetty, 8080, watchPaths = listOf("BlogAppKt"), module = Application::module).start(wait = true)
}


