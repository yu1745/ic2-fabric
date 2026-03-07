package ic2_120.client.compose

data class Padding(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val horizontal get() = left + right
    val vertical get() = top + bottom

    companion object {
        val ZERO = Padding()
    }
}

data class Modifier(
    val width: Int? = null,
    val height: Int? = null,
    val padding: Padding = Padding.ZERO,
    val backgroundColor: Int? = null,
    val borderColor: Int? = null,
) {
    fun width(w: Int) = copy(width = w)
    fun height(h: Int) = copy(height = h)
    fun size(w: Int, h: Int) = copy(width = w, height = h)
    fun padding(all: Int) = copy(padding = Padding(all, all, all, all))
    fun padding(horizontal: Int, vertical: Int) = copy(padding = Padding(horizontal, vertical, horizontal, vertical))
    fun padding(left: Int, top: Int, right: Int, bottom: Int) = copy(padding = Padding(left, top, right, bottom))
    fun background(color: Int) = copy(backgroundColor = color)
    fun border(color: Int) = copy(borderColor = color)

    companion object {
        val EMPTY = Modifier()
    }
}
