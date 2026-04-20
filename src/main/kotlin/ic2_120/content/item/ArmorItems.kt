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
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.ArmorMaterial
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.client.item.TooltipContext
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.armor.ElectricArmorItem
import ic2_120.content.recipes.crafting.BatteryEnergyShapedRecipeDatagen


//todo 全套防化服，防化三件套加橡胶靴 可免疫特斯拉线圈伤害 可免疫触电伤害，只要有单个防化头盔，物品栏有压缩空气单元，就可以在气泡值用尽时消耗压缩空气单元回满
//todo 

// ========== 护甲材料 ==========

/**
 * 创建自定义护甲材料
 *
 * @param name 材料名称（用于内部标识）
 * @param durabilityMultiplier 耐久度倍率，基础值 × 倍率 = 实际耐久
 *        - 头盔：11 × 倍率
 *        - 胸甲：16 × 倍率
 *        - 护腿：15 × 倍率
 *        - 靴子：13 × 倍率
 * @param protection 各部位护甲值（Minecraft 原版护甲点数；
 *        1 点护甲 = 护甲条 1 半格，2 点护甲 = 1 个完整护甲图标）
 * @param enchantability 附魔能力，影响附魔品质
 * @param equipSound 装备音效
 * @param toughness 护甲韧性，减少被击退伤害（钻石为 2.0）
 * @param knockbackResistance 击退抗性（量子服最高 0.4）
 * @param repairIngredient 修复配方所需原料
 */
private fun createArmorMaterial(
    name: String,
    durabilityMultiplier: Int,
    protection: Map<ArmorItem.Type, Int>,
    enchantability: Int,
    equipSound: SoundEvent,
    toughness: Float,
    knockbackResistance: Float,
    repairIngredient: Ingredient
): ArmorMaterial = object : ArmorMaterial {
    // 各护甲部位的基础耐久度乘数
    private val durabilityMap = mapOf(
        ArmorItem.Type.HELMET to 11 * durabilityMultiplier,
        ArmorItem.Type.CHESTPLATE to 16 * durabilityMultiplier,
        ArmorItem.Type.LEGGINGS to 15 * durabilityMultiplier,
        ArmorItem.Type.BOOTS to 13 * durabilityMultiplier
    )

    override fun getDurability(type: ArmorItem.Type) = durabilityMap[type] ?: 0
    override fun getProtection(type: ArmorItem.Type) = protection[type] ?: 0
    override fun getEnchantability() = enchantability
    override fun getEquipSound() = equipSound
    override fun getRepairIngredient() = repairIngredient
    override fun getName() = name
    override fun getToughness() = toughness
    override fun getKnockbackResistance() = knockbackResistance
}

// ========== 修复原料 ==========
// 各护甲材料对应的修复物品（通过模组 ID 加载）
private val bronzeIngot = Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "bronze_ingot")))
private val rubber = Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "rubber")))
private val carbonFibre = Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "carbon_fibre")))
private val advancedAlloy = Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "alloy")))
private val iridium = Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "iridium")))

// ========== 护甲材料定义 ==========

// ========== 太阳能护甲 (Solar Armor) ==========
/**
 * 太阳能护甲材料，用于太阳能头盔。
 * 可在日光下自动充电的太阳能装备。
 */
