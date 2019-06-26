package api

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route

/**
 * Money Transfer API
 *
 * This is an API for money transfer
 */
class RestAPIServer {
    /**
     * Register account-specific APIs into Ktor routing
     * See Swagger contracts for more detail
     */
    fun Routing.registerAccountAPIs() {
        route("/accounts"){
            get {
                call.respond(mapOf("error" to "notImplementedYet"))
                // TODO
            }
            post {
                call.respond(mapOf("error" to "notImplementedYet"))
                // TODO
            }
        }

        route("/accounts/{accountId}") {
            get {
                call.respond(mapOf("error" to "notImplementedYet"))
                // TODO
            }
            post {
                call.respond(mapOf("error" to "notImplementedYet"))
                // TODO
            }

            // technically transaction API probably should not know about account balance
            // (as in most cases it's private info). However following APIs are added for
            // demo purposes

            route("balance"){
                get {
                    call.respond(mapOf("error" to "notImplementedYet"))
                    // TODO
                }
            }

            route("deposit"){
                get {
                    call.respond(mapOf("error" to "notImplementedYet"))
                    // TODO
                }
            }

            route("withdraw"){
                get {
                    call.respond(mapOf("error" to "notImplementedYet"))
                    // TODO
                }
            }
        }
    }

    /**
     * Register transaction APIs into Ktor routing
     * See Swagger contracts for more detail
     */
    fun Routing.registerTransactionAPIs() {
        get("/transactions") {
            call.respond(mapOf("error" to "notImplementedYet"))
            // TODO
        }

        get("/transactions/{transactionId}") {
            call.respond(mapOf("error" to "notImplementedYet"))
            // TODO
        }

        post("/transfer") {
            call.respond(mapOf("error" to "notImplementedYet"))
            // TODO
        }
    }
}
