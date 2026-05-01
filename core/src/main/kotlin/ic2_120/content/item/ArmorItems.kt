package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.content.block.BatBoxBlock
import ic2_120.content.block.LuminatorFlatBlock
import ic2_120.content.block.ReinforcedGlassBlock
import ic2_120.content.block.SolarGeneratorBlock
import ic2_120.content.block.cables.InsulatedCopperCableBlock
import ic2_120.content.effect.ModStatusEffects
import ic2_120.content.item.AdvancedHeatExchangerItem
import ic2_120.content.item.energy.AdvancedReBatteryItem
import ic2_120.content.item.energy.BatteryItemBase
import ic2_120.content.item.energy.EnergyCrystalItem
import ic2_120.content.item.energy.LapotronCrystalItem
import ic2_120.content.item.energy.ReBatteryItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.content.item.energy.chargePlayerInventory
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.ArmorMaterial
import net.minecraft.item.Item
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Supplier
import ic2_120.content.item.armor.ElectricArmorItem
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen
import ic2_120.config.Ic2Config
import ic2_120.editCustomData
import ic2_120.getCustomData


//todo 全套防化服，防化三件套加橡胶靴 可免疫特斯拉线圈伤害 可免疫触电伤害，只要有单个防化头盔，物品栏有压缩空气单元，就可以在气泡值用尽时消耗压缩空气单元回满
//todo 

// ========== 护甲材料 ==========

/**
 * 创建自定义护甲材料（1.21.1 record 构造器）
 */
private fun createArmorMaterial(
    name: String,
    protection: Map<ArmorItem.Type, Int>,
    enchantability: Int,
    equipSound: RegistryEntry<SoundEvent>,
    toughness: Float,
    knockbackResistance: Float,
    repairIngredient: Ingredient
): RegistryEntry<ArmorMaterial> = RegistryEntry.of(
    ArmorMaterial(
        protection,
        enchantability,
        equipSound,
        Supplier { repairIngredient },
        listOf(ArmorMaterial.Layer(Identifier.ofVanilla(name))),
        toughness,
        knockbackResistance
    )
)

// ========== 修复原料 ==========
// 各护甲材料对应的修复物品（通过模组 ID 加载）
private val bronzeIngot = Ingredient.ofItems(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "bronze_ingot")))
private val rubber = Ingredient.ofItems(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "rubber")))
private val carbonFibre = Ingredient.ofItems(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "carbon_fibre")))
private val advancedAlloy = Ingredient.ofItems(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "alloy")))
private val iridium = Ingredient.ofItems(Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "iridium")))

// ========== 护甲材料定义 ==========

// ========== 太阳能护甲 (Solar Armor) ==========
/**
 * 太阳能护甲材料，用于太阳能头盔。
 * 可在日光下自动充电的太阳能装备。
 */
private val SOLAR_ARMOR = createArmorMaterial(
    name = "ic2_solar",  // 使用 "solar" 作为名称，这样会查找 solar_1.png 和 solar_2.png
    protection = mapOf(
        ArmorItem.Type.HELMET to 2,      // 2 点护甲（与青铜相同）
        ArmorItem.Type.CHESTPLATE to 0,
        ArmorItem.Type.LEGGINGS to 0,
        ArmorItem.Type.BOOTS to 0
    ),
    enchantability = 10,
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_IRON,
    toughness = 0f,
    knockbackResistance = 0f,
    repairIngredient = bronzeIngot  // 用青铜修复
)

// ========== 青铜护甲 (Bronze Armor) ==========
/**
 * IC2 模组的基础护甲套装，提供全面的四件套防护。
 * 属性接近原版铁质护甲，是早期游戏的主要防护装备。
 */
private val BRONZE_ARMOR = createArmorMaterial(
    name = "ic2_bronze",
    protection = mapOf(
        ArmorItem.Type.HELMET to 2,      // 2 点护甲（2 半格 / 1 图标）
        ArmorItem.Type.CHESTPLATE to 6,  // 6 点护甲（6 半格 / 3 图标）
        ArmorItem.Type.LEGGINGS to 5,    // 5 点护甲（5 半格 / 2.5 图标）
        ArmorItem.Type.BOOTS to 2        // 2 点护甲（2 半格 / 1 图标）
    ),
    enchantability = 10,       // 与铁质相同，中等附魔能力
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_IRON,  // 铁质音效
    toughness = 0f,            // 无韧性（钻石为 2.0）
    knockbackResistance = 0f,  // 无击退抗性
    repairIngredient = bronzeIngot
)

// 青铜护甲耐久倍率（与铁护甲相同）
private const val BRONZE_DURABILITY_MULTIPLIER = 15

// ========== 橡胶靴 (Rubber Boots) ==========
/**
 * IC2 橡胶靴专用护甲材料，提供绝缘特性。
 * 参考原版 IC2 耐久：64 (靴子基础乘数 13，需要约 4.92 倍率)
 * 护甲值：1 (与皮革相同)
 */
private val RUBBER_ARMOR = createArmorMaterial(
    name = "ic2_rubber",
    protection = mapOf(
        ArmorItem.Type.HELMET to 0,  // 无防护
        ArmorItem.Type.CHESTPLATE to 0,
        ArmorItem.Type.LEGGINGS to 0,
        ArmorItem.Type.BOOTS to 1     // 1 点护甲（同皮革）
    ),
    enchantability = 15,       // 与皮革相同，高附魔能力
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,  // 皮革音效
    toughness = 0f,
    knockbackResistance = 0f,
    repairIngredient = rubber
)
// 橡胶靴耐久倍率（约 5，使靴子耐久为 64）
private const val RUBBER_DURABILITY_MULTIPLIER = 5

// ========== 防化服 (Hazmat Armor) ==========
/**
 * IC2 防化服套装专用护甲材料。
 * 参考原版 IC2 各部位耐久：均为 64
 * - 头盔：11*5.8≈64，胸甲：16*4=64，护腿：15*4.3≈64，靴子：13*4.9≈64
 * 采用统一 multiplier=5 以获得最接近的均衡值
 */
private val HAZMAT_ARMOR = createArmorMaterial(
    name = "ic2_hazmat",
    protection = mapOf(
        ArmorItem.Type.HELMET to 1,     // 1 点护甲（1 半格 / 0.5 图标）
        ArmorItem.Type.CHESTPLATE to 3, // 3 点护甲（3 半格 / 1.5 图标）
        ArmorItem.Type.LEGGINGS to 2,   // 2 点护甲（2 半格 / 1 图标）
        ArmorItem.Type.BOOTS to 0       // 无防护
    ),
    enchantability = 15,       // 与皮革相同
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
    toughness = 0f,
    knockbackResistance = 0f,
    repairIngredient = rubber  // 用橡胶修复
)
// 防化服耐久倍率（使各部位耐久约 64）
private const val HAZMAT_DURABILITY_MULTIPLIER = 5

