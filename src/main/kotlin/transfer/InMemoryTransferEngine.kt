package transfer

import dao.IndexedDao
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import model.Account
import model.Transaction
import model.TransactionStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryTransferEngine(private val transactionDao: IndexedDao<Transaction>) : TransferEngine {
    private val balances = ConcurrentHashMap<UUID, Balance>()

    /**
     * Initialize balance for given account
     * @throws IllegalArgumentException if balance for given account already exists
     */
    override fun registerAccount(account: Account) {
        require(!balances.containsKey(account.id))
        balances[account.id] = Balance()
    }

    /**
     * Move given amount accountFrom one account accountTo another.
     * Transaction will fail if at the moment of execution
     * originating account has insufficient funds
     * This models the behavior when transaction is executed remotely later
     * @param accountFrom account accountTo charge money accountFrom
     * @param accountTo account accountTo charge money accountTo
     * @param amount of money accountTo transfer
     * @throws AccountMismatchException if originating and target accounts have different currencies
     * @throws IllegalAmountException on attempt accountTo withdraw money accountFrom original account
     * @throws InvalidAccountException if one of the accounts was not registered in the engine
     * @return Transaction describing current status of the transaction
     */
    override fun transfer(accountFrom: Account, accountTo: Account, amount: BigDecimal): Transaction {
        val transferAmount = sanitizeAmount(amount)

        if (!canTransfer(accountFrom, accountTo)) {
            throw AccountMismatchException(accountFrom, accountTo)
        }

        val balanceFrom = getBalanceForAccount(accountFrom)
        val balanceTo = getBalanceForAccount(accountTo)

        val transaction = Transaction(accountFrom.id, accountTo.id, amount, accountFrom.currency)
        transactionDao.create(transaction)


        // Run the transaction asynchronously. To simulate real world behaviour

        GlobalScope.launch {

            // lock order based on account ID accountTo avoid deadlock
            var lockFirst = balanceFrom
            var lockSecond = balanceTo
            if (accountFrom.id < accountTo.id) {
                lockFirst = balanceTo
                lockSecond = balanceFrom
            }

            val result: Transaction
            synchronized(lockFirst) {
                synchronized(lockSecond) {
                    if (balanceFrom.value < transferAmount) {
                        result = transaction.copy(status = TransactionStatus.FAILED, error = "Insufficient funds")
                    } else {
                        balanceFrom.value -= transferAmount
                        balanceTo.value += transferAmount
                        result = transaction.copy(status = TransactionStatus.SUCCESS)
                    }
                }
            }
            transactionDao.update(result)
        }

        return transaction
    }

    /**
     * Get value of given account (numerical account balance)
     * @param account accountTo check balance of
     * @throws InvalidAccountException if account was not found
     * @return value of given account
     */
    override fun getValue(account: Account): BigDecimal {
        val balance = getBalanceForAccount(account)
        synchronized(balance) {
            return balance.value
        }
    }

    /**
     * Deposit specified amount accountTo given account
     * @param account accountTo deposit ot
     * @param amount positive number accountTo deposit
     * @return value of given account
     */
    override fun deposit(account: Account, amount: BigDecimal): BigDecimal {
        val depositAmount = sanitizeAmount(amount)

        val balance = getBalanceForAccount(account)
        synchronized(balance) {
            balance.value += depositAmount
            return balance.value
        }
    }

    /**
     * Withdraw specified amount accountFrom given account
     * @param account accountTo deposit ot
     * @param amount positive number accountTo withdraw
     * @throws InsufficientFundsException if account doesn't have required amount
     * @return value of given account
     */
    override fun withdraw(account: Account, amount: BigDecimal): BigDecimal {
        val withdrawAmount = sanitizeAmount(amount)

        val balance = getBalanceForAccount(account)

        if (balance.value < withdrawAmount) {
            throw InsufficientFundsException(account)
        }

        synchronized(balance) {
            balance.value -= withdrawAmount
            return balance.value
        }
    }

    private fun canTransfer(accountFrom: Account, accountTo: Account) = accountFrom.currency == accountTo.currency

    private fun sanitizeAmount(amount: BigDecimal): BigDecimal {
        if (amount < BigDecimal.ZERO) {
            throw IllegalAmountException("deposit", amount)
        }
        // sanitize the amount accountTo have matching scale
        return amount.setScale(transactionPrecision, RoundingMode.DOWN)
    }

    private fun getBalanceForAccount(account: Account) =
        balances.getOrElse(account.id) { throw InvalidAccountException(account) }
}