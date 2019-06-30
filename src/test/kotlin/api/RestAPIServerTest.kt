package api

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import model.Account
import model.Currency
import model.Transaction
import model.TransactionStatus
import module
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals


class RestAPIServerTest {
    private val mapper = jacksonObjectMapper()

    init {
        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    }

    @Nested
    inner class Accounts {
        @Nested
        inner class Create {
            @Test
            fun `valid account creation succeeds`() {
                val account = createAccount()

                withTestEngine {
                    handleRequest(HttpMethod.Post, "/accounts") {
                        setJsonBody(account)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val respAccount = response.readJsonModel<Account>()
                        assertNotEquals(account.id, respAccount.id)
                        assertEqualsIgnoringID(account, respAccount)
                    }
                }
            }

            @Test
            fun `invalid account creation fails`() {
                val account = createAccount(currency = Currency.USD)
                var json = mapper.writeValueAsString(account)
                // Set currency accountTo unsupported value
                json = json.replace("USD", "XXX")

                withTestEngine {
                    handleRequest(HttpMethod.Post, "/accounts") {
                        setBody(json)
                    }.apply {
                        assertEquals(HttpStatusCode.BadRequest, response.status())
                    }
                }
            }
        }

        @Nested
        inner class Read {
            @Test
            fun `get all existing accounts succeeds`() {
                withTestEngine {
                    for (i in 1..5) {
                        val account = createAccount()
                        createRemoteAccount(account)

                        handleRequest(HttpMethod.Get, "/accounts").apply {
                            assertEquals(HttpStatusCode.OK, response.status())
                            val responseAccounts = response.readJsonList<Account>()
                            assertEquals(i, responseAccounts.size)
                        }
                    }
                }
            }

            @Test
            fun `fetching single existing account succeeds`() {
                val account = createAccount()
                withTestEngine {
                    val accountID = createRemoteAccount(account)

                    handleRequest(HttpMethod.Get, "/accounts/$accountID").apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val respAccount = response.readJsonModel<Account>()
                        assertEqualsIgnoringID(account, respAccount)
                    }
                }
            }

            @Test
            fun `fetching non-existing account fails`() {
                val account = createAccount()
                withTestEngine {
                    handleRequest(HttpMethod.Get, "/accounts/${account.id}").apply {
                        assertEquals(HttpStatusCode.NotFound, response.status())
                    }
                }
            }
        }

        @Nested
        inner class Delete {
            private val account = createAccount()

            @Test
            fun `test delete account success`() {
                withTestEngine {
                    val accountID = createRemoteAccount(account)

                    handleRequest(HttpMethod.Delete, "/accounts/$accountID").apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val respAccount = response.readJsonModel<Account>()
                        assertEqualsIgnoringID(account, respAccount)
                    }
                }
            }

