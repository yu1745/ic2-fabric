package ic2_120.content.item.armor

import ic2_120.config.Ic2Config
import ic2_120.content.item.IridiumPlate
import ic2_120.content.item.ModArmorMaterials
import ic2_120.content.item.RubberBoots
import ic2_120.content.item.energy.LapotronCrystalItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
<<<<<<< HEAD
=======
import ic2_120.editCustomData
import ic2_120.getCustomData
>>>>>>> backup/1.21.1
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.type
<<<<<<< HEAD
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.data.server.recipe.RecipeJsonProvider
=======
import net.minecraft.data.server.recipe.RecipeExporter
>>>>>>> backup/1.21.1
import net.minecraft.item.ArmorItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
<<<<<<< HEAD
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.function.Consumer
=======
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
>>>>>>> backup/1.21.1

/**
 * 量子靴子 (Quantum Boots)
 *
 * 量子套装的靴子部件，提供大跳功能。
 *
 * ## 核心参数
 *
 * - 载电量：10,000,000 EU（10 MEU）
 * - 能量等级：4
 * - 减伤比例：10%
 *
 * ## 大跳功能
 *
 * - Alt + J 开关
 * - 跳起高度 ~3.75 格（3 倍正常跳高）
 * - 耗电：10,000 EU/次（满电约 1000 跳）
<<<<<<< HEAD
 * - 大跳落地自动免疫摔落伤害；可通过配置切换为原版 IC2 的常驻保护
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 20000 EU × 配置倍率（默认倍率 1.0）
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_boots", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumBoots : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.BOOTS, FabricItemSettings().maxCount(1)) {

    companion object {
        private const val SUPER_JUMP_KEY = "SuperJumpEnabled"
        private const val PERMANENT_FALL_PROTECTION_FREE_DISTANCE = 10
        private const val PERMANENT_FALL_PROTECTION_EU_PER_DAMAGE = 20_000L
=======
 * - 落地自动免疫摔落伤害
 *
 * ## 能量消耗
 *
 * - 每减免 1 点伤害消耗 5000 EU
 * - 能量从所有量子装备均匀扣除
 */
@ModItem(name = "quantum_boots", tab = CreativeTab.IC2_MATERIALS, group = "quantum_armor")
class QuantumBoots : QuantumArmorItem(ModArmorMaterials.QUANTUM_ARMOR, ArmorItem.Type.BOOTS, Item.Settings().maxCount(1)) {

