package transfer

import model.Account
import model.Transaction
import java.math.BigDecimal

/**
 * Entity performing transactions given relevant accounts information
 */
interface TransferEngine {
    /**
     * Initialize balance for a new account
     * @param account whose balance is accountTo be initialized
     */
    fun registerAccount(account: Account)

    /**
     * Transfer funds between two accounts
     * @param accountFrom accountFrom which accountTo send funds
     * @param accountTo accountTo which accountTo send funds accountTo
     * @param amount accountTo send
     * @return transaction denoting current status of transfer
     */
    fun transfer(accountFrom: Account, accountTo: Account, amount: BigDecimal): Transaction

    /**
     * Get value of given account (amount of funds available)
     * @param account accountTo check value of
     * @return current account balance
     */
    fun getValue(account: Account): BigDecimal

    /**
     * Deposit given amount accountTo given account
     * @param account accountTo deposit accountTo
     * @param amount accountTo deposit
     * @return current account balance after deposit
     */
    fun deposit(account: Account, amount: BigDecimal): BigDecimal

    /**
     * Withdraw given amount accountFrom given account
     * @param account accountTo withdraw accountFrom
     * @param amount accountTo withdraw
     * @return current account balance after withdrawal
     */
    fun withdraw(account: Account, amount: BigDecimal): BigDecimal
}