package transfer

import dao.InMemoryIndexedDao
import kotlinx.coroutines.runBlocking
import model.Account
import model.Currency
import model.Transaction
import model.TransactionStatus
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

const val TASKS = 1000000

class TransferEngineStressTest {
    private val transactionDao = InMemoryIndexedDao<Transaction>()
    private val engine = InMemoryTransferEngine(transactionDao)

    @Nested
    inner class BalanceManipulations {
        private val account = createAccount()

        init {
            engine.registerAccount(account)
        }

        @Test
        fun `depositing multiple times works fine`() {
            runBlocking {
                repeat(TASKS) {
                    engine.deposit(account, BigDecimal.ONE)
                }
            }
            assertEquals(engine.getValue(account), BigDecimal(TASKS).setScale(4))
        }

        @Test
        fun `withdrawing multiple times works fine`() {
            engine.deposit(account, BigDecimal(TASKS))

            runBlocking {
                repeat(TASKS) {
                    engine.withdraw(account, BigDecimal.ONE)
                }
            }
            assertEquals(engine.getValue(account), BigDecimal.ZERO.setScale(4))
        }

        @Test
        fun `mixing depositing and withdrawing works fine`() {
            engine.deposit(account, BigDecimal(TASKS))

            runBlocking {
                repeat(TASKS) {
                    engine.withdraw(account, BigDecimal.ONE)
                }
                repeat(TASKS) {
                    engine.deposit(account, BigDecimal.ONE)
                }
            }
            assertEquals(engine.getValue(account), BigDecimal(TASKS).setScale(4))
        }
    }

    @Nested
    inner class Transfer {
        private val account1 = createAccount()
        private val account2 = createAccount()

        init {
            engine.registerAccount(account1)
            engine.registerAccount(account2)
            engine.deposit(account1, BigDecimal(TASKS))
            engine.deposit(account2, BigDecimal(TASKS))
        }

        @Test
        fun `transferring between accounts doesn't break consistency`() {
            runBlocking {
                repeat(TASKS) {
                    engine.transfer(account1, account2, BigDecimal.ONE)
                }
                repeat(TASKS) {
                    engine.transfer(account2, account1, BigDecimal.ONE)
                }
            }
            Thread.sleep(200) // wait for transactions to finish
            val expectedTotalBalance = BigDecimal(TASKS * 2).setScale(4)
            assertEquals(expectedTotalBalance, engine.getValue(account1) + engine.getValue(account2))
            val transactions = transactionDao.getAll()
            assertEquals(transactions.size, 2 * TASKS)
            transactions.forEach { assertEquals(TransactionStatus.SUCCESS, it.status) }
        }
    }


    fun createAccount(): Account {
        return Account("ABN", "0123456789", "user_id", Currency.USD)
    }
}