    companion object {
        private const val SUPER_JUMP_KEY = "SuperJumpEnabled"
        const val SUPER_JUMP_PROTECTION_KEY = "SuperJumpProtection"
>>>>>>> backup/1.21.1

        val jumpEnergyCost: Long
            get() = Ic2Config.getQuantumBootsJumpEnergyCost()

        @JvmStatic
        fun isSuperJumpEnabled(stack: ItemStack): Boolean =
<<<<<<< HEAD
            stack.orCreateNbt.getBoolean(SUPER_JUMP_KEY)

        @JvmStatic
        fun toggleSuperJump(stack: ItemStack): Boolean {
            val nbt = stack.orCreateNbt
            val enabled = !nbt.getBoolean(SUPER_JUMP_KEY)
            nbt.putBoolean(SUPER_JUMP_KEY, enabled)
            return enabled
        }

=======
            stack.getCustomData()?.getBoolean(SUPER_JUMP_KEY) ?: false

        @JvmStatic
        fun toggleSuperJump(stack: ItemStack): Boolean {
            val current = stack.getCustomData()?.getBoolean(SUPER_JUMP_KEY) ?: false
            val enabled = !current
            stack.editCustomData { it.putBoolean(SUPER_JUMP_KEY, enabled) }
            return enabled
        }

        /** 标记大跳摔落保护（给 Java Mixin 用） */
        @JvmStatic
        fun markSuperJumpProtection(stack: ItemStack) {
            stack.editCustomData { it.putBoolean(SUPER_JUMP_PROTECTION_KEY, true) }
        }

        /** 检测并清除大跳摔落保护（给 Java Mixin 用） */
        @JvmStatic
        fun checkAndClearSuperJumpProtection(stack: ItemStack): Boolean {
            val data = stack.getCustomData() ?: return false
            val protected = data.getBoolean(SUPER_JUMP_PROTECTION_KEY)
            if (protected) {
                stack.editCustomData { it.remove(SUPER_JUMP_PROTECTION_KEY) }
            }
            return protected
        }

>>>>>>> backup/1.21.1
        @JvmStatic
        fun getJumpHeightMultiplier(): Double =
            Ic2Config.getQuantumBootsJumpHeightMultiplier()

<<<<<<< HEAD
        @JvmStatic
        fun isPermanentFallProtectionEnabled(): Boolean =
            Ic2Config.current.armor.quantumBoots.permanentFallProtection

        /**
         * 按原版 IC2 规则尝试吸收任意摔落：前 10 格免费，超过部分每点消耗 20,000 EU。
         */
        @JvmStatic
        fun tryAbsorbPermanentFallDamage(stack: ItemStack, fallDistance: Float): Boolean {
            if (!isPermanentFallProtectionEnabled()) return false
            val boots = stack.item as? QuantumBoots ?: return false
            val fallDamage = (fallDistance.toInt() - PERMANENT_FALL_PROTECTION_FREE_DISTANCE)
                .coerceAtLeast(0)
            val energyCost = fallDamage.toLong() * PERMANENT_FALL_PROTECTION_EU_PER_DAMAGE
            val energy = boots.getEnergy(stack)
            if (energyCost > energy) return false
            if (energyCost > 0L) {
                boots.setEnergy(stack, energy - energyCost)
            }
            return true
        }

=======
>>>>>>> backup/1.21.1
        /**
         * 消耗大跳能量。由混合在跳跃时调用。
         * @return true 如果消耗成功
         */
        @JvmStatic
        fun consumeJumpEnergy(stack: ItemStack): Boolean {
            val item = stack.item as? QuantumBoots ?: return false
            val energy = item.getEnergy(stack)
            val cost = jumpEnergyCost
            if (energy < cost) return false
            item.setEnergy(stack, energy - cost)
            return true
        }

        @RecipeProvider
<<<<<<< HEAD
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
=======
        fun generateRecipes(exporter: RecipeExporter) {
>>>>>>> backup/1.21.1
            val iridium = IridiumPlate::class.instance()
            val nano = NanoBoots::class.instance()
            val rubber = RubberBoots::class.instance()
            val lapotron = LapotronCrystalItem::class.instance()
            if (iridium == Items.AIR || nano == Items.AIR || rubber == Items.AIR || lapotron == Items.AIR) return
            BatteryEnergyShapedRecipeDatagen.offer(
                exporter = exporter,
                recipeId = QuantumBoots::class.id(),
                result = QuantumBoots::class.instance(),
                pattern = listOf("   ", "INI", "RLR"),
                keys = mapOf<Char, Item>(
                    'I' to iridium,
                    'N' to nano,
                    'R' to rubber,
                    'L' to lapotron
                ),
                category = "equipment"
            )
        }
    }

<<<<<<< HEAD
    override fun appendTooltip(stack: ItemStack, world: net.minecraft.world.World?, tooltip: MutableList<Text>, context: net.minecraft.client.item.TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val jumpEnabled = stack.orCreateNbt.getBoolean(SUPER_JUMP_KEY)
=======
    override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        val jumpEnabled = stack.getCustomData()?.getBoolean(SUPER_JUMP_KEY) ?: false
>>>>>>> backup/1.21.1
        val energy = getEnergy(stack)
        val jumpCount = if (jumpEnergyCost > 0) energy / jumpEnergyCost else 0L

                tooltip.add(Text.translatable("tooltip.ic2_120.quantum_boots.jump",
            Text.translatable(if (jumpEnabled) "tooltip.ic2_120.status.on" else "tooltip.ic2_120.status.off"),
            jumpCount
        ).formatted(Formatting.GRAY))
        val pct = "%.0f".format(getDamageReduction() * 100)
        tooltip.add(Text.translatable("tooltip.ic2_120.quantum_boots.damage_reduction", pct).formatted(Formatting.GRAY))
    }
}
