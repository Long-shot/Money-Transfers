package api

import dao.IndexedDao
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import model.Account
import java.lang.IllegalArgumentException
import java.util.*

/**
 * Money Transfer API
 *
 * This is an API for money transfer
 */
class RestAPIServer(private val accountsDao: IndexedDao<Account>) {
    /**
     * Register account-specific APIs into Ktor routing
     * See Swagger contracts for more detail
     */
    fun Routing.registerAccountAPIs() {
        route("/accounts"){
            get {
                call.respond(accountsDao.getAll())
            }
            post {
                val account = call.receive<Account>()
                accountsDao.create(account)
                call.respond(account)
            }
        }

        route("/accounts/{accountId}") {
            get {
                val accountID = call.parameters["accountID"]
                // TODO: install not found pages
                val uuid = UUID.fromString(accountID) // TODO throw bad request
                val account = accountsDao.findById(uuid) ?: throw IllegalArgumentException() // TODO throw proper exception (not found)
                call.respond(account)
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
