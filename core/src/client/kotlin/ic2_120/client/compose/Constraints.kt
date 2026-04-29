package ic2_120.client.compose

data class Constraints(
    val maxWidth: Int = Int.MAX_VALUE,
    val maxHeight: Int = Int.MAX_VALUE
)

data class Size(val width: Int, val height: Int)
