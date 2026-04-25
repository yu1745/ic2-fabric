package ic2_120.content

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.entity.ItemEntity
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 创造模式发电机方块物品的掉落实体：不因 6000 tick 自然消失；岩浆/仙人掌/爆炸等不摧毁实体（仍可被玩家拾取）。
 */
object CreativeGeneratorItemEntityHandler {

    private val CREATIVE_GENERATOR_ID = Identifier.of(Ic2_120.MOD_ID, "creative_generator")

    fun register() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, _ ->
            if (entity !is ItemEntity) return@register
            if (Registries.ITEM.getId(entity.stack.item) != CREATIVE_GENERATOR_ID) return@register
            entity.setNeverDespawn()
            entity.isInvulnerable = true
        }
    }
}
