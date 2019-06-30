package transfer

import model.Account
import java.math.BigDecimal

class InsufficientFundsException(message: String) : Exception(message) {
    constructor(account: Account) : this("Account [${account.id}] has insufficient funds")
}

class AccountMismatchException(message: String) : Exception(message) {
    constructor(
        account1: Account,
        account2: Account
    ) : this("Can not transfer between accounts [${account1.id}] and [${account2.id}]")
}

class IllegalAmountException(message: String) : Exception(message) {
    constructor(operation: String, amount: BigDecimal) : this("Can not perform [$operation] with amount [$amount]")
}

class InvalidAccountException(message: String) : Exception(message) {
    constructor(account: Account) : this("No balance for account [${account.id}]")
}
