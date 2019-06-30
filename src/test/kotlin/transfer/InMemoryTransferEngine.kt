package transfer

import dao.InMemoryIndexedDao
import model.Account
import model.Currency
import model.Transaction
import model.TransactionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertFailsWith

class InMemoryIndexedDaoTest {
    private val transactionDao = InMemoryIndexedDao<Transaction>()
    private val engine = InMemoryTransferEngine(transactionDao)

    @Nested
    inner class RegisterAccount {
        @Test
        fun `accounts can be registered successfully`() {
            for (i in 1..10) {
                val account = createAccount()
                engine.registerAccount(account)
                assertEquals(engine.getValue(account), BigDecimal.ZERO.setScale(4))
            }
        }

        @Test
        fun `registering account twice fails`() {
            val account = createAccount()
            engine.registerAccount(account)
            assertFailsWith<IllegalArgumentException> { engine.registerAccount(account) }
        }
    }

    @Nested
    inner class GetValue {
        @Test
        fun `new accounts have zero funds`() {
            val account = createEngineAccount()
            assertEquals(engine.getValue(account), BigDecimal.ZERO.setScale(4))
        }

        @Test
        fun `getting value of non-existing account fails`() {
            val account = createAccount()
            assertFailsWith<InvalidAccountException> { engine.getValue(account) }
        }

        @Test
        fun `modifying account funds reflects on retrieved vale`() {
            val account = createEngineAccount()
            val balance = BigDecimal(100)
            engine.deposit(account, balance)
            assertEquals(balance.setScale(4), engine.getValue(account))
        }
    }

    @Nested
    inner class Deposit {
        private val account = createEngineAccount()
        @Test
        fun `depositing integer amount succeeds`() {
            val deposit = BigDecimal(100000).setScale(4)
            engine.deposit(account, deposit)
            assertEquals(deposit, engine.getValue(account))
        }

        @Test
        fun `depositing huge amount succeeds`() {
            val deposit = BigDecimal("10000000000000000000000000000000000000000000").setScale(4)
            engine.deposit(account, deposit)
            assertEquals(deposit, engine.getValue(account))
        }

        @Test
        fun `depositing negative amount fails`() {
            val deposit = BigDecimal(-1).setScale(4)
            assertFailsWith<IllegalAmountException> { engine.deposit(account, deposit) }
        }

        @Test
        fun `decimals after precision threshold of (4) are ignored`() {
            val deposit = BigDecimal("10.00019")
            engine.deposit(account, deposit)
            assertEquals(deposit.setScale(4, RoundingMode.DOWN), engine.getValue(account))
        }
    }

    @Nested
    inner class Withdraw {
        private val balance = BigDecimal(1000).setScale(4)
        private val account = createEngineAccount(balance)

        @Test
        fun `depositing integer amount succeeds`() {
            val withdraw = BigDecimal(100)
            engine.withdraw(account, withdraw)
            val expected = balance - withdraw.setScale(4)
            assertEquals(expected, engine.getValue(account))
        }

        @Test
        fun `withdrawing huge amount succeeds`() {
            // add funds
            val deposit = BigDecimal("10000000000000000000000000000000000000000000").setScale(4)
            engine.deposit(account, deposit)

            engine.withdraw(account, deposit)
            assertEquals(balance, engine.getValue(account))
        }

        @Test
        fun `withdrawing negative amount fails`() {
            val withdraw = BigDecimal(-1).setScale(4)
            assertFailsWith<IllegalAmountException> { engine.withdraw(account, withdraw) }
        }

        @Test
        fun `decimals after precision threshold of (4) are ignored`() {
            val withdraw = BigDecimal("10.00009")
            engine.withdraw(account, withdraw)
            val expected = balance - BigDecimal.TEN
            assertEquals(expected, engine.getValue(account))
        }

        @Test
        fun `withdrawing more than entire balance fails`() {
            val withdraw = balance + BigDecimal.TEN
            assertFailsWith<InsufficientFundsException> { engine.withdraw(account, withdraw) }
        }
    }

    @Nested
    inner class Transfer {
        private val balance = BigDecimal(1000).setScale(4)
        private val accountFrom = createEngineAccount(balance)
        private val accountTo = createEngineAccount()

        @Test
        fun `transfer between accounts with different currencies fails`() {
            val mismatchingAccount = createEngineAccount(currency = Currency.EUR)
            val transferAmount = BigDecimal.ONE
            assertFailsWith<AccountMismatchException> {
                engine.transfer(
                    accountFrom,
                    mismatchingAccount,
                    transferAmount
                )
            }
        }

        @Test
        fun `transfer of negative amount fails`() {
            val transferAmount = -BigDecimal.ONE
            assertFailsWith<IllegalAmountException> { engine.transfer(accountFrom, accountTo, transferAmount) }
        }

        @Test
        fun `transfer from non-existing account fails`() {
            val missingAccount = createAccount()
            val transferAmount = BigDecimal.ONE
            assertFailsWith<InvalidAccountException> { engine.transfer(missingAccount, accountTo, transferAmount) }
        }

        @Test
        fun `transfer to non-existing account fails`() {
            val missingAccount = createAccount()
            val transferAmount = BigDecimal.ONE
            assertFailsWith<InvalidAccountException> { engine.transfer(accountFrom, missingAccount, transferAmount) }
        }

        @Test
        fun `transfer returns pending transaction`() {
            val transferAmount = BigDecimal.TEN
            val transaction = engine.transfer(accountFrom, accountTo, transferAmount)
            assertEquals(TransactionStatus.PENDING, transaction.status)
        }

        @Test
        fun `pending transaction succeeds with sufficient funds`() {
            val transferAmount = BigDecimal.TEN
            val transaction = engine.transfer(accountFrom, accountTo, transferAmount)

            Thread.sleep(200) // wait for transaction to finish

            val expectedAmount = balance - transferAmount

            assertEquals(expectedAmount, engine.getValue(accountFrom))
            assertEquals(transferAmount.setScale(4), engine.getValue(accountTo))

            val transactions = transactionDao.getAll()
            assertEquals(transactions.size, 1)
            val updatedTransaction = transactions[0]
            assertEquals(transaction.id, updatedTransaction.id)
            assertEquals(TransactionStatus.SUCCESS, updatedTransaction.status)
        }

        @Test
        fun `pending transaction fails on sufficient funds`() {
            val transferAmount = BigDecimal("1000000")
            val transaction = engine.transfer(accountFrom, accountTo, transferAmount)

            Thread.sleep(200) // wait for transaction to finish

            assertEquals(balance, engine.getValue(accountFrom))
            assertEquals(BigDecimal.ZERO.setScale(4), engine.getValue(accountTo))

            val transactions = transactionDao.getAll()
            assertEquals(transactions.size, 1)
            val updatedTransaction = transactions[0]
            assertEquals(transaction.id, updatedTransaction.id)
            assertEquals(TransactionStatus.FAILED, updatedTransaction.status)
        }
    }

    private fun createAccount(currency: Currency = Currency.USD): Account {
        return Account("ABN", "0123456789", "user_id", currency)
    }

    private fun createEngineAccount(
        initialBalance: BigDecimal = BigDecimal.ZERO,
        currency: Currency = Currency.USD
    ): Account {
        val account = createAccount(currency)
        engine.registerAccount(account)
        if (initialBalance != BigDecimal.ZERO) {
            engine.deposit(account, initialBalance)
        }
        return account
    }
}
