package transfer

import java.math.BigDecimal
import java.math.RoundingMode

// precision of floating point operations. Ideally should be part of config
const val transactionPrecision = 4

/**
 * Stores information about account balance.
 * This is internal representation of in-memory transfer engine
 */
data class Balance(
    var value: BigDecimal = BigDecimal.ZERO.setScale(transactionPrecision, RoundingMode.DOWN)
)