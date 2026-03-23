package ic2_120.content.item

import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.api.Environment
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.Direction
import net.minecraft.world.World

// ========== 升级物品接口 ==========

/**
 * 机器升级物品标记接口。
 * 实现此接口的物品可放入机器的升级槽位。
 * 各升级的实际效果由机器在 tick 中自行读取并应用，此处不定义。
 */
interface IUpgradeItem

abstract class FluidFilterUpgradeItem : Item(FabricItemSettings()), IUpgradeItem {
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val filter = FluidPipeUpgradeComponent.readFilter(stack)
        if (filter != null) {
            val fluidName = filter.defaultState.blockState.block.name
            tooltip.add(Text.literal("过滤: ").append(fluidName).formatted(Formatting.GRAY))
        } else {
            tooltip.add(Text.literal("过滤: 未设置").formatted(Formatting.GRAY))
        }
        val side = FluidPipeUpgradeComponent.readDirection(stack)
        tooltip.add(Text.literal("方向: ${directionLabel(side)}").formatted(Formatting.GRAY))
    }

    override fun use(world: World, user: net.minecraft.entity.player.PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(stack)

        if (user.isSneaking) {
            val next = FluidPipeUpgradeComponent.nextDirection(FluidPipeUpgradeComponent.readDirection(stack))
            FluidPipeUpgradeComponent.writeDirection(stack, next)
            if (user is ServerPlayerEntity) {
                user.sendMessage(Text.literal("已设置工作方向: ${directionLabel(next)}"), true)
            }
            return TypedActionResult.success(stack)
        }

        val offhand = user.getStackInHand(if (hand == Hand.MAIN_HAND) Hand.OFF_HAND else Hand.MAIN_HAND)
        val storage = FluidStorage.ITEM.find(offhand, net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(offhand))
        val view = storage?.iterator()?.asSequence()?.firstOrNull { !it.resource.isBlank && it.amount > 0L }
        val fluid = view?.resource?.fluid

        if (fluid == null) {
            if (user is ServerPlayerEntity) {
                user.sendMessage(Text.literal("副手放入含流体容器后右键设置过滤；潜行右键清除"), true)
            }
            return TypedActionResult.fail(stack)
        }

        FluidPipeUpgradeComponent.writeFilter(stack, fluid)
        if (user is ServerPlayerEntity) {
            user.sendMessage(Text.literal("已设置过滤流体: ${fluid.defaultState.blockState.block.name.string}"), true)
        }
        return TypedActionResult.success(stack)
    }

    private fun directionLabel(side: Direction?): String = when (side) {
        null -> "任意"
        Direction.DOWN -> "下"
        Direction.UP -> "上"
        Direction.NORTH -> "北"
        Direction.SOUTH -> "南"
        Direction.WEST -> "西"
        Direction.EAST -> "东"
    }
}

abstract class ItemFilterUpgradeItem : Item(FabricItemSettings()), IUpgradeItem {
    @Environment(EnvType.CLIENT)
    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val filterItem = EjectorUpgradeComponent.readFilter(stack)
        if (filterItem != null) {
            tooltip.add(Text.literal("过滤物品: ").append(filterItem.name).formatted(Formatting.GRAY))
        } else {
            tooltip.add(Text.literal("过滤物品: 全部").formatted(Formatting.GRAY))
        }
        val side = EjectorUpgradeComponent.readDirection(stack)
        tooltip.add(Text.literal("方向: ${directionLabel(side)}").formatted(Formatting.GRAY))
    }

    override fun use(world: World, user: net.minecraft.entity.player.PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(stack)

        if (user.isSneaking) {
            val next = EjectorUpgradeComponent.nextDirection(EjectorUpgradeComponent.readDirection(stack))
            EjectorUpgradeComponent.writeDirection(stack, next)
            if (user is ServerPlayerEntity) {
                user.sendMessage(Text.literal("已设置弹出方向: ${directionLabel(next)}"), true)
            }
            return TypedActionResult.success(stack)
        }

        val offhand = user.getStackInHand(if (hand == Hand.MAIN_HAND) Hand.OFF_HAND else Hand.MAIN_HAND)
        if (offhand.isEmpty) {
            EjectorUpgradeComponent.writeFilter(stack, null)
            if (user is ServerPlayerEntity) {
                user.sendMessage(Text.literal("已清除物品过滤（当前为全部物品）"), true)
            }
            return TypedActionResult.success(stack)
        }

        EjectorUpgradeComponent.writeFilter(stack, offhand.item)
        if (user is ServerPlayerEntity) {
            user.sendMessage(Text.literal("已设置过滤物品: ${offhand.name.string}"), true)
        }
        return TypedActionResult.success(stack)
    }

    private fun directionLabel(side: Direction?): String = when (side) {
        null -> "任意"
        Direction.DOWN -> "下"
        Direction.UP -> "上"
        Direction.NORTH -> "北"
        Direction.SOUTH -> "南"
        Direction.WEST -> "西"
        Direction.EAST -> "东"
    }
}

// ========== 工具升级类 ==========

@ModItem(name = "overclocker_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class OverclockerUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "transformer_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class TransformerUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "energy_storage_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class EnergyStorageUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "redstone_inverter_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class RedstoneInverterUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class EjectorUpgrade : ItemFilterUpgradeItem()

@ModItem(name = "advanced_ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class AdvancedEjectorUpgrade : ItemFilterUpgradeItem()

@ModItem(name = "pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class PullingUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "advanced_pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class AdvancedPullingUpgrade : Item(FabricItemSettings()), IUpgradeItem

@ModItem(name = "fluid_ejector_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class FluidEjectorUpgrade : FluidFilterUpgradeItem()

@ModItem(name = "fluid_pulling_upgrade", tab = CreativeTab.IC2_MATERIALS, group = "upgrades")
class FluidPullingUpgrade : FluidFilterUpgradeItem()
