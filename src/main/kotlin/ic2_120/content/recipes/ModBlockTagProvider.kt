package ic2_120.content.recipes

import ic2_120.Ic2_120
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.pipes.BasePipeBlock
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.BlockTags
import java.util.concurrent.CompletableFuture

/**
 * 生成方块标签。
 * 将机器方块加入 mineable/pickaxe 和 needs_iron_tool，
 * 使铁镐能按设定硬度正常挖掘，并正确掉落。
 */
class ModBlockTagProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<RegistryWrapper.WrapperLookup>,
) : FabricTagProvider.BlockTagProvider(output, registriesFuture) {
    private val oreBlockIds = setOf(
        "lead_ore",
        "tin_ore",
        "uranium_ore",
        "iridium_ore",
        "deepslate_lead_ore",
        "deepslate_tin_ore",
        "deepslate_uranium_ore",
    )

    override fun configure(registries: RegistryWrapper.WrapperLookup) {
        val pickaxeBuilder = getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).setReplace(false)
        val ironToolBuilder = getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).setReplace(false)

        for (block in Registries.BLOCK) {
            val id = Registries.BLOCK.getId(block)
            if (id.namespace == Ic2_120.MOD_ID && (block is MachineBlock || block is BasePipeBlock || id.path in oreBlockIds)) {
                pickaxeBuilder.add(block)
                ironToolBuilder.add(block)
            }
        }
    }
}