// ========== 纳米护甲 (Nano Armor) ==========
/**
 * IC2 高级护甲套装，能量驱动的自修复护甲。
 * 参考 MC百科 https://www.mcmod.cn/item/208.html
 *
 * 机制（待实现）：
 * - 载电量：1 MEU (1,000,000 EU)，需 MFE 或更高级储电箱充电
 * - 每抵挡 1 点伤害消耗 5,000 EU
 * - 纳米头盔：Alt+M 开启夜视，持续 15 秒，穿戴时持续
 * - 纳米靴子：摔落减免（≤4 格不耗电不掉血；≤12 格耗电不掉血；>12 格减免 12 格后按剩余高度受伤）
 * - 无电时持续受伤过久装备会消失
 */
private val NANO_ARMOR = createArmorMaterial(
    name = "ic2_nano",
    protection = mapOf(
        ArmorItem.Type.HELMET to 3,      // 3 点护甲（钻石级）
        ArmorItem.Type.CHESTPLATE to 8,   // 8 点护甲（钻石级）
        ArmorItem.Type.LEGGINGS to 6,     // 6 点护甲（钻石级）
        ArmorItem.Type.BOOTS to 3        // 3 点护甲（钻石级）
    ),
    enchantability = 10,       // 与铁质相同
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,  // 钻石音效
    toughness = 2f,            // 钻石级韧性
    knockbackResistance = 0f,  // 无击退抗性
    repairIngredient = carbonFibre
)

// ========== 量子护甲 (Quantum Armor) ==========
/**
 * IC2 顶级护甲套装，具备全方位强化。
 * 参考 MC百科 https://www.mcmod.cn/item/212.html
 *
 * 机制（待实现）：
 * - 载电量：10 MEU (10,000,000 EU)，需 MFSU 或 MFSU 充电座充电
 * - 各部件伤害减免：头盔 15%、胸甲 44%、护腿 30%、靴子 15%（穿齐可完全抵挡岩浆等）
 * - 每次受击：每件装备至少 900 EU + 每点伤害 30 EU
 * - 量子头盔：水下呼吸、夜视(Alt+M)、自动补饥饿(需满锡罐)；全套时：消除 debuff(中毒/凋零/辐射)
 * - 量子胸甲：飞行、防火、熄灭火焰
 * - 量子护腿：3 倍奔跑速度，冰上 9 倍（双击 W 激活）
 * - 量子靴子：减免坠落伤害、超级跳(Ctrl+空格，最高 9 格，1000 EU/次)
 * - 无电时持续受伤过久装备会消失
 */
private val QUANTUM_ARMOR = createArmorMaterial(
    name = "ic2_quantum",
    protection = mapOf(
        ArmorItem.Type.HELMET to 4,       // 等效 15% 减免
        ArmorItem.Type.CHESTPLATE to 9,   // 等效 44% 减免，单件 9 格护甲（钻石胸甲 4 格）
        ArmorItem.Type.LEGGINGS to 6,      // 等效 30% 减免
        ArmorItem.Type.BOOTS to 4         // 等效 15% 减免
    ),
    enchantability = 15,       // 高附魔能力
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,  // 下界合金音效
    toughness = 3f,            // 超越钻石的韧性
    knockbackResistance = 0.4f, // 40% 击退抗性（量子服最高 0.4）
    repairIngredient = iridium
)

// ========== 复合胸甲 (Alloy Chestplate) ==========
/**
 * 仅包含胸甲部件的"套装"，专为特定战术用途设计。
 * 胸甲：9 点护甲、耐久与原版钻石胸甲相同（528）。
 */
private val ALLOY_ARMOR = createArmorMaterial(
    name = "ic2_alloy",
    // 胸甲耐久 = 16 × multiplier；原版钻石胸甲为 528 ⇒ multiplier = 33
    protection = mapOf(
        ArmorItem.Type.HELMET to 0,     // 无头盔
        ArmorItem.Type.CHESTPLATE to 9, // 9 点护甲
        ArmorItem.Type.LEGGINGS to 0,   // 无护腿
        ArmorItem.Type.BOOTS to 0       // 无靴子
    ),
    enchantability = 16,
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_IRON,
    toughness = 0f,
    knockbackResistance = 0f,
    repairIngredient = advancedAlloy
)
// 复合胸甲耐久倍率（与钻石胸甲相同）
private const val ALLOY_DURABILITY_MULTIPLIER = 33

// ========== 背包护甲材料 ==========
/**
 * 背包类护甲材料，用于各种电池背包和喷气背包。
 * 提供基础的胸甲防护，主要功能在于储能或飞行。
 */
private val BACKPACK_ARMOR = createArmorMaterial(
    name = "batpack",  // 使用 batpack 作为基础，会查找 batpack_1.png
    protection = mapOf(
        ArmorItem.Type.HELMET to 0,
        ArmorItem.Type.CHESTPLATE to 3, // 3 点护甲（同铁胸甲）
        ArmorItem.Type.LEGGINGS to 0,
        ArmorItem.Type.BOOTS to 0
    ),
    enchantability = 10,
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
    toughness = 0f,
    knockbackResistance = 0f,
    repairIngredient = bronzeIngot
)

// 高级电池背包护甲材料
private val ADVANCED_BATPACK_ARMOR = createArmorMaterial(
    name = "advbatpack",  // 查找 advbatpack_1.png
    protection = mapOf(
        ArmorItem.Type.HELMET to 0,
        ArmorItem.Type.CHESTPLATE to 3,
        ArmorItem.Type.LEGGINGS to 0,
        ArmorItem.Type.BOOTS to 0
    ),
    enchantability = 10,
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
    toughness = 0f,
    knockbackResistance = 0f,
    repairIngredient = advancedAlloy
)

// 能量背包护甲材料
private val ENERGY_PACK_ARMOR = createArmorMaterial(
    name = "energypack",  // 查找 energypack_1.png
    protection = mapOf(
        ArmorItem.Type.HELMET to 0,
        ArmorItem.Type.CHESTPLATE to 4,
        ArmorItem.Type.LEGGINGS to 0,
        ArmorItem.Type.BOOTS to 0
    ),
    enchantability = 10,
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
    toughness = 0f,
    knockbackResistance = 0f,
    repairIngredient = carbonFibre
)

// 兰波顿背包护甲材料
private val LAPPACK_ARMOR = createArmorMaterial(
    name = "lappack",  // 查找 lappack_1.png
    protection = mapOf(
        ArmorItem.Type.HELMET to 0,
        ArmorItem.Type.CHESTPLATE to 5,
        ArmorItem.Type.LEGGINGS to 0,
        ArmorItem.Type.BOOTS to 0
    ),
    enchantability = 10,
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
    toughness = 0f,
    knockbackResistance = 0f,
    repairIngredient = iridium
)

