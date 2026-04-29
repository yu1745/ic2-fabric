package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

class ManualKineticGeneratorSync(schema: SyncSchema) {
    var storedKu by schema.int("StoredKu")
    var extractedKu by schema.int("ExtractedKu")
    var outputKu by schema.int("OutputKu")  // 当前输出速度 KU/t
    
    // 曲柄相关同步数据（使用 int 模拟 boolean 和 float）
    // hasCrank: 0 = false, 1 = true
    var hasCrank: Boolean
        get() = _hasCrankInt != 0
        set(value) { _hasCrankInt = if (value) 1 else 0 }
    private var _hasCrankInt by schema.int("HasCrank", default = 0)
    
    // crankMaterial: 0=无, 1=木, 2=铁, 3=钢, 4=碳
    var crankMaterial by schema.int("CrankMaterial", default = 0)
    
    // isTurning: 0 = false, 1 = true
    var isTurning: Boolean
        get() = _isTurningInt != 0
        set(value) { _isTurningInt = if (value) 1 else 0 }
    private var _isTurningInt by schema.int("IsTurning", default = 0)
    
    // turnAngle: 存储为 0-3600 的定点数（0.1度精度）
    var turnAngle: Float
        get() = _turnAngleInt / 10f
        set(value) { _turnAngleInt = (value * 10).toInt() }
    private var _turnAngleInt by schema.int("TurnAngle", default = 0)
}
