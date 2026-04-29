package ic2_120.content.sync

import ic2_120.content.syncs.SyncSchema

class CokeKilnSync(schema: SyncSchema) {
    companion object {
        const val PROGRESS_MAX = 1800
    }

    var progress by schema.int("Progress")
    var structureValid by schema.int("StructureValid")
}