// 喷气背包护甲材料
// 已移至 ModArmorMaterials.JETPACK_ARMOR

// 建筑泡沫背包护甲材料
private val CF_PACK_ARMOR = createArmorMaterial(
    name = "cf_pack",  // 需要创建 cf_pack_1.png
    protection = mapOf(
        ArmorItem.Type.HELMET to 0,
        ArmorItem.Type.CHESTPLATE to 2,
        ArmorItem.Type.LEGGINGS to 0,
        ArmorItem.Type.BOOTS to 0
    ),
    enchantability = 10,
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
    toughness = 0f,
    knockbackResistance = 0f,
    repairIngredient = carbonFibre
)

private val NIGHT_VISION_ARMOR = createArmorMaterial(
    name = "ic2_night_vision",
    protection = mapOf(
        ArmorItem.Type.HELMET to 1,
        ArmorItem.Type.CHESTPLATE to 0,
        ArmorItem.Type.LEGGINGS to 0,
        ArmorItem.Type.BOOTS to 0
    ),
    enchantability = 10,
    equipSound = SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
    toughness = 0f,
    knockbackResistance = 0f,
    repairIngredient = carbonFibre
)

// ========== 护甲类实现 ==========

// ========== 青铜护甲套装 (Bronze Armor Set) ==========
/**
 * IC2 基础护甲套装，完整的四件套防护。
 * 适用于游戏早期阶段，属性接近铁质护甲但可通过 IC2 方式获取。
 */
@ModItem(name = "bronze_helmet", tab = CreativeTab.IC2_MATERIALS, group = "bronze_armor")
class BronzeHelmet : ArmorItem(BRONZE_ARMOR, ArmorItem.Type.HELMET, Item.Settings().maxCount(1).maxDamage(ArmorItem.Type.HELMET.getMaxDamage(BRONZE_DURABILITY_MULTIPLIER))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val bronze = BronzeIngot::class.instance()
            if (bronze != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, BronzeHelmet::class.instance(), 1)
                    .pattern("BBB")
                    .pattern("B B")
                    .input('B', bronze)
                    .criterion(hasItem(bronze), conditionsFromItem(bronze))
                    .offerTo(exporter, BronzeHelmet::class.id())
            }
        }
    }
}

@ModItem(name = "bronze_chestplate", tab = CreativeTab.IC2_MATERIALS, group = "bronze_armor")
class BronzeChestplate : ArmorItem(BRONZE_ARMOR, ArmorItem.Type.CHESTPLATE, Item.Settings().maxCount(1).maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(BRONZE_DURABILITY_MULTIPLIER))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val bronze = BronzeIngot::class.instance()
            if (bronze != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, BronzeChestplate::class.instance(), 1)
                    .pattern("B B")
                    .pattern("BBB")
                    .pattern("BBB")
                    .input('B', bronze)
                    .criterion(hasItem(bronze), conditionsFromItem(bronze))
                    .offerTo(exporter, BronzeChestplate::class.id())
            }
        }
    }
}

@ModItem(name = "bronze_leggings", tab = CreativeTab.IC2_MATERIALS, group = "bronze_armor")
class BronzeLeggings : ArmorItem(BRONZE_ARMOR, ArmorItem.Type.LEGGINGS, Item.Settings().maxCount(1).maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(BRONZE_DURABILITY_MULTIPLIER))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val bronze = BronzeIngot::class.instance()
            if (bronze != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, BronzeLeggings::class.instance(), 1)
                    .pattern("BBB")
                    .pattern("B B")
                    .pattern("B B")
                    .input('B', bronze)
                    .criterion(hasItem(bronze), conditionsFromItem(bronze))
                    .offerTo(exporter, BronzeLeggings::class.id())
            }
        }
    }
}

@ModItem(name = "bronze_boots", tab = CreativeTab.IC2_MATERIALS, group = "bronze_armor")
class BronzeBoots : ArmorItem(BRONZE_ARMOR, ArmorItem.Type.BOOTS, Item.Settings().maxCount(1).maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(BRONZE_DURABILITY_MULTIPLIER))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val bronze = BronzeIngot::class.instance()
            if (bronze != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, BronzeBoots::class.instance(), 1)
                    .pattern("B B")
                    .pattern("B B")
                    .input('B', bronze)
                    .criterion(hasItem(bronze), conditionsFromItem(bronze))
                    .offerTo(exporter, BronzeBoots::class.id())
            }
        }
    }
}

// ========== 橡胶靴 (Rubber Boots) ==========
/**
 * IC2 橡胶靴，提供基础防触电保护并带有缓降落功能。
 * 参考原版 IC2 耐久：64
 * 行走时可为背包中的可充电物品充电，数值由配置控制。
 */
@ModItem(name = "rubber_boots", tab = CreativeTab.IC2_MATERIALS, group = "hazmat_armor")
class RubberBoots : ArmorItem(RUBBER_ARMOR, ArmorItem.Type.BOOTS, Item.Settings().maxCount(1).maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(RUBBER_DURABILITY_MULTIPLIER))) {
    companion object {
        private const val WALK_ACC_KEY = "Ic2RubberBootsWalkAcc"
        private const val LAST_X_KEY = "Ic2RubberBootsLastX"
        private const val LAST_Z_KEY = "Ic2RubberBootsLastZ"

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val rubber = RubberItem::class.instance()
            if (rubber == Items.AIR) return
            ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, RubberBoots::class.instance(), 1)
                .pattern("R R")
                .pattern("R R")
                .pattern("RWR")
                .input('R', rubber)
                .input('W', Items.WHITE_WOOL)
                .criterion(hasItem(rubber), conditionsFromItem(rubber))
                .offerTo(exporter, RubberBoots::class.id())
        }
    }

    override fun inventoryTick(stack: ItemStack, world: World, entity: net.minecraft.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return

        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.FEET) !== stack) return

        val nbt = stack.getCustomData()
        val hasLastPos = nbt?.contains(LAST_X_KEY) == true && nbt.contains(LAST_Z_KEY)
        if (!hasLastPos) {
            stack.editCustomData {
                it.putDouble(LAST_X_KEY, player.x)
                it.putDouble(LAST_Z_KEY, player.z)
            }
            return
        }

        val lastX = nbt.getDouble(LAST_X_KEY)
        val lastZ = nbt.getDouble(LAST_Z_KEY)
        val dx = player.x - lastX
        val dz = player.z - lastZ
        stack.editCustomData {
            it.putDouble(LAST_X_KEY, player.x)
            it.putDouble(LAST_Z_KEY, player.z)
        }

        if (!player.isOnGround) return
        val segment = kotlin.math.sqrt(dx * dx + dz * dz)
        if (segment <= 0.0 || segment > 2.0) return

        val distance = Ic2Config.getRubberBootsDistance()
        val eu = Ic2Config.getRubberBootsEu()

        var acc = nbt.getDouble(WALK_ACC_KEY) + segment
        val charges = kotlin.math.floor(acc / distance).toLong()
        if (charges <= 0L) {
            stack.editCustomData { it.putDouble(WALK_ACC_KEY, acc) }
            return
        }

        val toCharge = charges * eu
        var remaining = toCharge
        remaining -= chargePlayerInventory(player, remaining)
        val actualCharged = (toCharge - remaining) / eu
        acc -= actualCharged * distance
        stack.editCustomData { it.putDouble(WALK_ACC_KEY, acc.coerceAtLeast(0.0)) }
    }

    override fun appendTooltip(stack: ItemStack, context: Item.TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        tooltip.add(Text.literal("§7行走时为背包内可充电物品充电"))
        super.appendTooltip(stack, context, tooltip, type)
    }
}