private val SOLAR_ARMOR = createArmorMaterial(
    name = "ic2_solar",  // 使用 "solar" 作为名称，这样会查找 solar_1.png 和 solar_2.png
    durabilityMultiplier = 8,
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
    durabilityMultiplier = 8,
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

// ========== 橡胶靴 (Rubber Boots) ==========
/**
 * IC2 橡胶靴专用护甲材料，提供绝缘特性。
 * 参考原版 IC2 耐久：64 (靴子基础乘数 13，需要约 4.92 倍率)
 * 护甲值：1 (与皮革相同)
 */
private val RUBBER_ARMOR = createArmorMaterial(
    name = "ic2_rubber",
    durabilityMultiplier = 5,  // 13*5=65，接近原版 64
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

// ========== 防化服 (Hazmat Armor) ==========
/**
 * IC2 防化服套装专用护甲材料。
 * 参考原版 IC2 各部位耐久：均为 64
 * - 头盔：11*5.8≈64，胸甲：16*4=64，护腿：15*4.3≈64，靴子：13*4.9≈64
 * 采用统一 multiplier=5 以获得最接近的均衡值
 */
private val HAZMAT_ARMOR = createArmorMaterial(
    name = "ic2_hazmat",
    durabilityMultiplier = 5,  // 接近原版 64 的统一耐久
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
    durabilityMultiplier = 15,      // 超高耐久（青铜约 1.7 倍）
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
 * - 量子头盔：水下呼吸、自动补饥饿(需满锡罐)、夜视(Alt+M)、消除 debuff(中毒/凋零/辐射)
 * - 量子胸甲：飞行、防火、熄灭火焰
 * - 量子护腿：3 倍奔跑速度，冰上 9 倍（双击 W 激活）
 * - 量子靴子：减免坠落伤害、超级跳(Ctrl+空格，最高 9 格，1000 EU/次)
 * - 无电时持续受伤过久装备会消失
 */
private val QUANTUM_ARMOR = createArmorMaterial(
    name = "ic2_quantum",
    durabilityMultiplier = 25,    // 最高耐久（青铜约 2.8 倍）
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
    durabilityMultiplier = 33,
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

// ========== 背包护甲材料 ==========
/**
 * 背包类护甲材料，用于各种电池背包和喷气背包。
 * 提供基础的胸甲防护，主要功能在于储能或飞行。
 */
private val BACKPACK_ARMOR = createArmorMaterial(
    name = "batpack",  // 使用 batpack 作为基础，会查找 batpack_1.png
    durabilityMultiplier = 10,
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
    durabilityMultiplier = 12,
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
    durabilityMultiplier = 15,
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
    durabilityMultiplier = 20,
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
    durabilityMultiplier = 8,
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
    durabilityMultiplier = 5,
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
class BronzeHelmet : ArmorItem(BRONZE_ARMOR, ArmorItem.Type.HELMET, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
class BronzeChestplate : ArmorItem(BRONZE_ARMOR, ArmorItem.Type.CHESTPLATE, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
class BronzeLeggings : ArmorItem(BRONZE_ARMOR, ArmorItem.Type.LEGGINGS, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
class BronzeBoots : ArmorItem(BRONZE_ARMOR, ArmorItem.Type.BOOTS, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
 *
 * TODO: 实现每行走 5 格发电 1EU 的功能
 */
@ModItem(name = "rubber_boots", tab = CreativeTab.IC2_MATERIALS, group = "hazmat_armor")
class RubberBoots : ArmorItem(RUBBER_ARMOR, ArmorItem.Type.BOOTS, FabricItemSettings().maxCount(1)) {
    companion object {
        private const val WALK_ACC_KEY = "Ic2RubberBootsWalkAcc"
        private const val LAST_X_KEY = "Ic2RubberBootsLastX"
        private const val LAST_Z_KEY = "Ic2RubberBootsLastZ"
        private const val DISTANCE_PER_EU = 5.0

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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

    /**
     * 每 tick 触发的物品回调。
     * 仅在服务端、且靴子实际穿在脚部时统计水平移动距离：
     * - 每行走 5 格尝试发 1 EU
     * - 发出的 EU 会优先充入玩家背包中的可充电物品
     */
    override fun inventoryTick(stack: ItemStack, world: World, entity: net.minecraft.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return

        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.FEET) !== stack) return

        val nbt = stack.orCreateNbt
        val hasLastPos = nbt.contains(LAST_X_KEY) && nbt.contains(LAST_Z_KEY)
        if (!hasLastPos) {
            nbt.putDouble(LAST_X_KEY, player.x)
            nbt.putDouble(LAST_Z_KEY, player.z)
            return
        }

        val lastX = nbt.getDouble(LAST_X_KEY)
        val lastZ = nbt.getDouble(LAST_Z_KEY)
        val dx = player.x - lastX
        val dz = player.z - lastZ
        nbt.putDouble(LAST_X_KEY, player.x)
        nbt.putDouble(LAST_Z_KEY, player.z)

        if (!player.isOnGround) return
        val segment = kotlin.math.sqrt(dx * dx + dz * dz)
        if (segment <= 0.0 || segment > 2.0) return

        var acc = nbt.getDouble(WALK_ACC_KEY) + segment
        val toGenerate = kotlin.math.floor(acc / DISTANCE_PER_EU).toLong()
        if (toGenerate <= 0L) {
            nbt.putDouble(WALK_ACC_KEY, acc)
            return
        }

        var remaining = toGenerate
        remaining -= chargePlayerInventory(player, remaining)
        acc -= (toGenerate - remaining) * DISTANCE_PER_EU
        nbt.putDouble(WALK_ACC_KEY, acc.coerceAtLeast(0.0))
    }

    /**
     * 将指定 EU 充入玩家物品栏，返回实际充入量。
     * 支持：电池（IBatteryItem）与电动工具（IElectricTool）。
     */
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
class HazmatHelmet : ArmorItem(HAZMAT_ARMOR, ArmorItem.Type.HELMET, FabricItemSettings().maxCount(1)) {
    companion object {
        private const val CHECK_COOLDOWN_KEY = "AirCheckCooldown"
        private const val AIR_THRESHOLD = 60  // 当气泡值 <= 60 时触发（约 3 秒，20%）

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
        val nbt = stack.orCreateNbt
        val cooldown = nbt.getInt(CHECK_COOLDOWN_KEY)
        if (cooldown > 0) {
            nbt.putInt(CHECK_COOLDOWN_KEY, cooldown - 1)
            return
        }

        // 当气泡值低于阈值时（<= 60，约 3 秒）
        if (player.air <= AIR_THRESHOLD) {
            val airCellItem = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "air_cell"))
            val emptyCellItem = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell"))

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
                nbt.putInt(CHECK_COOLDOWN_KEY, 20)
            }
        }
    }
}

@ModItem(name = "hazmat_chestplate", tab = CreativeTab.IC2_MATERIALS, group = "hazmat_armor")
class HazmatChestplate : ArmorItem(HAZMAT_ARMOR, ArmorItem.Type.CHESTPLATE, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
class HazmatLeggings : ArmorItem(HAZMAT_ARMOR, ArmorItem.Type.LEGGINGS, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
class SolarHelmet : ArmorItem(SOLAR_ARMOR, ArmorItem.Type.HELMET, FabricItemSettings().maxCount(1)) {
    companion object {
        private const val EU_PER_TICK = 1L

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
class AlloyChestplate : ArmorItem(ALLOY_ARMOR, ArmorItem.Type.CHESTPLATE, FabricItemSettings().maxCount(1)) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
 * 作为胸甲装备。
 */
@ModItem(name = "cf_pack", tab = CreativeTab.IC2_MATERIALS, group = "armor")
class CfPack : ArmorItem(CF_PACK_ARMOR, ArmorItem.Type.CHESTPLATE, FabricItemSettings().maxCount(1)) {
    /**
     * CF 背包使用建筑泡沫系统，不走原版耐久系统。
     * 若不禁用，受伤时会累积 Damage NBT 导致耐久条混乱。
     */
    override fun isDamageable(): Boolean = false
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
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
    FabricItemSettings().maxCount(1)
) {
    companion object {
        private const val FLIGHT_ENABLED_KEY = "FlightEnabled"
        private const val FLIGHT_REMAINDER_KEY = "ElectricJetpackFlightRemainder"
        private const val FLIGHT_COST_BASE = 8L
        private const val FLIGHT_COST_REM_NUM = 1
        private const val FLIGHT_COST_REM_DEN = 3

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
    override val maxCapacity: Long = 30_000L

    override fun getDamageReduction(): Float = 0f

    fun isFlightEnabled(stack: ItemStack): Boolean =
        stack.orCreateNbt.getBoolean(FLIGHT_ENABLED_KEY)

    fun setFlightEnabled(stack: ItemStack, enabled: Boolean) {
        stack.orCreateNbt.putBoolean(FLIGHT_ENABLED_KEY, enabled)
    }

    fun toggleFlightEnabled(stack: ItemStack): Boolean {
        val enabled = !isFlightEnabled(stack)
        setFlightEnabled(stack, enabled)
        return enabled
    }

    fun consumeFlightEnergyPerTick(stack: ItemStack): Boolean {
        val nbt = stack.orCreateNbt
        var rem = nbt.getInt(FLIGHT_REMAINDER_KEY).coerceIn(0, FLIGHT_COST_REM_DEN - 1)
        var cost = FLIGHT_COST_BASE

        rem += FLIGHT_COST_REM_NUM
        if (rem >= FLIGHT_COST_REM_DEN) {
            cost += 1L
            rem -= FLIGHT_COST_REM_DEN
        }

        val energy = getEnergy(stack)
        if (energy < cost) {
            return false
        }

        setEnergy(stack, energy - cost)
        nbt.putInt(FLIGHT_REMAINDER_KEY, rem)
        return true
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)

        val enabled = isFlightEnabled(stack)
        val flightStatusText = if (enabled) "开启" else "关闭"

        // 计算剩余飞行时间（秒），基于平均能量消耗 8 + 1/3 EU/tick
        val energy = getEnergy(stack)
        val remainingSeconds = if (energy > 0) {
            val avgCostPerTick = FLIGHT_COST_BASE + FLIGHT_COST_REM_NUM.toDouble() / FLIGHT_COST_REM_DEN
            (energy / avgCostPerTick) / 20.0
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
class NightVisionGoggles : ArmorItem(NIGHT_VISION_ARMOR, ArmorItem.Type.HELMET, FabricItemSettings().maxCount(1)), IBatteryItem {
    override val tier: Int = 2
    override val maxCapacity: Long = 100_000L
    override val canChargeWireless: Boolean = false

    companion object {
        private const val ENERGY_KEY = "Energy"
        private const val ENABLED_KEY = "NightVisionEnabled"
        private const val ENERGY_PER_TICK = 1L
        private const val BLINDNESS_DURATION_TICKS = 80
        private const val NIGHT_VISION_DURATION_TICKS = 220

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
            val nbt = stack.orCreateNbt
            val enabled = !nbt.getBoolean(ENABLED_KEY)
            nbt.putBoolean(ENABLED_KEY, enabled)
            return enabled
        }
    }

    override fun getCurrentCharge(stack: ItemStack): Long = stack.orCreateNbt.getLong(ENERGY_KEY)

    override fun setCurrentCharge(stack: ItemStack, charge: Long) {
        stack.orCreateNbt.putLong(ENERGY_KEY, charge.coerceIn(0L, maxCapacity))
    }

    private fun isEnabled(stack: ItemStack): Boolean = stack.orCreateNbt.getBoolean(ENABLED_KEY)

    /**
     * 夜视镜使用电量系统，不走原版耐久系统。
     * 若不禁用，受伤时会累积 Damage NBT 导致耐久条混乱。
     */
    override fun isDamageable(): Boolean = false

    override fun inventoryTick(stack: ItemStack, world: World, entity: net.minecraft.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)
        if (world.isClient) return
        val player = entity as? PlayerEntity ?: return
        if (player.getEquippedStack(EquipmentSlot.HEAD) !== stack) return
        if (!isEnabled(stack)) return

        val energy = getCurrentCharge(stack)
        if (energy < ENERGY_PER_TICK) {
            setCurrentCharge(stack, 0)
            player.removeStatusEffect(StatusEffects.NIGHT_VISION)
            return
        }

        setCurrentCharge(stack, energy - ENERGY_PER_TICK)
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
        world: World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
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
    armorMaterial: ArmorMaterial,
    override val tier: Int,
    override val maxCapacity: Long,
) : ArmorItem(armorMaterial, ArmorItem.Type.CHESTPLATE, FabricItemSettings().maxCount(1)), IBatteryItem {

    init {
        require(tier in 1..4) { "电池背包等级须在 1–4，当前: $tier" }
        require(maxCapacity > 0) { "电池背包容量须 > 0" }
    }

    /**
     * 与 [WirelessBatteryItemBase] 语义一致：穿戴时可向物品栏中的 [IElectricTool] 输电。
     * 实际逻辑见 [autoFillElectricToolsInInventory]。
     */
    override val canChargeWireless: Boolean = true

    /**
     * 电池背包不走原版耐久系统，完全由 EU 能量驱动。
     *
     * 若不禁用，原版 [net.minecraft.entity.LivingEntity.damageArmor]
     * 会在每次受击时消耗原版耐久度，导致背包耐久条下降。
     */
    override fun isDamageable(): Boolean = false

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
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
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
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
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
