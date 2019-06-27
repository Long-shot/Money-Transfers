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
            fun `test create account success`() {
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
            fun `test create invalid account failure`() {
                val account = createAccount(currency = Currency.USD)
                var json = mapper.writeValueAsString(account)
                // Set currency to unsupported value
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
            fun `test get all existing accounts`() {
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
            fun `test get single existing account succeeds`() {
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
        }
        @Nested
        inner class Delete {
            @Test
            fun `test delete account success`() {
                // TODO
            }

            @Test
            fun `test fail on delete non-existing account`() {
                // TODO
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
        fun `test success fetching all transactions`() {
            // TODO
        }

        @Test
        fun `test success fetching signle transactions`() {
            // TODO
        }

        @Test
        fun `test fail on fetching non-existent transactions`() {
            // TODO
        }

        @Test
        fun `test filtering on transaction issuer`() {
            // TODO
        }

        @Test
        fun `test filtering on transaction receiver`() {
            // TODO
        }
    }

    @Nested
    inner class Transfer {
        @Test
        fun `test successfull transfer`(){
            // TODO
        }

        @Test
        fun `test concurrent transfer requests`(){
            // TODO
        }

        @Test
        fun `test fail in mismatching currencies`() {
            // TODO
        }

        @Test
        fun `test fail in insufficient amount`() {
            // TODO
        }

        @Test
        fun `test fail on negative transfer amount`() {
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
     * Set request body to given object
     */
    private fun TestApplicationRequest.setJsonBody(value: Any?) = setBody(mapper.writeValueAsString(value))

    /**
     * Read object from response
     */
    private inline fun <reified T> TestApplicationResponse.readJsonModel() = mapper.readValue(content, T::class.java)
    /**
     * Read list of objects from response
     */
    private inline fun <reified T> TestApplicationResponse.readJsonList() : List<T> = mapper.readValue(content,
        mapper.typeFactory.constructCollectionType(List::class.java, T::class.java))

    /**
     * Fetch UUID from json-encoded object
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