// ========== 防化服 (Hazmat Suit) ==========
/**
 * IC2 危害物质防护服套装，用于辐射、水下环境和危险物质防护。
 * 完整套装效果：
 * - 免疫放射性物品的辐射减益
 * - 获得防火 II 增益
 * - 阻止裸露导线造成的电击伤害
 * - 配合空气单元实现水下呼吸（头盔消耗空气单元）
 *
 * 参考原版 IC2 各部位耐久：均为 64
 */
@ModItem(name = "hazmat_helmet", tab = CreativeTab.IC2_MATERIALS, group = "hazmat_armor")
class HazmatHelmet : ArmorItem(HAZMAT_ARMOR, ArmorItem.Type.HELMET, Item.Settings().maxCount(1).maxDamage(ArmorItem.Type.HELMET.getMaxDamage(HAZMAT_DURABILITY_MULTIPLIER))) {
    companion object {
        private const val CHECK_COOLDOWN_KEY = "AirCheckCooldown"
        private const val AIR_THRESHOLD = 60  // 当气泡值 <= 60 时触发（约 3 秒，20%）

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val rubber = RubberItem::class.instance()
            if (rubber == Items.AIR) return
            ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, HazmatHelmet::class.instance(), 1)
                .pattern(" D ")
                .pattern("RGR")
                .pattern("RIR")
                .input('D', Items.ORANGE_DYE)
                .input('R', rubber)
                .input('G', Items.GLASS)
                .input('I', Items.IRON_BARS)
                .criterion(hasItem(rubber), conditionsFromItem(rubber))
                .offerTo(exporter, HazmatHelmet::class.id())
        }
    }

    override fun inventoryTick(stack: ItemStack, world: World, entity: net.minecraft.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return

        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.HEAD) !== stack) return

        // 只在玩家接触水时检测
        if (!player.isTouchingWater) return

        // 检查冷却，防止同一tick多次消耗
        val nbt = stack.getCustomData()
        val cooldown = nbt?.getInt(CHECK_COOLDOWN_KEY) ?: 0
        if (cooldown > 0) {
            stack.editCustomData { it.putInt(CHECK_COOLDOWN_KEY, cooldown - 1) }
            return
        }

        // 当气泡值低于阈值时（<= 60，约 3 秒）
        if (player.air <= AIR_THRESHOLD) {
            val airCellItem = Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "air_cell"))
            val emptyCellItem = Registries.ITEM.get(Identifier.of(Ic2_120.MOD_ID, "empty_cell"))

            // 查找背包中的压缩空气单元
            val airCellSlot = player.inventory.main.find { slot ->
                !slot.isEmpty && slot.item === airCellItem
            }

            if (airCellSlot != null) {
                // 消耗一个压缩空气单元
                airCellSlot.decrement(1)

                // 创建空单元
                val emptyCell = ItemStack(emptyCellItem)

                // 尝试放入背包，如果满了就掉在地上
                if (!player.inventory.insertStack(emptyCell)) {
                    player.dropItem(emptyCell, false)
                }

                // 回满气泡值
                player.air = player.maxAir

                // 设置冷却时间，防止同一tick多次触发（20 ticks = 1秒）
                stack.editCustomData { it.putInt(CHECK_COOLDOWN_KEY, 20) }
            }
        }
    }
}

@ModItem(name = "hazmat_chestplate", tab = CreativeTab.IC2_MATERIALS, group = "hazmat_armor")
class HazmatChestplate : ArmorItem(HAZMAT_ARMOR, ArmorItem.Type.CHESTPLATE, Item.Settings().maxCount(1).maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(HAZMAT_DURABILITY_MULTIPLIER))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val rubber = RubberItem::class.instance()
            if (rubber == Items.AIR) return
            ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, HazmatChestplate::class.instance(), 1)
                .pattern("R R")
                .pattern("RDR")
                .pattern("RDR")
                .input('R', rubber)
                .input('D', Items.ORANGE_DYE)
                .criterion(hasItem(rubber), conditionsFromItem(rubber))
                .offerTo(exporter, HazmatChestplate::class.id())
        }
    }
}

@ModItem(name = "hazmat_leggings", tab = CreativeTab.IC2_MATERIALS, group = "hazmat_armor")
class HazmatLeggings : ArmorItem(HAZMAT_ARMOR, ArmorItem.Type.LEGGINGS, Item.Settings().maxCount(1).maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(HAZMAT_DURABILITY_MULTIPLIER))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val rubber = RubberItem::class.instance()
            if (rubber == Items.AIR) return
            ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, HazmatLeggings::class.instance(), 1)
                .pattern("RDR")
                .pattern("R R")
                .pattern("R R")
                .input('R', rubber)
                .input('D', Items.ORANGE_DYE)
                .criterion(hasItem(rubber), conditionsFromItem(rubber))
                .offerTo(exporter, HazmatLeggings::class.id())
        }
    }
}

// ========== 纳米护甲套装 (Nano Armor Set) ==========
// ========== 太阳能头盔 ==========
/**
 * 太阳能头盔 (Solar Helmet)
 * 可在日光下自动为玩家装备中的电池物品充电的头盔。
 */
