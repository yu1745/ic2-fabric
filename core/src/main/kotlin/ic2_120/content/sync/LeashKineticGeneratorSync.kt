package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

class LeashKineticGeneratorSync(schema: SyncSchema) {
    var generatedKu by schema.int("GeneratedKu")
    var outputKu by schema.int("OutputKu")

    var hasAnimal: Boolean
        get() = _hasAnimalInt != 0
        set(value) { _hasAnimalInt = if (value) 1 else 0 }
    private var _hasAnimalInt by schema.int("HasAnimal", default = 0)

    var leashLengthCm by schema.int("LeashLengthCm")
    var angularVelocityDegPerSec by schema.int("AngularVelocityDegPerSec")

    var animalAngle: Float
        get() = _animalAngleInt / 10f
        set(value) { _animalAngleInt = (value * 10).toInt() }
    private var _animalAngleInt by schema.int("AnimalAngle", default = 0)
}