            @Test
            fun `test fail on delete non-existing account`() {
                withTestEngine {
                    handleRequest(HttpMethod.Delete, "/accounts/${account.id}").apply {
                        assertEquals(HttpStatusCode.NotFound, response.status())
                    }
                }
            }
        }

        @Nested
        inner class Balance {
            private val account = createAccount()

            @Test
            fun `test initial balance is zero`() {
                withTestEngine {
                    val accountID = createRemoteAccount(account)

                    handleRequest(HttpMethod.Get, "/accounts/$accountID/balance").apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val response = response.readJsonModel<BalanceResponse>()
                        assertEquals(BalanceResponse(BigDecimal.ZERO.setScale(4), account.currency), response)
                    }
                }
            }

            @Test
            fun `test error on balance of non-existing account`() {
                withTestEngine {
                    handleRequest(HttpMethod.Get, "/accounts/${account.id}/balance").apply {
                        assertEquals(HttpStatusCode.NotFound, response.status())
                    }
                }
            }
        }

        @Nested
        inner class Deposit {
            private val account = createAccount()

            @Test
            fun `test successful deposit to account`() {
                withTestEngine {
                    val accountID = createRemoteAccount(account)
                    val depositRequest = BalanceModificationRequest(BigDecimal.TEN.setScale(4))
                    handleRequest(HttpMethod.Post, "/accounts/$accountID/deposit") {
                        setJsonBody(depositRequest)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val response = response.readJsonModel<BalanceResponse>()
                        assertEquals(BalanceResponse(depositRequest.amount, account.currency), response)
                    }
                }
            }

            @Test
            fun `test error on deposit to non-existent account`() {
                withTestEngine {
                    val depositRequest = BalanceModificationRequest(BigDecimal.TEN.setScale(4))
                    handleRequest(HttpMethod.Post, "/accounts/${account.id}/deposit") {
                        setJsonBody(depositRequest)
                    }.apply {
                        assertEquals(HttpStatusCode.NotFound, response.status())
                    }
                }
            }
        }

        @Nested
        inner class Withdraw {
            private val account = createAccount()

            @Test
            fun `test successful withdrawal from account`() {
                withTestEngine {
                    val accountID = createRemoteAccount(account)
                    val depositRequest = BalanceModificationRequest(BigDecimal.TEN.setScale(4))
                    handleRequest(HttpMethod.Post, "/accounts/$accountID/deposit") {
                        setJsonBody(depositRequest)
                    }
                    handleRequest(HttpMethod.Post, "/accounts/$accountID/withdraw") {
                        setJsonBody(depositRequest)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val response = response.readJsonModel<BalanceResponse>()
                        assertEquals(BalanceResponse(BigDecimal.ZERO.setScale(4), account.currency), response)
                    }
                }
            }

            @Test
            fun `test fail on withdrawal from non-existing account`() {
                withTestEngine {
                    val depositRequest = BalanceModificationRequest(BigDecimal.TEN.setScale(4))
                    handleRequest(HttpMethod.Post, "/accounts/${account.id}/withdraw") {
                        setJsonBody(depositRequest)
                    }.apply {
                        assertEquals(HttpStatusCode.NotFound, response.status())
                    }
                }
            }

            @Test
            fun `test fail on insufficient funds`() {
                withTestEngine {
                    val accountID = createRemoteAccount(account)
                    val withdrawRequest = BalanceModificationRequest(BigDecimal.TEN.setScale(4))
                    handleRequest(HttpMethod.Post, "/accounts/$accountID/withdraw") {
                        setJsonBody(withdrawRequest)
                    }.apply {
                        assertEquals(HttpStatusCode.BadRequest, response.status())
                    }
                }
            }
        }
    }

    @Nested
    inner class Transactions {
        private val accountFrom = createAccount()
        private val accountTo = createAccount()

        @Test
        fun `fetching all transactions succeeds`() {
            withTestEngine {
                val accountID1 = createRemoteAccount(accountFrom)
                val accountID2 = createRemoteAccount(accountTo)
                val depositRequest = BalanceModificationRequest(BigDecimal.TEN.setScale(4))
                handleRequest(HttpMethod.Post, "/accounts/$accountID1/deposit") {
                    setJsonBody(depositRequest)
                }

                for (i in 1..10) {
                    handleRequest(HttpMethod.Post, "/transfer") {
                        setJsonBody(TransferRequest(accountID1, accountID2, BigDecimal.ONE))
                    }
                    handleRequest(HttpMethod.Get, "/transactions").apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val responseAccounts = response.readJsonList<Transaction>()
                        assertEquals(i, responseAccounts.size)
                    }
                }
            }
        }

        @Test
        fun `fetching single transaction succeeds`() {
            withTestEngine {
                val accountID1 = createRemoteAccount(accountFrom)
                val accountID2 = createRemoteAccount(accountTo)
                val depositRequest = BalanceModificationRequest(BigDecimal.TEN.setScale(4))
                handleRequest(HttpMethod.Post, "/accounts/$accountID1/deposit") {
                    setJsonBody(depositRequest)
                }


                handleRequest(HttpMethod.Post, "/transfer") {
                    setJsonBody(TransferRequest(accountID1, accountID2, BigDecimal.ONE))
                }.apply {
                    val transactionID = getIDFromJson(response.content!!)
                    handleRequest(HttpMethod.Get, "/transactions/$transactionID").apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val transaction = response.readJsonModel<Transaction>()
                        assertEquals(transaction.amount, BigDecimal.ONE)
                    }
                }
            }
        }

        @Test
        fun `fetching non-existent transaction fails`() {
            withTestEngine {
                val transactionID = UUID.randomUUID()!!
                handleRequest(HttpMethod.Get, "/transactions/$transactionID").apply {
                    assertEquals(HttpStatusCode.NotFound, response.status())
                }
            }
        }

        @Test
        fun `filtering on transaction issuer works`() {
            withTestEngine {
                val accountOther = createAccount()

                val accountID1 = createRemoteAccount(accountFrom)
                val accountID2 = createRemoteAccount(accountTo)
                val accountID3 = createRemoteAccount(accountOther)

                val depositRequest = BalanceModificationRequest(BigDecimal.TEN.setScale(4))
                // init balance of sender accounts
                handleRequest(HttpMethod.Post, "/accounts/$accountID1/deposit") {
                    setJsonBody(depositRequest)
                }
                handleRequest(HttpMethod.Post, "/accounts/$accountID3/deposit") {
                    setJsonBody(depositRequest)
                }

                // make transfers account1 -> account2 and account3 -> account2
                handleRequest(HttpMethod.Post, "/transfer") {
                    setJsonBody(TransferRequest(accountID1, accountID2, BigDecimal.ONE))
                }
                handleRequest(HttpMethod.Post, "/transfer") {
                    setJsonBody(TransferRequest(accountID3, accountID2, BigDecimal.ONE))
                }

                handleRequest(HttpMethod.Get, "/transactions?from=$accountID1").apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val responseAccounts = response.readJsonList<Transaction>()
                    assertEquals(1, responseAccounts.size)
                    assertEquals(accountID1, responseAccounts[0].accountFrom)
                }
            }
        }

        @Test
        fun `filtering on transaction receiver works`() {
            withTestEngine {
                val accountOther = createAccount()

                val accountID1 = createRemoteAccount(accountFrom)
                val accountID2 = createRemoteAccount(accountTo)
                val accountID3 = createRemoteAccount(accountOther)

                val depositRequest = BalanceModificationRequest(BigDecimal.TEN.setScale(4))
                // init balance of sender accounts
                handleRequest(HttpMethod.Post, "/accounts/$accountID1/deposit") {
                    setJsonBody(depositRequest)
                }

                // make transfers account1 -> account2 and account1 -> account3
                handleRequest(HttpMethod.Post, "/transfer") {
                    setJsonBody(TransferRequest(accountID1, accountID2, BigDecimal.ONE))
                }
                handleRequest(HttpMethod.Post, "/transfer") {
                    setJsonBody(TransferRequest(accountID1, accountID3, BigDecimal.ONE))
                }

                handleRequest(HttpMethod.Get, "/transactions?to=$accountID2").apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val responseAccounts = response.readJsonList<Transaction>()
                    assertEquals(1, responseAccounts.size)
                    assertEquals(accountID2, responseAccounts[0].accountTo)
                }
            }
        }
    }

    @Nested
    inner class Transfer {
        private val accountFrom = createAccount()
        private val accountTo = createAccount()

        @Test
        fun `transfers can be performed successfully`() {
            withTestEngine {
                val accountID1 = createRemoteAccount(accountFrom)
                val accountID2 = createRemoteAccount(accountTo)
                val depositRequest = BalanceModificationRequest(BigDecimal.TEN.setScale(4))
                handleRequest(HttpMethod.Post, "/accounts/$accountID1/deposit") {
                    setJsonBody(depositRequest)
                }

                handleRequest(HttpMethod.Post, "/transfer") {
                    setJsonBody(TransferRequest(accountID1, accountID2, BigDecimal.ONE))
                }.run {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val transactionID = getIDFromJson(response.content!!)
                    Thread.sleep(200)
                    handleRequest(HttpMethod.Get, "transactions/$transactionID").apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val transaction = response.readJsonModel<Transaction>()
                        assertEquals(TransactionStatus.SUCCESS, transaction.status)
                    }
                }
            }
        }

        @Test
        fun `transfer fails in case of insufficient amount`() {
            withTestEngine {
                val accountID1 = createRemoteAccount(accountFrom)
                val accountID2 = createRemoteAccount(accountTo)

                handleRequest(HttpMethod.Post, "/transfer") {
                    setJsonBody(TransferRequest(accountID1, accountID2, BigDecimal.ONE))
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val transactionID = getIDFromJson(response.content!!)
                    Thread.sleep(200)
                    handleRequest(HttpMethod.Get, "transactions/$transactionID").apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val transaction = response.readJsonModel<Transaction>()
                        assertEquals(TransactionStatus.FAILED, transaction.status)
                    }
                }
            }
        }

        @Test
        fun `transfer of negative amount fails`() {
            withTestEngine {
                val accountID1 = createRemoteAccount(accountFrom)
                val accountID2 = createRemoteAccount(accountTo)

                handleRequest(HttpMethod.Post, "/transfer") {
                    setJsonBody(TransferRequest(accountID1, accountID2, -BigDecimal.ONE))
                }.apply {
                    assertEquals(HttpStatusCode.BadRequest, response.status())
                }
            }
        }
    }

    /**
     * Create bew account, while filling missing constructor values
     */
    private fun createAccount(
        bank: String = "BANK",
        number: String = "0123456789",
        userId: String = "userID",
        currency: Currency = Currency.USD
    ) = Account(bank, number, userId, currency)

    /**
     * Set request body accountTo given object
     */
    private fun TestApplicationRequest.setJsonBody(value: Any?) {
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(mapper.writeValueAsString(value))
    }

    /**
     * Read object accountFrom response
     */
    private inline fun <reified T> TestApplicationResponse.readJsonModel() = mapper.readValue(content, T::class.java)

    /**
     * Read list of objects accountFrom response
     */
    private inline fun <reified T> TestApplicationResponse.readJsonList(): List<T> = mapper.readValue(
        content,
        mapper.typeFactory.constructCollectionType(List::class.java, T::class.java)
    )

    /**
     * Fetch UUID accountFrom json-encoded object
     */
    private fun getIDFromJson(json: String): UUID {
        return UUID.fromString(mapper.readValue(json, ObjectNode::class.java)["id"].asText())
    }

    /**
     * Use initialize test environment
     */
    fun <T> withTestEngine(test: TestApplicationEngine.() -> T): T {
        return withApplication(createTestEnvironment()) {
            application.module()
            test()
        }
    }


    fun TestApplicationEngine.createRemoteAccount(account: Account): UUID {
        return handleRequest(HttpMethod.Post, "/accounts") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setJsonBody(account)
        }.run { getIDFromJson(response.content!!) }
    }

    private fun assertEqualsIgnoringID(account1: Account, account2: Account) {
        val account2WithID1 = account2.copy(id = account1.id)
        assertEquals(account1, account2WithID1)
    }
}