@ModItem(name = "solar_helmet", tab = CreativeTab.IC2_MATERIALS, group = "solar_armor")
class SolarHelmet : ArmorItem(SOLAR_ARMOR, ArmorItem.Type.HELMET, Item.Settings().maxCount(1).maxDamage(0)) {
    companion object {
        private const val EU_PER_TICK = 1L

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val solar = SolarGeneratorBlock::class.item()
            val cable = InsulatedCopperCableBlock::class.item()
            if (solar == Items.AIR || cable == Items.AIR) return
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SolarHelmet::class.instance(), 1)
                .pattern("III")
                .pattern("ISI")
                .pattern("CCC")
                .input('I', Items.IRON_INGOT)
                .input('S', solar)
                .input('C', cable)
                .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                .offerTo(exporter, SolarHelmet::class.id())
        }
    }

    override fun inventoryTick(stack: ItemStack, world: World, entity: net.minecraft.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return

        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.HEAD) !== stack) {
            player.removeStatusEffect(ModStatusEffects.SOLAR_GENERATING)
            return
        }

        val generating = canGenerate(world, player)
        if (!generating) {
            player.removeStatusEffect(ModStatusEffects.SOLAR_GENERATING)
            return
        }

        chargePlayerInventory(player, EU_PER_TICK)
        player.addStatusEffect(
            StatusEffectInstance(
                ModStatusEffects.SOLAR_GENERATING,
                40,
                0,
                true,
                false,
                true
            )
        )
    }

    private fun canGenerate(world: World, player: PlayerEntity): Boolean {
        if (!world.isDay) return false
        val startY = player.blockY + 1
        val topY = world.topY
        val mutablePos = BlockPos.Mutable(player.blockX, startY, player.blockZ)
        for (y in startY..topY) {
            mutablePos.set(player.blockX, y, player.blockZ)
            val state = world.getBlockState(mutablePos)
            if (!state.isAir && !state.isTransparent(world, mutablePos)) {
                return false
            }
        }
        return true
    }
}

// ========== 复合胸甲 (Alloy Chestplate) ==========
/**
 * 先进合金单件胸甲，专为战术灵活性设计。
 * 9 点护甲、耐久与原版钻石胸甲相同；仅胸甲槽，无其他部位。
 */
@ModItem(name = "alloy_chestplate", tab = CreativeTab.IC2_MATERIALS, group = "armor")
class AlloyChestplate : ArmorItem(ALLOY_ARMOR, ArmorItem.Type.CHESTPLATE, Item.Settings().maxCount(1).maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(ALLOY_DURABILITY_MULTIPLIER))) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val alloy = Alloy::class.instance()
            if (alloy != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AlloyChestplate::class.instance(), 1)
                    .pattern("A A")
                    .pattern("AIA")
                    .pattern("ALA")
                    .input('A', alloy)
                    .input('I', Items.IRON_CHESTPLATE)
                    .input('L', Items.LEATHER_CHESTPLATE)
                    .criterion(hasItem(alloy), conditionsFromItem(alloy))
                    .offerTo(exporter, AlloyChestplate::class.id())
            }
        }
    }
}

// ========== 背包与胸甲类装备 ==========

/**
 * 建筑泡沫背包 (CF Pack)
 * 碳纤维强化背包，用于存储和喷射建筑泡沫的特殊物品。
 * 存储 80 桶建筑泡沫，装备时自动补充喷枪并为喷枪提供泡沫源。
 * 作为胸甲装备。
 */
@ModItem(name = "cf_pack", tab = CreativeTab.IC2_MATERIALS, group = "armor")
class CfPack : ArmorItem(CF_PACK_ARMOR, ArmorItem.Type.CHESTPLATE, Item.Settings().maxCount(1).maxDamage(0)) {

    override fun inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return
        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.CHEST) !== stack) return
        autoRefillSprayer(player, stack)
    }

    private fun autoRefillSprayer(player: PlayerEntity, cfPackStack: ItemStack) {
        var available = getFluidAmount(cfPackStack)
        if (available <= 0L) return

        for (i in 0 until player.inventory.main.size) {
            val s = player.inventory.getStack(i)
            if (s.item !is FoamSprayerItem) continue
            val current = FoamSprayerItem.getFluidAmount(s)
            if (current >= FoamSprayerItem.CAPACITY_DROPLETS) continue
            val need = FoamSprayerItem.CAPACITY_DROPLETS - current
            val transfer = minOf(need, available)
            if (transfer <= 0L) continue
            FoamSprayerItem.setFluidAmount(s, current + transfer)
            available -= transfer
            setFluidAmount(cfPackStack, available)
            if (available <= 0L) break
        }
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        val amt = getFluidAmount(stack)
        val buckets = amt * 1000L / FluidConstants.BUCKET
        val maxBuckets = CAPACITY_DROPLETS * 1000L / FluidConstants.BUCKET
        tooltip.add(
            Text.literal("建筑泡沫: ${buckets / 1000}.${(buckets % 1000) / 100}桶 / ${maxBuckets / 1000}桶")
                .formatted(Formatting.GRAY)
        )
        tooltip.add(
            Text.literal("装备时自动为建筑泡沫喷枪补充泡沫")
                .formatted(Formatting.DARK_AQUA)
        )
    }

    override fun isItemBarVisible(stack: ItemStack): Boolean = true

    override fun getItemBarStep(stack: ItemStack): Int {
        val amt = getFluidAmount(stack)
        if (amt <= 0L) return 0
        return (13.0 * amt.toDouble() / CAPACITY_DROPLETS.toDouble()).toInt().coerceIn(1, 13)
    }

    override fun getItemBarColor(stack: ItemStack): Int = 0xFF_6B8E9F.toInt()

    companion object {
        private const val NBT_FLUID_DROPLETS = "CfPackFoamDroplets"

        val CAPACITY_DROPLETS: Long = FluidConstants.BUCKET * 80L

        fun getFluidAmount(stack: ItemStack): Long {
            if (stack.isEmpty || stack.item !is CfPack) return 0L
            return stack.getCustomData()?.getLong(NBT_FLUID_DROPLETS)?.coerceIn(0L, CAPACITY_DROPLETS) ?: 0L
        }

        fun setFluidAmount(stack: ItemStack, amount: Long) {
            if (stack.isEmpty || stack.item !is CfPack) return
            val v = amount.coerceIn(0L, CAPACITY_DROPLETS)
            if (v <= 0L) {
                stack.editCustomData { it.remove(NBT_FLUID_DROPLETS) }
            } else {
                stack.editCustomData { it.putLong(NBT_FLUID_DROPLETS, v) }
            }
        }

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val sprayer = FoamSprayerItem::class.instance()
            val circuit = Circuit::class.instance()
            val emptyCell = EmptyCell::class.instance()
            val casing = IronCasing::class.instance()
            if (sprayer != Items.AIR && circuit != Items.AIR && emptyCell != Items.AIR && casing != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, CfPack::class.instance(), 1)
                    .pattern("xox")
                    .pattern("yzy")
                    .pattern("y y")
                    .input('x', sprayer)
                    .input('o', circuit)
                    .input('y', emptyCell)
                    .input('z', casing)
                    .criterion(hasItem(sprayer), conditionsFromItem(sprayer))
                    .offerTo(exporter, CfPack::class.id())
            }
        }
    }
}

/**
 * 喷气背包 (Jetpack)
 * 允许玩家在空中飞行的喷气推进装置。
 * 需要生物燃料才能运行。
 * 作为胸甲装备。
 */
