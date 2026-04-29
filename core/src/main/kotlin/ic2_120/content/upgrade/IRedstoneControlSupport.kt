package ic2_120.content.upgrade

/**
 * 机器红石控制支持接口。
 *
 * 实现该接口的机器可被红石信号控制启停。
 * - 默认模式：有红石信号时运行，无信号时停机
 * - 反转模式：有红石信号时停机，无信号时运行
 */
interface IRedstoneControlSupport {
    /** 是否反转红石逻辑（常用于红石反转升级） */
    var redstoneInverted: Boolean
}

