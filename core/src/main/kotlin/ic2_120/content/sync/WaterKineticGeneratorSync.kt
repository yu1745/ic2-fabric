package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

class WaterKineticGeneratorSync(schema: SyncSchema) {
    var isStuck by schema.int("IsStuck")
    var stuckAngle by schema.int("StuckAngle")
    var isSubmerged by schema.int("IsSubmerged")
    var waterFlowBonus by schema.int("WaterFlowBonus")
    var generatedKu by schema.int("GeneratedKu")
    var outputKu by schema.int("OutputKu")
    var rotorLifetimeTenthsHours by schema.int("RotorLifetimeTenthsHours")
}