@ModItem(name = "jetpack", tab = CreativeTab.IC2_MATERIALS, group = "armor")
class Jetpack : JetpackItem() {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val casing = IronCasing::class.instance()
            val circuit = Circuit::class.instance()
            val emptyCell = EmptyCell::class.instance()
            if (casing != Items.AIR && circuit != Items.AIR && emptyCell != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Jetpack::class.instance(), 1)
                    .pattern("CEC")
                    .pattern("CUC")
                    .pattern("R R")
                    .input('C', casing)
                    .input('E', circuit)
                    .input('U', emptyCell)
                    .input('R', Items.REDSTONE)
                    .criterion(hasItem(casing), conditionsFromItem(casing))
                    .offerTo(exporter, Jetpack::class.id())
            }
        }
    }
}

/**
 * 电力喷气背包 (Electric Jetpack)
 * 电力驱动的喷气背包，使用 EU 作为能源而非燃料。
 * 作为胸甲装备。
 */
@ModItem(name = "electric_jetpack", tab = CreativeTab.IC2_MATERIALS, group = "armor")
class ElectricJetpack : ElectricArmorItem(
    ModArmorMaterials.ELECTRIC_JETPACK_ARMOR,
    ArmorItem.Type.CHESTPLATE,
    Item.Settings().maxCount(1).maxDamage(0)
) {
    companion object {
        private const val FLIGHT_ENABLED_KEY = "FlightEnabled"
        private const val FLIGHT_REMAINDER_KEY = "ElectricJetpackFlightRemainder"

        val maxCapacity: Long
            get() = Ic2Config.current.armor.electricJetpack.maxEnergy

        val euPerTick: Long
            get() = Ic2Config.getElectricJetpackEuPerTick()

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val casing = IronCasing::class.instance()
            val advanced = AdvancedCircuit::class.instance()
            val batbox = BatBoxBlock::class.item()
            if (casing != Items.AIR && advanced != Items.AIR && batbox != Items.AIR) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ElectricJetpack::class.instance(), 1)
                    .pattern("CAC")
                    .pattern("CBC")
                    .pattern("G G")
                    .input('C', casing)
                    .input('A', advanced)
                    .input('B', batbox)
                    .input('G', Items.GLOWSTONE_DUST)
                    .criterion(hasItem(casing), conditionsFromItem(casing))
                    .offerTo(exporter, ElectricJetpack::class.id())
            }
        }
    }

    override val tier: Int = 1
    override val maxCapacity: Long
        get() = Ic2Config.current.armor.electricJetpack.maxEnergy

    override fun getDamageReduction(): Float = 0f

    fun isFlightEnabled(stack: ItemStack): Boolean =
        stack.getCustomData()?.getBoolean(FLIGHT_ENABLED_KEY) ?: false

    fun setFlightEnabled(stack: ItemStack, enabled: Boolean) {
        stack.editCustomData { it.putBoolean(FLIGHT_ENABLED_KEY, enabled) }
    }

    fun toggleFlightEnabled(stack: ItemStack): Boolean {
        val enabled = !isFlightEnabled(stack)
        setFlightEnabled(stack, enabled)
        return enabled
    }

    fun consumeFlightEnergyPerTick(stack: ItemStack): Boolean {
        val cost = euPerTick
        val energy = getEnergy(stack)
        if (energy < cost) {
            return false
        }
        setEnergy(stack, energy - cost)
        return true
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)

        val enabled = isFlightEnabled(stack)
        val flightStatusText = if (enabled) "开启" else "关闭"

        // 计算剩余飞行时间（秒），基于平均能量消耗 8 + 1/3 EU/tick
        val energy = getEnergy(stack)
        val remainingSeconds = if (energy > 0) {
            (energy / euPerTick.toDouble()) / 20.0
        } else 0.0

        // 格式化时间
        val timeText = if (remainingSeconds >= 60) {
            val minutes = (remainingSeconds / 60).toInt()
            val seconds = (remainingSeconds % 60).toInt()
            "${minutes}分${seconds}秒"
        } else {
            "${remainingSeconds.toInt()}秒"
        }

        tooltip.add(Text.literal("飞行: $flightStatusText | 剩余飞行: $timeText").formatted(Formatting.GRAY))
        tooltip.add(Text.literal("Alt+M：切换飞行开关").formatted(Formatting.DARK_GRAY))
    }
}

/**
 * 夜视镜 (Night Vision Goggles)
 * 提供夜视效果的头戴式装备。
 */
@ModItem(name = "night_vision_goggles", tab = CreativeTab.IC2_MATERIALS, group = "armor")
class NightVisionGoggles : ArmorItem(NIGHT_VISION_ARMOR, ArmorItem.Type.HELMET, Item.Settings().maxCount(1).maxDamage(0)), IBatteryItem {
    override val tier: Int = 2
    override val maxCapacity: Long
        get() = Ic2Config.current.armor.nightVision.nightVisionGogglesMaxEnergy
    override val canChargeWireless: Boolean = false

