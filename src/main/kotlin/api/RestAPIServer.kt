package api

import dao.EntryAlreadyExistsException
import dao.IndexedDao
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.*
import model.Account
import model.Transaction
import transfer.IllegalAmountException
import transfer.InsufficientFundsException
import transfer.InvalidAccountException
import transfer.TransferEngine
import java.util.*

/**
 * Money Transfer API
 *
 * This is an API for money transfer
 */
class RestAPIServer(
    private val accountsDao: IndexedDao<Account>,
    private val transactionDao: IndexedDao<Transaction>,
    private val transferEngine: TransferEngine
) {
    /**
     * Register account-specific APIs into Ktor routing
     * See Swagger contracts for more detail
     */
    fun Routing.registerAccountAPIs() {
        route("/accounts") {
            get {
                call.respond(accountsDao.getAll())
            }
            // create account. Node, that ID passed will be ignored
            post {
                val account = call.receiveModel<Account>()

                try {
                    accountsDao.create(account)
                    transferEngine.registerAccount(account)
                } catch (e: EntryAlreadyExistsException) {
                    // this can happen only if two entries with same UUID was generated. That's almost impossible
                    throw HttpException(HttpStatusCode.InternalServerError, e.toString())
                }

                call.respond(account)
            }
        }

        route("/accounts/{accountId}") {
            get {
                val id = call.getUUIDFromPath("accountID")
                val account = accountsDao.findById(id) ?: throw HttpException(HttpStatusCode.NotFound)
                call.respond(account)
            }

            delete {
                val id = call.getUUIDFromPath("accountID")

                val account = accountsDao.deleteByID(id) ?: throw HttpException(HttpStatusCode.NotFound)
                call.respond(account)
            }

            // technically transaction API probably should not know about account balance
            // (as in most cases it's private info). However following APIs are added for
            // demo purposes

            route("balance") {
                get {
                    val id = call.getUUIDFromPath("accountID")
                    val account = accountsDao.findById(id) ?: throw HttpException(HttpStatusCode.NotFound)

                    try {
                        call.respond(BalanceResponse(transferEngine.getValue(account), account.currency))
                    } catch (e: Exception) {
                        processTransferException(e)
                    }
                }
            }

            route("deposit") {
                post {
                    val id = call.getUUIDFromPath("accountID")
                    val account = accountsDao.findById(id) ?: throw HttpException(HttpStatusCode.NotFound)

                    val request = call.receiveModel<BalanceModificationRequest>()
                    try {
                        call.respond(BalanceResponse(transferEngine.deposit(account, request.amount), account.currency))
                    } catch (e: Exception) {
                        processTransferException(e)
                    }
                }
            }

            route("withdraw") {
                post {
                    val id = call.getUUIDFromPath("accountID")
                    val account = accountsDao.findById(id) ?: throw HttpException(HttpStatusCode.NotFound)

                    val request = call.receiveModel<BalanceModificationRequest>()
                    try {
                        call.respond(BalanceResponse(transferEngine.withdraw(account, request.amount), account.currency))
                    } catch (e: Exception) {
                        processTransferException(e)
                    }
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
            val accountFrom = call.getUUIDFromQueryParameters("from")
            val accountTo = call.getUUIDFromQueryParameters("to")

            call.respond(transactionDao.filter {
                (accountFrom == null || accountFrom == it.accountFrom) && (accountTo == null || accountTo == it.accountTo)
            })
        }

        get("/transactions/{transactionId}") {
            val id = call.getUUIDFromPath("transactionId")
            val transaction = transactionDao.findById(id) ?: throw HttpException(HttpStatusCode.NotFound)
            call.respond(transaction)
        }

        post("/transfer") {
            val transferRequest = call.receiveModel<TransferRequest>()
            val accountFrom = accountsDao.findById(transferRequest.accountFrom) ?: throw HttpException(
                HttpStatusCode.BadRequest,
                "Account [${transferRequest.accountFrom} not found"
            )

            val accountTo = accountsDao.findById(transferRequest.accountTo) ?: throw HttpException(
                HttpStatusCode.BadRequest,
                "Account [${transferRequest.accountTo} not found"
            )

            try {
                call.respond(transferEngine.transfer(accountFrom, accountTo, transferRequest.amount))
            } catch (e: Exception) {
                processTransferException(e)
            }
        }
    }

    /**
     * Try accountTo parse model accountFrom application call
     */
    private suspend inline fun <reified T : Any> ApplicationCall.receiveModel(): T {
        try {
            return this.receive()
        } catch (e: Exception) {
            val name = T::class.java
            throw HttpException(HttpStatusCode.BadRequest, "Can not interpret input as [$name]")
        }
    }

    /**
     * Try accountTo parse UUID accountFrom path
     * @throws HttpException when specified path parameter isn't UUID
     */
    private fun ApplicationCall.getUUIDFromPath(varname: String): UUID {
        val id = parameters[varname]
        return try {
            UUID.fromString(id)
        } catch (e: Exception) {
            throw HttpException(HttpStatusCode.BadRequest, "[$id] is not a valid UUID")
        }
    }

    /**
     * Try accountTo parse UUID accountFrom query parameters
     * @throws HttpException when specified path parameter isn't UUID
     * @return UUID matching given parameter name if present, null otherwise
     */
    private fun ApplicationCall.getUUIDFromQueryParameters(varname: String): UUID? {
        return request.queryParameters[varname]?.let {
            try {
                UUID.fromString(it)
            } catch (e: Exception) {
                throw HttpException(HttpStatusCode.BadRequest, "[$it] is not a valid UUID")
            }
        }
    }

    private fun processTransferException(e: Exception) {
        when (e) {
            is InsufficientFundsException, is InvalidAccountException, is IllegalAmountException -> {
                throw HttpException(HttpStatusCode.BadRequest, e.toString())
            }
            else -> {
                throw HttpException(HttpStatusCode.InternalServerError, e.toString())
            }
        }
    }
}
