package api

import model.Currency
import java.math.BigDecimal
import java.util.*

data class BalanceModificationRequest(val amount: BigDecimal)

data class BalanceResponse(val balance: BigDecimal, val currency: Currency)

data class TransferRequest(val accountFrom: UUID, val accountTo: UUID, val amount: BigDecimal)