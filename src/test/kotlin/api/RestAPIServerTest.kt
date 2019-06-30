package api

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.ktor.server.testing.*
import model.*
import model.Currency
import module
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals


class RestAPIServerTest {
    private val mapper = jacksonObjectMapper()

    @Nested
    inner class Accounts {
        @Nested
        inner class Create {
            @Test
            fun `valid account creation succeeds`() {
                val account = createAccount()

                withTestEngine {
                    createRemoteAccount(account).apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val respAccount = response.readJsonModel<Account>()
                        assertNotEquals(account.id, respAccount.id)
                        // id is not a primary field of Account -> it's not in equals
                        assertEquals(account, respAccount)
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
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
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
                    for (i in 1..5){
                        val account = createAccount()
                        createRemoteAccount(account)

                        handleRequest(HttpMethod.Get, "/accounts") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        }.apply {
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
                var accountID : UUID
                withTestEngine {
                    createRemoteAccount(account).apply {
                        accountID = getIDFromJson(response.content!!)
                    }

                    handleRequest(HttpMethod.Get, "/accounts/$accountID") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val respAccount = response.readJsonModel<Account>()
                        assertEquals(account, respAccount)
                    }
                }
            }

            @Test
            fun `fetching non-existing account fails`() {
                val account = createAccount()
                withTestEngine {
                    handleRequest(HttpMethod.Get, "/accounts/${account.id}") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.apply {
                        assertEquals(HttpStatusCode.NotFound, response.status())
                    }
                }
            }
        }
        @Nested
        inner class Delete {
            @Test
            fun `test delete account success`() {
                val account = createAccount()
                var accountID : UUID
                withTestEngine {
                    createRemoteAccount(account).apply {
                        accountID = getIDFromJson(response.content!!)
                    }

                    handleRequest(HttpMethod.Delete, "/accounts/$accountID") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val respAccount = response.readJsonModel<Account>()
                        assertEquals(account, respAccount)
                    }
                }
            }

            @Test
            fun `test fail on delete non-existing account`() {
                val account = createAccount()
                withTestEngine {
                    handleRequest(HttpMethod.Delete, "/accounts/${account.id}") {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }.apply {
                        assertEquals(HttpStatusCode.NotFound, response.status())
                    }
                }
            }
        }

        @Nested
        inner class Balance {
            @Test
            fun `test initial balance is zero`() {
                TODO()
            }

            @Test
            fun `test error on balance of non-existing account`() {
                TODO()
            }
        }

        @Nested
        inner class Deposit {
            @Test
            fun `test successful deposit to account`() {
                TODO()
            }

            @Test
            fun `test error on deposit to non-existent account`() {
                TODO()
            }
        }

        @Nested
        inner class Withdraw {
            @Test
            fun `test successful withdrawal from account`() {
                TODO()
            }

            @Test
            fun `test fail on withdrawal from non-existing account`() {
                TODO()
            }

            @Test
            fun `test fail on insufficient funds`() {
                TODO()
            }
        }
    }

    @Nested
    inner class Transactions {
        @Test
        fun `fetching all transactions succeeds`() {
            // TODO
        }

        @Test
        fun `fetching signle transaction succeeds`() {
            // TODO
        }

        @Test
        fun `fetching non-existent transaction fails`() {
            // TODO
        }

        @Test
        fun `filtering on transaction issuer works`() {
            // TODO
        }

        @Test
        fun `filtering on transaction receiver works`() {
            // TODO
        }
    }

    @Nested
    inner class Transfer {
        @Test
        fun `transfers can be performed successfully`(){
            // TODO
        }

        @Test
        fun `concurrent transfer requests work`(){
            // TODO
        }

        @Test
        fun `transfer between accounts with mismatching currencies fails`() {
            // TODO
        }

        @Test
        fun `transfer fails in case of insufficient amount`() {
            // TODO
        }

        @Test
        fun `transfer of negative amount fails`() {
            // TODO
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
    private fun TestApplicationRequest.setJsonBody(value: Any?) = setBody(mapper.writeValueAsString(value))

    /**
     * Read object accountFrom response
     */
    private inline fun <reified T> TestApplicationResponse.readJsonModel() = mapper.readValue(content, T::class.java)
    /**
     * Read list of objects accountFrom response
     */
    private inline fun <reified T> TestApplicationResponse.readJsonList() : List<T> = mapper.readValue(content,
        mapper.typeFactory.constructCollectionType(List::class.java, T::class.java))

    /**
     * Fetch UUID accountFrom json-encoded object
     */
    private fun getIDFromJson(json: String) : UUID {
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

    fun TestApplicationEngine.createRemoteAccount(account: Account) : TestApplicationCall {
        return handleRequest(HttpMethod.Post, "/accounts") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setJsonBody(account)
        }
    }
}