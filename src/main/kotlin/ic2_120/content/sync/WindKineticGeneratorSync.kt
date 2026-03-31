package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

/**
 * 风力动能发电机同步属性。
 *
 * 记录转子是否被阻挡（卡住）以及卡住时的角度，
 * 客户端渲染器据此显示停转或旋转。
 */
class WindKineticGeneratorSync(schema: SyncSchema) {

    /** 转子是否被阻挡（0=正常旋转，1=卡住停转） */
    var isStuck by schema.int("IsStuck")

    /** 卡住时转子的角度（0-359），正常旋转时为 0 */
    var stuckAngle by schema.int("StuckAngle")

    /** 当前理论产生的 KU/t。 */
    var generatedKu by schema.int("GeneratedKu")

    /** 当前实际输出到网络的 KU/t。 */
    var outputKu by schema.int("OutputKu")

    /** 转子按晴天标准换算的剩余寿命，单位 0.1 小时。 */
    var rotorLifetimeTenthsHours by schema.int("RotorLifetimeTenthsHours")
}
