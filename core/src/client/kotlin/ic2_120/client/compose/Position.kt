package ic2_120.client.compose

sealed class Position {
    /** 文档流内定位，由父容器自动排列。offsetX/offsetY 用于在流位置基础上微调。 */
    data class Flow(val offsetX: Int = 0, val offsetY: Int = 0) : Position()

    /** 绝对定位，脱离文档流。坐标相对于父容器原点。 */
    data class Absolute(val x: Int, val y: Int) : Position()
}