    companion object {
        private const val ENERGY_KEY = "Energy"
        private const val ENABLED_KEY = "NightVisionEnabled"
        private const val BLINDNESS_DURATION_TICKS = 80
        private const val NIGHT_VISION_DURATION_TICKS = 220

        val maxCapacity: Long
            get() = Ic2Config.current.armor.nightVision.nightVisionGogglesMaxEnergy

        val euPerTick: Long
            get() = Ic2Config.getNightVisionGogglesEuPerTick()

        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val heatEx = AdvancedHeatExchangerItem::class.instance()
            val battery = AdvancedReBatteryItem::class.instance()
            val luminator = LuminatorFlatBlock::class.item()
            val glass = ReinforcedGlassBlock::class.item()
            val rubber = RubberItem::class.instance()
            val circuit = AdvancedCircuit::class.instance()
            if (
                heatEx != Items.AIR && battery != Items.AIR && luminator != Items.AIR &&
                glass != Items.AIR && rubber != Items.AIR && circuit != Items.AIR
            ) {
                ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, NightVisionGoggles::class.instance(), 1)
                    .pattern("HBH")
                    .pattern("LGL")
                    .pattern("RCR")
                    .input('H', heatEx)
                    .input('B', battery)
                    .input('L', luminator)
                    .input('G', glass)
                    .input('R', rubber)
                    .input('C', circuit)
                    .criterion(hasItem(battery), conditionsFromItem(battery))
                    .offerTo(exporter, NightVisionGoggles::class.id())
            }
        }

        fun toggleEnabled(stack: ItemStack): Boolean {
            val enabled = !(stack.getCustomData()?.getBoolean(ENABLED_KEY) ?: false)
            stack.editCustomData { it.putBoolean(ENABLED_KEY, enabled) }
            return enabled
        }
    }

    override fun getCurrentCharge(stack: ItemStack): Long = stack.getCustomData()?.getLong(ENERGY_KEY) ?: 0L

    override fun setCurrentCharge(stack: ItemStack, charge: Long) {
        stack.editCustomData { it.putLong(ENERGY_KEY, charge.coerceIn(0L, maxCapacity)) }
    }

    private fun isEnabled(stack: ItemStack): Boolean = stack.getCustomData()?.getBoolean(ENABLED_KEY) ?: false

    override fun inventoryTick(stack: ItemStack, world: World, entity: net.minecraft.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return
        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.HEAD) !== stack) return
        if (!isEnabled(stack)) return

        val energy = getCurrentCharge(stack)
        if (energy < euPerTick) {
            setCurrentCharge(stack, 0)
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
            return
        }

        setCurrentCharge(stack, energy - euPerTick)
        val brightness = world.getLightLevel(player.blockPos)
        if (brightness >= 8) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
            val blind = player.getStatusEffect(StatusEffects.BLINDNESS)
            if (blind == null || blind.duration <= 0) {
                player.addStatusEffect(
                    StatusEffectInstance(StatusEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, 0, true, false, true)
                )
            }
        } else {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.NIGHT_VISION, NIGHT_VISION_DURATION_TICKS, 0, true, false, true)
            )
        }
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        val energy = getCurrentCharge(stack)
        val ratio = if (maxCapacity > 0) energy.toDouble() / maxCapacity else 0.0
        val status = if (isEnabled(stack)) "ON" else "OFF"
        tooltip.add(Text.literal("⚡ %,d / %,d EU (%.1f%%)".format(energy, maxCapacity, ratio * 100)))
        tooltip.add(Text.literal("等级: $tier | 状态: $status").formatted(Formatting.GRAY))
    }

    override fun isItemBarVisible(stack: ItemStack): Boolean = true
    override fun getItemBarStep(stack: ItemStack): Int = ((getCurrentCharge(stack).toDouble() / maxCapacity) * 13).toInt().coerceIn(0, 13)
    override fun getItemBarColor(stack: ItemStack): Int {
        val ratio = getCurrentCharge(stack).toDouble() / maxCapacity
        return when {
            ratio > 0.5 -> 0x4AFF4A
            ratio > 0.2 -> 0xFFFF4A
            else -> 0xFF4A4A
        }
    }
}

/**
 * 储电胸甲类背包：实现 [IBatteryItem]，与 [BatteryItemBase] 共用 `Energy` NBT，
 * 可在电池充电座等机器中充放电。
 */
abstract class BatteryPackArmorItem(
    armorMaterial: RegistryEntry<ArmorMaterial>,
    override val tier: Int,
    override val maxCapacity: Long,
) : ArmorItem(armorMaterial, ArmorItem.Type.CHESTPLATE, Item.Settings().maxCount(1).maxDamage(0)), IBatteryItem {

    init {
        require(tier in 1..4) { "电池背包等级须在 1–4，当前: $tier" }
        require(maxCapacity > 0) { "电池背包容量须 > 0" }
    }

    /**
     * 与 [WirelessBatteryItemBase] 语义一致：穿戴时可向物品栏中的 [IElectricTool] 输电。
     * 实际逻辑见 [autoFillElectricToolsInInventory]。
     */
    override val canChargeWireless: Boolean = true

    override fun getCurrentCharge(stack: ItemStack): Long =
        BatteryItemBase.getEnergy(stack).coerceIn(0L, maxCapacity)

    override fun setCurrentCharge(stack: ItemStack, charge: Long) {
        BatteryItemBase.setEnergy(stack, charge, maxCapacity)
    }

    /**
     * 每 tick 自动执行：用本背包剩余 EU 将玩家物品栏（含主副手、护甲、主背包）中
     * 所有 [IElectricTool] 一次补满，前提是 `工具.tier <= 本背包.tier`。
     * 不充 [IBatteryItem]（含其他电池/电池背包），避免互充循环。
     */
    override fun inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return
        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.CHEST) !== stack) return
        autoFillElectricToolsInInventory(player, stack)
    }

    private fun autoFillElectricToolsInInventory(player: PlayerEntity, batpackStack: ItemStack) {
        var available = getCurrentCharge(batpackStack)
        if (available <= 0L) return

        // 先收集所有需要处理的物品（避免遍历时修改背包）
        val toCharge = mutableListOf<ItemStack>()

        fun collectItem(stack: ItemStack) {
            if (stack.isEmpty) return
            // 只处理单个物品或电动工具，跳过堆叠的普通物品
            if (stack.count > 1) return
            val item = stack.item as? IElectricTool ?: return
            if (item.tier > tier) return
            if (item.isFullyCharged(stack)) return
            toCharge += stack
        }

        collectItem(player.mainHandStack)
        collectItem(player.offHandStack)
        for (armorStack in player.inventory.armor) {
            if (armorStack === batpackStack) continue
            collectItem(armorStack)
        }
        for (i in 0 until player.inventory.main.size) {
            collectItem(player.inventory.getStack(i))
        }

        // 然后为收集到的物品充电
        for (stack in toCharge) {
            if (available <= 0L) break

            val item = stack.item as? IElectricTool ?: continue
            val need = (item.maxCapacity - item.getEnergy(stack)).coerceAtLeast(0L)
            if (need <= 0L) continue

            val transfer = minOf(need, available)
            item.setEnergy(stack, item.getEnergy(stack) + transfer)
            available -= transfer
            setCurrentCharge(batpackStack, available)
        }
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        val energy = getCurrentCharge(stack)
        val ratio = getChargeRatio(stack)
        tooltip.add(
            Text.literal(
                "⚡ ${BatteryItemBase.formatEnergy(energy, maxCapacity)} (${BatteryItemBase.formatPercentage(ratio)})"
            )
        )
        tooltip.add(
            Text.literal("等级: $tier | 速度: ${nominalEuPerTick()} EU/t")
                .formatted(Formatting.GRAY)
        )
        tooltip.add(
            Text.literal("穿戴时自动为物品栏中电动工具（等级≤背包）补满 EU")
                .formatted(Formatting.DARK_AQUA)
        )
    }

    override fun isItemBarVisible(stack: ItemStack): Boolean = true

    override fun getItemBarStep(stack: ItemStack): Int =
        (getChargeRatio(stack) * 13).toInt().coerceIn(0, 13)

    override fun getItemBarColor(stack: ItemStack): Int {
        val ratio = getChargeRatio(stack)
        return when {
            ratio > 0.5 -> 0x4AFF4A
            ratio > 0.2 -> 0xFFFF4A
            else -> 0xFF4A4A
        }
    }
}

/**
 * 电池背包 (BatPack)
 * 基础级电池背包，可存储中等容量的 EU 并为装备充电。
 * 作为胸甲装备。
 */
