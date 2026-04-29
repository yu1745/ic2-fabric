package ic2_120.content

import ic2_120.Ic2_120
import ic2_120.content.block.RubberFaceState
import ic2_120.content.block.RubberLogBlock
import ic2_120.content.block.RubberLogBlockEntity
import ic2_120.content.item.energy.IElectricTool
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.entity.ItemEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World

/**
 * 木龙头与电动树脂提取器与橡胶树原木的交互：
 * - 木龙头：右键湿面提取 1 粘性树脂，耗 1 耐久（共 10 次）
 * - 电动树脂提取器：右键湿面提取 1 粘性树脂，耗 500 EU（共 20 次，10k EU）
 */
object RubberTreetapHandler {

    private val TREETAP_ID = Identifier(Ic2_120.MOD_ID, "treetap")
    private val ELECTRIC_TREETAP_ID = Identifier(Ic2_120.MOD_ID, "electric_treetap")
    private val RESIN_ID = Identifier(Ic2_120.MOD_ID, "resin")
    private val TREETAP_SOUND = SoundEvent.of(Identifier("ic2", "item.treetap.use"))
    private val ELECTRIC_TREETAP_SOUND = SoundEvent.of(Identifier("ic2", "item.treetap.electric.use"))
    private const val EU_PER_USE = 500L

    fun isTreetap(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return Registries.ITEM.getId(stack.item) == TREETAP_ID
    }

    fun isElectricTreetap(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return Registries.ITEM.getId(stack.item) == ELECTRIC_TREETAP_ID
    }

    fun isExtractor(stack: ItemStack): Boolean = isTreetap(stack) || isElectricTreetap(stack)

    fun register() {
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            val stack = player.getStackInHand(hand)
            if (!isExtractor(stack)) return@register ActionResult.PASS

            val pos = hitResult.blockPos
            val state = world.getBlockState(pos)
            val block = state.block

            if (block !is RubberLogBlock) return@register ActionResult.PASS

            val face = hitResult.side
            val faceState = block.getRubberState(state, face)
            if (faceState != RubberFaceState.WET) return@register ActionResult.PASS

            if (world.isClient) return@register ActionResult.SUCCESS

            // 电动树脂提取器：电量不足 500 EU 则不允许提取
            if (isElectricTreetap(stack)) {
                val tool = stack.item as IElectricTool
                if (tool.getEnergy(stack) < EU_PER_USE) return@register ActionResult.FAIL
            }

            val resin = Registries.ITEM.get(RESIN_ID)
            // 采集随机掉落 1-3 个：1个70%、2个20%、3个10%
            val count = when (val r = world.random.nextFloat()) {
                in 0f..0.7f -> 1
                in 0.7f..0.9f -> 2
                else -> 3
            }
            val resinStack = ItemStack(resin, count)
            val spawnX = pos.x + 0.5 + face.offsetX * 0.6
            val spawnY = pos.y + 0.5 + face.offsetY * 0.35
            val spawnZ = pos.z + 0.5 + face.offsetZ * 0.6
            // 粘性树脂从被采集的那一面掉出，并带一点向外的速度
            val itemEntity = ItemEntity(
                world,
                spawnX,
                spawnY,
                spawnZ,
                resinStack
            )
            itemEntity.velocity = itemEntity.velocity.add(
                face.offsetX * 0.08,
                face.offsetY * 0.04,
                face.offsetZ * 0.08
            )
            itemEntity.setToDefaultPickupDelay()
            world.spawnEntity(itemEntity)

            val newState = block.setFaceDry(state, face)
            world.setBlockState(pos, newState)

            // 记录提取时间，用于 1 MC 天后恢复
            (world.getBlockEntity(pos) as? RubberLogBlockEntity)?.setExtractedTime(face, world.time)

            when {
                isElectricTreetap(stack) -> {
                    val tool = stack.item as IElectricTool
                    tool.setEnergy(stack, tool.getEnergy(stack) - EU_PER_USE)
                }
                isTreetap(stack) -> {
                    stack.damage(1, player) { it.sendToolBreakStatus(hand) }
                }
            }

            world.playSound(
                null,
                pos,
                if (isElectricTreetap(stack)) ELECTRIC_TREETAP_SOUND else TREETAP_SOUND,
                SoundCategory.BLOCKS,
                1.0f,
                1.0f
            )
            ActionResult.SUCCESS
        }
    }
}
