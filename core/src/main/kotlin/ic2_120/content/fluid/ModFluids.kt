package ic2_120.content.fluid

import net.minecraft.core.registries.Registries

import net.minecraft.core.registries.BuiltInRegistries

import ic2_120.compat.time

import ic2_120.compat.isClient

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.LiquidBlockContainer
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.level.material.FlowingFluid
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.item.BucketItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.core.Direction
import net.minecraft.world.level.redstone.Orientation
import net.minecraft.world.phys.Vec3
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.sounds.SoundSource
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting

/**
 * IC2 模组流体注册。
 * 基于 Fabric Wiki 流体教程实现，参考 /websites/fabricmc_net_develop 与 Fabric 官方文档。
 *
 * 流体纹理路径约定：ic2:block/fluid/{name}_still, ic2:block/fluid/{name}_flow
 * 无 flow 纹理时使用 still 纹理作为 flowing。
 */
object ModFluids {

    private const val TEXTURE_NS = "ic2"

    // Coolant - 冷却液（核反应堆）
    val COOLANT_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "coolant")) as FlowingFluid }
    val COOLANT_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_coolant")) as FlowingFluid }
    lateinit var COOLANT_BLOCK: Block
    lateinit var COOLANT_BUCKET: Item

    // Hot Coolant - 热冷却液
    val HOT_COOLANT_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "hot_coolant")) as FlowingFluid }
    val HOT_COOLANT_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_hot_coolant")) as FlowingFluid }
    lateinit var HOT_COOLANT_BLOCK: Block
    lateinit var HOT_COOLANT_BUCKET: Item

    // UU Matter - UU物质
    val UU_MATTER_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "uu_matter")) as FlowingFluid }
    val UU_MATTER_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_uu_matter")) as FlowingFluid }
    lateinit var UU_MATTER_BLOCK: Block
    lateinit var UU_MATTER_BUCKET: Item

    // Weed-EX - 除草剂
    val WEED_EX_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "weed_ex")) as FlowingFluid }
    val WEED_EX_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_weed_ex")) as FlowingFluid }
    lateinit var WEED_EX_BLOCK: Block
    lateinit var WEED_EX_BUCKET: Item

    // Pahoehoe Lava - 熔岩岩浆
    val PAHOEHOE_LAVA_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "pahoehoe_lava")) as FlowingFluid }
    val PAHOEHOE_LAVA_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_pahoehoe_lava")) as FlowingFluid }
    lateinit var PAHOEHOE_LAVA_BLOCK: Block
    lateinit var PAHOEHOE_LAVA_BUCKET: Item

    // Biofuel - 生物燃料
    val BIOFUEL_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "biofuel")) as FlowingFluid }
    val BIOFUEL_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_biofuel")) as FlowingFluid }
    lateinit var BIOFUEL_BLOCK: Block
    lateinit var BIOFUEL_BUCKET: Item

    // Biomass - 生物质
    val BIOMASS_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "biomass")) as FlowingFluid }
    val BIOMASS_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_biomass")) as FlowingFluid }
    lateinit var BIOMASS_BLOCK: Block
    lateinit var BIOMASS_BUCKET: Item

    // Distilled Water - 蒸馏水
    val DISTILLED_WATER_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "distilled_water")) as FlowingFluid }
    val DISTILLED_WATER_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_distilled_water")) as FlowingFluid }
    lateinit var DISTILLED_WATER_BLOCK: Block
    lateinit var DISTILLED_WATER_BUCKET: Item

    // Construction foam - 建筑泡沫（装罐机水+泡沫粉产出；桶由 registerFluid 注册，勿再 @ModItem 重复注册）
    val CONSTRUCTION_FOAM_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "construction_foam")) as FlowingFluid }
    val CONSTRUCTION_FOAM_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_construction_foam")) as FlowingFluid }
    lateinit var CONSTRUCTION_FOAM_BLOCK: Block
    lateinit var CONSTRUCTION_FOAM_BUCKET: Item

    // Creosote - 杂酚油
    val CREOSOTE_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "creosote")) as FlowingFluid }
    val CREOSOTE_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_creosote")) as FlowingFluid }
    lateinit var CREOSOTE_BLOCK: Block
    lateinit var CREOSOTE_BUCKET: Item

    // Compressed Air - 压缩空气（高炉）
    val COMPRESSED_AIR_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "compressed_air")) as FlowingFluid }
    val COMPRESSED_AIR_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_compressed_air")) as FlowingFluid }
    lateinit var COMPRESSED_AIR_BLOCK: Block
    lateinit var COMPRESSED_AIR_BUCKET: Item

    // Steam - 蒸汽
    val STEAM_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "steam")) as FlowingFluid }
    val STEAM_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_steam")) as FlowingFluid }
    lateinit var STEAM_BLOCK: Block
    lateinit var STEAM_BUCKET: Item

    // Superheated Steam - 过热蒸汽
    val SUPERHEATED_STEAM_STILL: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "superheated_steam")) as FlowingFluid }
    val SUPERHEATED_STEAM_FLOWING: FlowingFluid by lazy { BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "flowing_superheated_steam")) as FlowingFluid }
    lateinit var SUPERHEATED_STEAM_BLOCK: Block
    lateinit var SUPERHEATED_STEAM_BUCKET: Item

    /** 服务端安全的流体 → ARGB 颜色映射。注册时附带颜色，避免硬编码 if-else。 */
    private val fluidTintColors = mutableMapOf<Fluid, Int>()

    /** 获取流体的 ARGB 颜色，无记录则返回 null */
    fun getFluidTintOrNull(fluid: Fluid): Int? = fluidTintColors[fluid]

    fun register() {
        registerFluid("coolant", "coolant", "coolant")
        registerFluid("hot_coolant", "hot_coolant", "hot_coolant")
        registerFluid("uu_matter", "uu_matter", "uu_matter")
        registerFluid("weed_ex", "weed_ex", "weed_ex")  // 仅 still，flow 用 still
        registerFluid("pahoehoe_lava", "pahoehoe_lava", "pahoehoe_lava")  // 仅 still，flow 用 still
        registerFluid("biofuel", "biofuel", "biofuel", tintArgb = 0xFF97CA00.toInt())
        registerFluid("biomass", "biomass", "biomass", tintArgb = 0xFF476A3C.toInt())
        // 蒸馏水视觉复用原版水纹理
        registerFluid("distilled_water", "water_still", "water_flow", tintArgb = 0xFF3F76E4.toInt())
        // 建筑泡沫：客户端渲染复用通用流体贴图 + 着色（见 ModFluidClient）
        registerFluid("construction_foam", "fluid_still", "fluid_flow", tintArgb = 0xFFB4B4AF.toInt())
        registerFluid("creosote", "fluid_still", "fluid_flow", tintArgb = 0xFF4E2D14.toInt())
        registerFluid("compressed_air", "fluid_still", "fluid_flow", tintArgb = 0x80B0D8F0.toInt())
        // 蒸汽和过热蒸汽不能装在桶里（蒸汽单元/过热蒸汽单元代替）
        registerFluid("steam", "fluid_still", "fluid_flow", tintArgb = 0xFFD0D0D0.toInt(), withBucket = false, rises = true)
        registerFluid("superheated_steam", "fluid_still", "fluid_flow", tintArgb = 0xFFFFBEBE.toInt(), withBucket = false, rises = true)

        // 原版水着色（洗矿机、装罐机等机器 GUI 流体渲染使用）
        fluidTintColors[Fluids.WATER] = 0xFF3F76E4.toInt()
        fluidTintColors[Fluids.FLOWING_WATER] = 0xFF3F76E4.toInt()

        // 注册流体桶的玩家存储查找器，让 AE2 等模组能正确交互
        registerBucketPlayerStorage()
    }

    /**
     * 注册流体桶的流体存储。
     * 使用 FluidStorage.combinedItemApiProvider() 为桶提供流体存储接口。
     * 这样 AE2 等模组调用 FluidStorageUtil.interactWithFluidStorage 时能正确找到桶的存储。
     */
    private fun registerBucketPlayerStorage() {
        // 遍历所有已注册的桶物品并为它们注册存储提供者
        val bucketItems = listOf(
            COOLANT_BUCKET, HOT_COOLANT_BUCKET, UU_MATTER_BUCKET, WEED_EX_BUCKET,
            PAHOEHOE_LAVA_BUCKET, BIOFUEL_BUCKET, BIOMASS_BUCKET, DISTILLED_WATER_BUCKET,
            CONSTRUCTION_FOAM_BUCKET, CREOSOTE_BUCKET, COMPRESSED_AIR_BUCKET
        )

        for (bucket in bucketItems) {
            if (bucket is Ic2BucketItem) {
                FluidStorage.combinedItemApiProvider(bucket).register { ctx ->
                    // 返回桶的流体存储
                    BucketFluidStorage(ctx, bucket.bucketFluid)
                }
            }
        }
    }

    /**
     * 注册结果：包含新注册流体的 block 与 bucket 引用。
     * 供附属通过 [registerFluidFor] 获取返回值。
     */
    data class RegisteredFluid(
        val still: FlowingFluid,
        val flowing: FlowingFluid,
        val block: Block,
        val bucket: Item
    )

    /**
     * 为任意附属 modId 注册一套流体（Still + Flowing + LiquidBlock + Bucket）。
     *
     * 贴图路径约定：`<modId>:block/fluid/<stillTex>`、`<modId>:block/fluid/<flowTex>`
     * （与 core 自身注册一致，modId 替换 core 的 ic2 命名空间）。
     *
     * @param modId 附属 mod id（决定流体/方块/物品的注册命名空间与贴图命名空间）
     * @param name 流体名（注册为 `<modId>:<name>` 与 `<modId>:flowing_<name>`）
     * @param stillTex 静止贴图名（不含扩展名，路径为 `<modId>:block/fluid/<stillTex>`）
     * @param flowTex 流动贴图名
     * @param tintArgb 可选的 ARGB 着色值
     * @param withBucket 是否注册桶物品
     * @param rises 是否为上升流体（如蒸汽）
     * @return 注册结果（含 block 与 bucket 引用）
     */
    fun registerFluidFor(
        modId: String,
        name: String,
        stillTex: String,
        flowTex: String,
        tintArgb: Int? = null,
        withBucket: Boolean = true,
        rises: Boolean = false
    ): RegisteredFluid {
        // 1. 注册 Still 和 Flowing 流体
        val still = Registry.register(
            BuiltInRegistries.FLUID,
            Identifier.fromNamespaceAndPath(modId, name),
            Ic2Fluid.Still(name, stillTex, flowTex, rises)
        )
        val flowing = Registry.register(
            BuiltInRegistries.FLUID,
            Identifier.fromNamespaceAndPath(modId, "flowing_$name"),
            Ic2Fluid.Flowing(name, stillTex, flowTex, rises)
        )

        // ★ 必须在 LiquidBlock 创建前填充查找表，因为 LiquidBlock 构造函数会调用 getStillFluid()
        Ic2Fluid.stillFluidMap[name] = still
        Ic2Fluid.flowingFluidMap[name] = flowing

        if (tintArgb != null) {
            fluidTintColors[still] = tintArgb
            fluidTintColors[flowing] = tintArgb
        }

        // 2. 注册 LiquidBlock
        val block = Registry.register(
            BuiltInRegistries.BLOCK,
            Identifier.fromNamespaceAndPath(modId, name),
            if (rises) {
                SteamFluidBlock(still as FlowingFluid, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noCollision().noLootTable().setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(modId, name))))
            } else {
                LiquidBlock(still as FlowingFluid, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noCollision().noLootTable().setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(modId, name))))
            }
        )

        // 3. 注册 Bucket（可选）
        val bucket: Item = if (withBucket) {
            // 蒸馏水放置时转为普通水（仅 core 自身使用此约定；附属传 modId 时不触发）
            val placeOverride = if (modId == Ic2_120.MOD_ID && name == "distilled_water") Fluids.WATER else null
            val bucketItem = Ic2BucketItem(
                still,
                placeOverride,
                Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(modId, "${name}_bucket")))
            )
            val b = Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(modId, "${name}_bucket"),
                bucketItem
            )
            b
        } else {
            Items.AIR
        }

        // 4. 填充 block/bucket 查找表
        Ic2Fluid.blockMap[name] = block
        Ic2Fluid.bucketMap[name] = bucket

        return RegisteredFluid(still, flowing, block, bucket)
    }

    private fun registerFluid(name: String, stillTex: String, flowTex: String, tintArgb: Int? = null, withBucket: Boolean = true, rises: Boolean = false): RegisteredFluid {
        val modId = Ic2_120.MOD_ID
        val result = registerFluidFor(modId, name, stillTex, flowTex, tintArgb, withBucket, rises)
        val block = result.block
        val bucket = result.bucket

        // core 专属：把流体桶加入 IC2 材料创造栏
        if (withBucket && bucket !== Items.AIR) {
            val ic2MaterialsKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(modId, CreativeTab.IC2_MATERIALS.id))
            CreativeModeTabEvents.modifyOutputEvent(ic2MaterialsKey).register { entries ->
                entries.accept(bucket)
            }
        }

        // core 专属：把 block/bucket 赋值给模块级 lateinit 字段，供机器配方/模型引用
        when (name) {
            "coolant" -> {
                COOLANT_BLOCK = block
                COOLANT_BUCKET = bucket
            }
            "hot_coolant" -> {
                HOT_COOLANT_BLOCK = block
                HOT_COOLANT_BUCKET = bucket
            }
            "uu_matter" -> {
                UU_MATTER_BLOCK = block
                UU_MATTER_BUCKET = bucket
            }
            "weed_ex" -> {
                WEED_EX_BLOCK = block
                WEED_EX_BUCKET = bucket
            }
            "pahoehoe_lava" -> {
                PAHOEHOE_LAVA_BLOCK = block
                PAHOEHOE_LAVA_BUCKET = bucket
            }
            "biofuel" -> {
                BIOFUEL_BLOCK = block
                BIOFUEL_BUCKET = bucket
            }
            "biomass" -> {
                BIOMASS_BLOCK = block
                BIOMASS_BUCKET = bucket
            }
            "distilled_water" -> {
                DISTILLED_WATER_BLOCK = block
                DISTILLED_WATER_BUCKET = bucket
            }
            "construction_foam" -> {
                CONSTRUCTION_FOAM_BLOCK = block
                CONSTRUCTION_FOAM_BUCKET = bucket
            }
            "creosote" -> {
                CREOSOTE_BLOCK = block
                CREOSOTE_BUCKET = bucket
            }
            "compressed_air" -> {
                COMPRESSED_AIR_BLOCK = block
                COMPRESSED_AIR_BUCKET = bucket
            }
            "steam" -> {
                STEAM_BLOCK = block
                STEAM_BUCKET = bucket
            }
            "superheated_steam" -> {
                SUPERHEATED_STEAM_BLOCK = block
                SUPERHEATED_STEAM_BUCKET = bucket
            }
        }

        return result
    }

    /** 蒸汽世界方块：流体 tick 负责扩散，方块 tick 负责独立、可持久化的生命周期。 */
    class SteamFluidBlock(fluid: FlowingFluid, settings: BlockBehaviour.Properties) : LiquidBlock(fluid, settings) {
        companion object {
            /** 5 tick/阶段，共 40 阶段，即 10 秒。 */
            val AGE: IntegerProperty = IntegerProperty.create("age", 0, 39)
            private const val LIFECYCLE_TICK_RATE = 5
            private const val MAX_AGE = 39
        }

        override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
            super.createBlockStateDefinition(builder)
            builder.add(AGE)
        }

        override fun onPlace(state: BlockState, world: Level, pos: BlockPos, oldState: BlockState, notify: Boolean) {
            super.onPlace(state, world, pos, oldState, notify)
            if (!world.isClient) {
                world.scheduleTick(pos, this, LIFECYCLE_TICK_RATE)
            }
        }

        @Deprecated("override deprecated member", level = DeprecationLevel.WARNING)
        override fun neighborChanged(state: BlockState, world: Level, pos: BlockPos, sourceBlock: Block, orientation: Orientation?, notify: Boolean) {
            @Suppress("DEPRECATION")
            super.neighborChanged(state, world, pos, sourceBlock, orientation, notify)
            if (!world.isClient) {
                world.scheduleTick(pos, this, LIFECYCLE_TICK_RATE)
            }
        }

        override fun isRandomlyTicking(state: BlockState): Boolean = true

        override fun randomTick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
            // 兼容升级前已经留在存档中的无调度蒸汽块。
            world.scheduleTick(pos, this, LIFECYCLE_TICK_RATE)
            val currentFluid = state.fluidState.getType()
            world.scheduleTick(pos, currentFluid, currentFluid.getTickDelay(world))
        }

        @Deprecated("override deprecated member", level = DeprecationLevel.WARNING)
        override fun tick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
            val nextAge = state.getValue(AGE) + 1
            if (nextAge <= MAX_AGE) {
                // AGE 只在服务端用于生命周期，不需要每 5 tick 向客户端广播。
                world.setBlock(pos, state.setValue(AGE, nextAge), Block.UPDATE_KNOWN_SHAPE or Block.UPDATE_INVISIBLE)
                world.scheduleTick(pos, this, LIFECYCLE_TICK_RATE)
                return
            }

            if (state.`is`(SUPERHEATED_STEAM_BLOCK)) {
                // 过热蒸汽冷却为普通蒸汽，并保留当前扩散等级；新方块从 0 重新计时。
                val cooled = STEAM_BLOCK.defaultBlockState()
                    .setValue(LEVEL, state.getValue(LEVEL))
                    .setValue(AGE, 0)
                world.setBlock(pos, cooled, Block.UPDATE_ALL)
            } else {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)
            }
        }
    }

    /**
     * 普通液体沿用原版 FlowingFluid；rises 流体使用有限距离的向上/水平扩散。
     * 蒸汽生命周期由 SteamFluidBlock 的方块状态管理。
     */
    abstract class Ic2Fluid(
        protected val name: String,
        private val stillTex: String,
        private val flowTex: String,
        protected val rises: Boolean = false
    ) : FlowingFluid() {

        companion object {
            /** 注册名 → Still/Flowing/Block/Bucket 查找表。由 registerFluid()/registerFluidFor() 自动填充，替代 getStill/getFlowing/getBlock/getIc2Bucket 中的 when。 */
            val stillFluidMap = mutableMapOf<String, Fluid>()
            val flowingFluidMap = mutableMapOf<String, Fluid>()
            val blockMap = mutableMapOf<String, Block>()
            val bucketMap = mutableMapOf<String, Item>()
        }

        override fun getSource(): Fluid = getStillFluid()
        override fun getFlowing(): Fluid = getFlowingFluid()
        override fun getBucket(): Item = getIc2Bucket()
        override fun createLegacyBlock(state: FluidState): BlockState =
            getBlock().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state))

        protected abstract fun getStillFluid(): Fluid
        protected abstract fun getFlowingFluid(): Fluid
        protected abstract fun getBlock(): Block
        protected abstract fun getIc2Bucket(): Item

        override fun isSame(fluid: Fluid): Boolean = fluid == getStillFluid() || fluid == getFlowingFluid()
        override fun canConvertToSource(world: ServerLevel): Boolean = false
        override fun beforeDestroyingBlock(world: net.minecraft.world.level.LevelAccessor, pos: net.minecraft.core.BlockPos, state: BlockState) {
            val entity = if (state.hasBlockEntity()) world.getBlockEntity(pos) else null
            Block.dropResources(state, world, pos, entity)
        }
        override fun canBeReplacedWith(
            state: FluidState,
            world: BlockGetter,
            pos: BlockPos,
            fluid: Fluid,
            direction: Direction
        ): Boolean = false
        override fun getSlopeFindDistance(world: LevelReader): Int = 4
        override fun getDropOff(world: LevelReader): Int = 1
        override fun getTickDelay(world: LevelReader): Int = 5
        override fun getExplosionResistance(): Float = 100f

        override fun getFlow(world: BlockGetter, pos: BlockPos, state: FluidState): Vec3 =
            if (rises) Vec3(0.0, 0.08, 0.0) else super.getFlow(world, pos, state)

        override fun getOwnHeight(state: FluidState): Float =
            if (rises) 1.0f else super.getOwnHeight(state)

        override fun getHeight(state: FluidState, world: BlockGetter, pos: BlockPos): Float =
            if (rises) 1.0f else super.getHeight(state, world, pos)

        // ========== 上升流体（蒸汽）行为 ==========

        override fun getNewLiquid(world: ServerLevel, pos: BlockPos, state: BlockState): FluidState {
            if (!rises) return super.getNewLiquid(world, pos, state)

            // 源块由生命周期管理；流动块从下方和水平邻居获取强度。
            val current = world.getFluidState(pos)
            if (current.isSource && current.getType().isSame(this)) {
                return getSource(false)
            }

            var strongestLevel = propagatedLevel(world.getFluidState(pos.below()), world)
            for (direction in Direction.Plane.HORIZONTAL) {
                strongestLevel = maxOf(strongestLevel, propagatedLevel(world.getFluidState(pos.relative(direction)), world))
            }

            return if (strongestLevel > 0) getFlowing(strongestLevel, false) else Fluids.EMPTY.defaultFluidState()
        }

        private fun propagatedLevel(state: FluidState, world: LevelReader): Int {
            if (state.isEmpty || !state.getType().isSame(this)) return 0
            val level = if (state.isSource()) 8 else state.getAmount()
            return (level - getDropOff(world)).coerceAtLeast(0)
        }

        override fun spread(world: ServerLevel, pos: BlockPos, blockState: BlockState, state: FluidState) {
            if (state.isEmpty || !rises) {
                super.spread(world, pos, blockState, state)
                return
            }

            val blockState = world.getBlockState(pos)

            // 优先向上，同时向四周扩散；不向下传播。
            val directions = arrayOf(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)
            for (direction in directions) {
                val targetPos = pos.relative(direction)
                val targetBlockState = world.getBlockState(targetPos)
                val targetFluidState = getNewLiquid(world, targetPos, targetBlockState)
                if (targetFluidState.isEmpty) continue
                val existingFluid = targetBlockState.fluidState
                val canPlace = if (targetBlockState.block is LiquidBlockContainer) {
                    (targetBlockState.block as LiquidBlockContainer).canPlaceLiquid(null, world, targetPos, targetBlockState, targetFluidState.type)
                } else targetBlockState.canBeReplaced()
                if (!existingFluid.isSource && canPlace &&
                    existingFluid.canBeReplacedWith(world, targetPos, targetFluidState.type, direction)
                ) {
                    spreadTo(world, targetPos, targetBlockState, direction, targetFluidState)
                }
            }
        }

        override fun tick(world: ServerLevel, pos: BlockPos, blockState: BlockState, state: FluidState) {
            if (!rises) {
                super.tick(world, pos, blockState, state)
                return
            }

            // 蒸汽伤害：每秒 2 血
            applySteamDamage(world, pos)

            if (state.isSource()) {
                world.scheduleTick(pos, state.getType(), getTickDelay(world))
                spread(world, pos, world.getBlockState(pos), state)
                return
            }

            // 流动状态：上升流体需要始终重调度 tick，以便检测源消散
            val newState = getNewLiquid(world, pos, world.getBlockState(pos))
            if (newState.isEmpty) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)
            } else {
                if (!newState.equals(state)) {
                    val oldBlockState = world.getBlockState(pos)
                    val newBlockState = newState.createLegacyBlock().let { updated ->
                        if (oldBlockState.hasProperty(SteamFluidBlock.AGE) && updated.hasProperty(SteamFluidBlock.AGE)) {
                            updated.setValue(SteamFluidBlock.AGE, oldBlockState.getValue(SteamFluidBlock.AGE))
                        } else {
                            updated
                        }
                    }
                    world.setBlock(pos, newBlockState, Block.UPDATE_CLIENTS)
                    world.updateNeighborsAt(pos, newBlockState.block)
                }
                val actualFluid = world.getFluidState(pos).getType()
                world.scheduleTick(pos, actualFluid, getTickDelay(world))
            }
            val actualState = world.getFluidState(pos)
            if (!actualState.isEmpty) {
                spread(world, pos, world.getBlockState(pos), actualState)
            }
        }

        /** 蒸汽伤害：每 20 tick（1 秒）对格内生物造成 2 点伤害 */
        private fun applySteamDamage(world: Level, pos: BlockPos) {
            if (world.isClient) return
            if (world.time % 20L >= 5L) return
            val entities = world.getEntitiesOfClass(LivingEntity::class.java, AABB(pos)) { !it.isSpectator }
            if (entities.isEmpty()) return
            val damageKey = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Ic2_120.MOD_ID, "steam"))
            val source = DamageSource(world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(damageKey).orElseThrow())
            for (entity in entities) {
                entity.hurt(source, 2.0f)
            }
        }

        class Still(name: String, stillTex: String, flowTex: String, rises: Boolean = false) : Ic2Fluid(name, stillTex, flowTex, rises) {
            override fun getAmount(state: FluidState): Int = 8
            override fun isSource(state: FluidState): Boolean = true
            override fun getStillFluid(): Fluid = stillFluidMap[name] ?: throw IllegalStateException("Unknown fluid: $name")
            override fun getFlowingFluid(): Fluid = flowingFluidMap[name] ?: throw IllegalStateException("Unknown fluid: $name")
            override fun getBlock(): Block = blockMap[name] ?: throw IllegalStateException("Unknown fluid: $name")
            override fun getIc2Bucket(): Item = bucketMap[name] ?: throw IllegalStateException("Unknown fluid: $name")
        }

        class Flowing(name: String, stillTex: String, flowTex: String, rises: Boolean = false) : Ic2Fluid(name, stillTex, flowTex, rises) {
            override fun createFluidStateDefinition(builder: StateDefinition.Builder<Fluid, FluidState>) {
                super.createFluidStateDefinition(builder)
                builder.add(LEVEL)
            }
            override fun getAmount(state: FluidState): Int = state.getValue(LEVEL)
            override fun isSource(state: FluidState): Boolean = false
            override fun getStillFluid(): Fluid = stillFluidMap[name] ?: throw IllegalStateException("Unknown fluid: $name")
            override fun getFlowingFluid(): Fluid = flowingFluidMap[name] ?: throw IllegalStateException("Unknown fluid: $name")
            override fun getBlock(): Block = blockMap[name] ?: throw IllegalStateException("Unknown fluid: $name")
            override fun getIc2Bucket(): Item = bucketMap[name] ?: throw IllegalStateException("Unknown fluid: $name")
        }
    }

    /**
     * IC2 自定义流体桶类。
     * 与原版 BucketItem 的区别：覆盖 useOnBlock 方法，确保在与第三方 FluidStorage（如 AE2 储罐）交互时正确处理事务。
     *
     * 问题背景：
     * - 原版 BucketItem 可能依赖默认的 onUse 处理流程
     * - 与某些 mod 的 Storage 实现交互时，可能因事务管理冲突导致崩溃
     * - 解决方案：直接使用 Fabric Transfer API 进行流体传输，绕过可能有问题的高级 API
     */
    open class Ic2BucketItem(
        internal val bucketFluid: FlowingFluid, // 改名为 bucketFluid 避免与父类冲突
        private val placeFluidOverride: FlowingFluid? = null,
        settings: Item.Properties
    ) : BucketItem(
        bucketFluid,
        // 1.21.1: Item#getCraftingRemainingItem() 已 final 且组件化，余物经 Properties.craftRemainder 注入
        settings.stacksTo(1).craftRemainder(Items.BUCKET)
    ) {


        /** 实际放置到世界的流体（用于蒸馏水等特殊情况） */
        private val fluidToPlace: FlowingFluid
            get() = placeFluidOverride ?: bucketFluid

        @Environment(EnvType.CLIENT)
        override fun appendHoverText(stack: ItemStack, context: Item.TooltipContext, display: net.minecraft.world.item.component.TooltipDisplay, consumer: java.util.function.Consumer<Component>, type: TooltipFlag) {
        val tooltip = ic2_120.compat.TooltipSinkList(consumer)
            super.appendHoverText(stack, context, display, consumer, type)
            if (placeFluidOverride != null) {
                tooltip.add(Component.translatable("tooltip.ic2_120.distilled_water_places_water").withStyle(ChatFormatting.GRAY))
            }
        }

        override fun emptyContents(
            player: LivingEntity?,
            world: Level,
            pos: BlockPos,
            hitResult: BlockHitResult?
        ): Boolean {
            val actualFluid = fluidToPlace
            val state = world.getBlockState(pos)
            val block = state.block

            // LiquidBlockContainer：如炼药锅等可注入液体的方块
            if (block is LiquidBlockContainer) {
                if (block.canPlaceLiquid(player, world, pos, state, actualFluid)) {
                    block.placeLiquid(world, pos, state, actualFluid.defaultFluidState())
                    actualFluid.getPickupSound().ifPresent { world.playSound(player, pos, it, SoundSource.BLOCKS, 1f, 1f) }
                    world.gameEvent(player, GameEvent.FLUID_PLACE, pos)
                    return true
                }
            }

            // 普通方块：在点击位置放置流体（需可替换，如空气、流体本身）
            if (state.canBeReplaced() || state.fluidState.isSource()) {
                if (world.setBlock(pos, actualFluid.defaultFluidState().createLegacyBlock(), 3)) {
                    actualFluid.getPickupSound().ifPresent { world.playSound(player, pos, it, SoundSource.BLOCKS, 1f, 1f) }
                    world.gameEvent(player, GameEvent.FLUID_PLACE, pos)
                    return true
                }
            }

            // 固体方块：在点击面朝向的相邻格放置
            val adjacentPos = hitResult?.direction?.let { pos.relative(it) } ?: pos.above()
            val adjacentState = world.getBlockState(adjacentPos)
            if (adjacentState.canBeReplaced() || adjacentState.fluidState.isSource()) {
                if (world.setBlock(adjacentPos, actualFluid.defaultFluidState().createLegacyBlock(), 3)) {
                    actualFluid.getPickupSound().ifPresent { world.playSound(player, adjacentPos, it, SoundSource.BLOCKS, 1f, 1f) }
                    world.gameEvent(player, GameEvent.FLUID_PLACE, adjacentPos)
                    return true
                }
            }

            return false
        }
    }

    /**
     * 桶的流体存储实现。
     * 为手持桶的玩家提供流体存储接口，使 AE2 等模组能正确交互。
     * 继承 SingleVariantStorage 简化实现。
     */
    private class BucketFluidStorage(
        private val ctx: ContainerItemContext,
        private val bucketFluid: FlowingFluid
    ) : SingleVariantStorage<FluidVariant>() {

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()

        override fun getCapacity(variant: FluidVariant): Long = FluidConstants.BUCKET

        init {
            // 桶初始时包含一桶流体
            this.variant = FluidVariant.of(bucketFluid)
            this.amount = FluidConstants.BUCKET
        }

        override fun isResourceBlank(): Boolean {
            return variant.isBlank || variant.fluid == Fluids.EMPTY
        }

        override fun getAmount(): Long {
            // 如果桶是满的，返回 BUCKET，否则返回 0
            val stack = ctx.itemVariant
            return if (!stack.isBlank && stack.item is BucketItem) {
                FluidConstants.BUCKET
            } else {
                0
            }
        }

        /**
         * 提取流体（从桶倒出到储罐）
         * 桶只支持提取，不支持插入（因为已经满了）
         */
        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (isResourceBlank()) return 0
            if (resource.fluid != bucketFluid) return 0
            if (maxAmount < FluidConstants.BUCKET) return 0

            // 在事务中交换物品：满桶 → 空桶
            val emptyBucket = ItemVariant.of(Items.BUCKET)
            if (ctx.exchange(emptyBucket, 1, transaction) == 1L) {
                return FluidConstants.BUCKET
            }

            return 0
        }

        /**
         * 插入流体（从储罐填充到桶）
         * 满桶不支持插入
         */
        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            // 桶已经满了，不能插入
            return 0
        }
    }

    /**
     * 从 [Ic2BucketItem] 中提取流体。
     * 作为 [FluidStorage.ITEM] 的硬编码后备方案，用于升级 GUI 中检测容器流体。
     */
    fun getFluidFromModBucket(item: Item): FlowingFluid? {
        return (item as? Ic2BucketItem)?.bucketFluid
    }

    // ========== 特定流体判定 ==========

    /** 蒸汽或过热蒸汽 */
    fun isSteam(fluid: Fluid): Boolean =
        fluid == STEAM_STILL || fluid == STEAM_FLOWING ||
        fluid == SUPERHEATED_STEAM_STILL || fluid == SUPERHEATED_STEAM_FLOWING

    /** 压缩空气 */
    fun isCompressedAir(fluid: Fluid): Boolean =
        fluid == COMPRESSED_AIR_STILL || fluid == COMPRESSED_AIR_FLOWING
}
