package ic2_120.content.upgrade

import ic2_120.content.item.PullingUpgrade
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

object PullingUpgradeComponent {

    /**
     * 扫描升级槽中的所有抽入升级，从相邻容器抽取物品到机器的输入槽。
     * 每个抽入升级使用自己的过滤和方向配置。
     * 复用 EjectorUpgradeComponent 的 NBT 读写方法（过滤和方向配置格式一致）。
     */
    fun pullIfUpgraded(
        world: World,
        pos: BlockPos,
        inventory: Inventory,
        upgradeSlotIndices: IntArray,
        inputSlotIndices: IntArray
    ) {
        if (inputSlotIndices.isEmpty()) return

        for (idx in upgradeSlotIndices) {
            val upgradeStack = inventory.getStack(idx)
            if (upgradeStack.isEmpty || upgradeStack.item !is PullingUpgrade) continue

            val filter = EjectorUpgradeComponent.readFilter(upgradeStack)
            val side = EjectorUpgradeComponent.readDirection(upgradeStack)
            val dirs = if (side != null) listOf(side) else Direction.values().toList()

            for (dir in dirs) {
                val source = ItemStorage.SIDED.find(world, pos.offset(dir), dir.opposite) ?: continue

                // 遍历源容器中的所有物品变体
                for (view in source) {
                    if (view.isResourceBlank()) continue
                    val variant = view.resource as? ItemVariant ?: continue
                    val item = variant.item

                    // 应用过滤
                    if (filter != null && item != filter) continue

                    // 找到可以接受此物品的输入槽
                    for (slotIndex in inputSlotIndices) {
                        val current = inventory.getStack(slotIndex)
                        if (!current.isEmpty) {
                            if (current.item != item) continue
                            if (current.count >= current.maxCount) continue
                        }

                        // 检查机器是否接受该物品
                        if (!inventory.isValid(slotIndex, variant.toStack(1))) continue

                        val space = if (current.isEmpty) current.maxCount else current.maxCount - current.count
                        if (space <= 0) continue

                        // 尝试从源容器中抽取物品
                        Transaction.openOuter().use { tx ->
                            val extracted = source.extract(variant, space.toLong(), tx)
                            if (extracted > 0) {
                                tx.commit()
                                // 将物品放入输入槽
                                if (current.isEmpty) {
                                    inventory.setStack(slotIndex, variant.toStack(extracted.toInt()))
                                } else {
                                    val newStack = current.copy()
                                    newStack.count += extracted.toInt()
                                    inventory.setStack(slotIndex, newStack)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
