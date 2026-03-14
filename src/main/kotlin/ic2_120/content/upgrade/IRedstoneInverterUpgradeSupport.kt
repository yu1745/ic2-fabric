package ic2_120.content.upgrade

/**
 * 机器支持红石反转升级的接口。
 * 机器实现此接口后，可放入红石反转升级，并通过 [IRedstoneControlSupport.redstoneInverted] 控制反转。
 */
interface IRedstoneInverterUpgradeSupport : IRedstoneControlSupport
