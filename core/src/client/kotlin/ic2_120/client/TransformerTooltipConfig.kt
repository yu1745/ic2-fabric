package ic2_120.client

/**
 * 变压器 tooltip 显示控制
 * 用于控制 TransformerTooltipMixin 中的开关
 */
object TransformerTooltipConfig {
    /**
     * 设置是否显示变压器模式 tooltip
     */
    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        try {
            val mixinClass = Class.forName("ic2_120.mixin.client.TransformerTooltipMixin")
            val field = mixinClass.getDeclaredField("showTransformerTooltip")
            field.isAccessible = true
            field.setBoolean(null, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取当前是否显示变压器模式 tooltip
     */
    @JvmStatic
    fun isEnabled(): Boolean {
        return try {
            val mixinClass = Class.forName("ic2_120.mixin.client.TransformerTooltipMixin")
            val field = mixinClass.getDeclaredField("showTransformerTooltip")
            field.isAccessible = true
            field.getBoolean(null)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 切换显示状态
     */
    @JvmStatic
    fun toggle() {
        setEnabled(!isEnabled())
    }
}