@ModItem(name = "batpack", tab = CreativeTab.IC2_MATERIALS, group = "battery_pack")
class BatPack : BatteryPackArmorItem(BACKPACK_ARMOR, tier = 1, maxCapacity = 60_000L) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val re = ReBatteryItem::class.instance()
            val circuit = Circuit::class.instance()
            if (re != Items.AIR && circuit != Items.AIR) {
                BatteryEnergyShapedRecipeDatagen.offer(
                    exporter = exporter,
                    recipeId = BatPack::class.id(),
                    result = BatPack::class.instance(),
                    pattern = listOf("RCR", "RPR", "R R"),
                    keys = mapOf(
                        'R' to re,
                        'C' to circuit,
                        'P' to Items.OAK_PLANKS
                    )
                )
            }
        }
    }
}

/**
 * 高级电池背包 (Advanced BatPack)
 * 高级电池背包，提供更大的 EU 容量。
 * 作为胸甲装备。
 */
@ModItem(name = "advanced_batpack", tab = CreativeTab.IC2_MATERIALS, group = "battery_pack")
class AdvancedBatPack : BatteryPackArmorItem(ADVANCED_BATPACK_ARMOR, tier = 2, maxCapacity = 600_000L) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val adv = AdvancedReBatteryItem::class.instance()
            val circuit = Circuit::class.instance()
            val copper = CopperCasing::class.instance()
            if (adv != Items.AIR && circuit != Items.AIR && copper != Items.AIR) {
                BatteryEnergyShapedRecipeDatagen.offer(
                    exporter = exporter,
                    recipeId = AdvancedBatPack::class.id(),
                    result = AdvancedBatPack::class.instance(),
                    pattern = listOf("ACA", "AUA", "A A"),
                    keys = mapOf(
                        'A' to adv,
                        'C' to circuit,
                        'U' to copper
                    )
                )
            }
        }
    }
}

/**
 * 能量背包 (Energy Pack)
 * 使用能量水晶的高容量背包，可存储大量 EU。
 * 作为胸甲装备。
 */
@ModItem(name = "energy_pack", tab = CreativeTab.IC2_MATERIALS, group = "battery_pack")
class EnergyPack : BatteryPackArmorItem(ENERGY_PACK_ARMOR, tier = 3, maxCapacity = 2_000_000L) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val advCircuit = AdvancedCircuit::class.instance()
            val iron = IronCasing::class.instance()
            val crystal = EnergyCrystalItem::class.instance()
            if (advCircuit != Items.AIR && iron != Items.AIR && crystal != Items.AIR) {
                BatteryEnergyShapedRecipeDatagen.offer(
                    exporter = exporter,
                    recipeId = EnergyPack::class.id(),
                    result = EnergyPack::class.instance(),
                    pattern = listOf("AIA", "EIE", "I I"),
                    keys = mapOf(
                        'A' to advCircuit,
                        'I' to iron,
                        'E' to crystal
                    )
                )
            }
        }
    }
}

/**
 * 兰波顿背包 (Lapotron Pack)
 * 使用兰波顿水晶的超大容量背包，顶级 EU 存储设备。
 * 作为胸甲装备。
 */
@ModItem(name = "lappack", tab = CreativeTab.IC2_MATERIALS, group = "battery_pack")
class LapPack : BatteryPackArmorItem(LAPPACK_ARMOR, tier = 4, maxCapacity = 60_000_000L) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeExporter) {
            val lapotron = LapotronCrystalItem::class.instance()
            val advCircuit = AdvancedCircuit::class.instance()
            val energyPack = EnergyPack::class.instance()
            if (lapotron != Items.AIR && advCircuit != Items.AIR && energyPack != Items.AIR) {
                BatteryEnergyShapedRecipeDatagen.offer(
                    exporter = exporter,
                    recipeId = LapPack::class.id(),
                    result = LapPack::class.instance(),
                    pattern = listOf("LAL", "LEL", "L L"),
                    keys = mapOf(
                        'L' to lapotron,
                        'A' to advCircuit,
                        'E' to energyPack
                    )
                )
            }
        }
    }
}

internal class CfPackFluidStorage(
    private val ctx: ContainerItemContext
) : Storage<FluidVariant> {

    override fun supportsInsertion(): Boolean = true

    override fun supportsExtraction(): Boolean = true

    override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (ctx.itemVariant.item !is CfPack) return 0L
        val fluid = resource.fluid
        if (!isConstructionFoamFluid(fluid)) return 0L
        val stack = ctx.itemVariant.toStack(1)
        val current = CfPack.getFluidAmount(stack)
        val space = CfPack.CAPACITY_DROPLETS - current
        if (space <= 0L) return 0L
        val inserted = minOf(maxAmount, space)
        if (inserted <= 0L) return 0L
        CfPack.setFluidAmount(stack, current + inserted)
        return if (ctx.exchange(ItemVariant.of(stack), 1, transaction) == 1L) inserted else 0L
    }

    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)
        if (ctx.itemVariant.item !is CfPack) return 0L
        val stack = ctx.itemVariant.toStack(1)
        val current = CfPack.getFluidAmount(stack)
        if (current <= 0L) return 0L
        if (!resource.isBlank && !isConstructionFoamFluid(resource.fluid)) return 0L
        val extracted = minOf(maxAmount, current)
        if (extracted <= 0L) return 0L
        CfPack.setFluidAmount(stack, current - extracted)
        return if (ctx.exchange(ItemVariant.of(stack), 1, transaction) == 1L) extracted else 0L
    }

    override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
        if (ctx.itemVariant.item !is CfPack) {
            return mutableListOf<StorageView<FluidVariant>>().iterator() as MutableIterator<StorageView<FluidVariant>>
        }
        val stack = ctx.itemVariant.toStack(1)
        val amt = CfPack.getFluidAmount(stack)
        if (amt <= 0L) {
            return mutableListOf<StorageView<FluidVariant>>().iterator() as MutableIterator<StorageView<FluidVariant>>
        }
        val variant = FluidVariant.of(ic2_120.content.fluid.ModFluids.CONSTRUCTION_FOAM_STILL)
        val view = object : StorageView<FluidVariant> {
            override fun getResource(): FluidVariant = variant
            override fun getAmount(): Long = amt
            override fun getCapacity(): Long = CfPack.CAPACITY_DROPLETS
            override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                this@CfPackFluidStorage.extract(resource, maxAmount, transaction)
            override fun isResourceBlank(): Boolean = false
        }
        return mutableListOf(view).iterator() as MutableIterator<StorageView<FluidVariant>>
    }
}

private fun isConstructionFoamFluid(fluid: net.minecraft.fluid.Fluid): Boolean =
    fluid == ic2_120.content.fluid.ModFluids.CONSTRUCTION_FOAM_STILL || fluid == ic2_120.content.fluid.ModFluids.CONSTRUCTION_FOAM_FLOWING
