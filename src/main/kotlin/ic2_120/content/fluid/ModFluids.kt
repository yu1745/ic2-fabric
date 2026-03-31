package ic2_120.content.fluid

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.type
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.FluidBlock
import net.minecraft.block.FluidFillable
import net.minecraft.block.AbstractBlock
import net.minecraft.fluid.FlowableFluid
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.item.BucketItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateManager
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import net.minecraft.world.WorldView
import net.minecraft.world.event.GameEvent
import net.minecraft.sound.SoundCategory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.ActionResult
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.item.TooltipContext
import net.minecraft.text.Text
import net.minecraft.util.Formatting

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
    val COOLANT_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "coolant")) as FlowableFluid }
    val COOLANT_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_coolant")) as FlowableFluid }
    lateinit var COOLANT_BLOCK: Block
    lateinit var COOLANT_BUCKET: Item

    // Hot Coolant - 热冷却液
    val HOT_COOLANT_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "hot_coolant")) as FlowableFluid }
    val HOT_COOLANT_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_hot_coolant")) as FlowableFluid }
    lateinit var HOT_COOLANT_BLOCK: Block
    lateinit var HOT_COOLANT_BUCKET: Item

    // UU Matter - UU物质
    val UU_MATTER_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "uu_matter")) as FlowableFluid }
    val UU_MATTER_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_uu_matter")) as FlowableFluid }
    lateinit var UU_MATTER_BLOCK: Block
    lateinit var UU_MATTER_BUCKET: Item

    // Weed-EX - 除草剂
    val WEED_EX_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "weed_ex")) as FlowableFluid }
    val WEED_EX_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_weed_ex")) as FlowableFluid }
    lateinit var WEED_EX_BLOCK: Block
    lateinit var WEED_EX_BUCKET: Item

    // Pahoehoe Lava - 熔岩岩浆
    val PAHOEHOE_LAVA_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "pahoehoe_lava")) as FlowableFluid }
    val PAHOEHOE_LAVA_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_pahoehoe_lava")) as FlowableFluid }
    lateinit var PAHOEHOE_LAVA_BLOCK: Block
    lateinit var PAHOEHOE_LAVA_BUCKET: Item

    // Biofuel - 生物燃料
    val BIOFUEL_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "biofuel")) as FlowableFluid }
    val BIOFUEL_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_biofuel")) as FlowableFluid }
    lateinit var BIOFUEL_BLOCK: Block
    lateinit var BIOFUEL_BUCKET: Item

    // Biomass - 生物质
    val BIOMASS_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "biomass")) as FlowableFluid }
    val BIOMASS_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_biomass")) as FlowableFluid }
    lateinit var BIOMASS_BLOCK: Block
    lateinit var BIOMASS_BUCKET: Item

    // Distilled Water - 蒸馏水
    val DISTILLED_WATER_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "distilled_water")) as FlowableFluid }
    val DISTILLED_WATER_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_distilled_water")) as FlowableFluid }
    lateinit var DISTILLED_WATER_BLOCK: Block
    lateinit var DISTILLED_WATER_BUCKET: Item

    // Construction foam - 建筑泡沫（装罐机水+泡沫粉产出；桶由 registerFluid 注册，勿再 @ModItem 重复注册）
    val CONSTRUCTION_FOAM_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "construction_foam")) as FlowableFluid }
    val CONSTRUCTION_FOAM_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_construction_foam")) as FlowableFluid }
    lateinit var CONSTRUCTION_FOAM_BLOCK: Block
    lateinit var CONSTRUCTION_FOAM_BUCKET: Item

    // Creosote - 杂酚油
    val CREOSOTE_STILL: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "creosote")) as FlowableFluid }
    val CREOSOTE_FLOWING: FlowableFluid by lazy { Registries.FLUID.get(Identifier(Ic2_120.MOD_ID, "flowing_creosote")) as FlowableFluid }
    lateinit var CREOSOTE_BLOCK: Block
    lateinit var CREOSOTE_BUCKET: Item

    fun register() {
        registerFluid("coolant", "coolant", "coolant")
        registerFluid("hot_coolant", "hot_coolant", "hot_coolant")
        registerFluid("uu_matter", "uu_matter", "uu_matter")
        registerFluid("weed_ex", "weed_ex", "weed_ex")  // 仅 still，flow 用 still
        registerFluid("pahoehoe_lava", "pahoehoe_lava", "pahoehoe_lava")  // 仅 still，flow 用 still
        registerFluid("biofuel", "biofuel", "biofuel")
        registerFluid("biomass", "biomass", "biomass")
        // 蒸馏水视觉复用原版水纹理
        registerFluid("distilled_water", "water_still", "water_flow")
        // 建筑泡沫：客户端渲染复用通用流体贴图 + 着色（见 ModFluidClient）
        registerFluid("construction_foam", "fluid_still", "fluid_flow")
        registerFluid("creosote", "fluid_still", "fluid_flow")

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
            CONSTRUCTION_FOAM_BUCKET, CREOSOTE_BUCKET
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

    private fun registerFluid(name: String, stillTex: String, flowTex: String) {
        val modId = Ic2_120.MOD_ID

        // 1. 注册 Still 和 Flowing 流体
        val still = Registry.register(
            Registries.FLUID,
            Identifier(modId, name),
            Ic2Fluid.Still(name, stillTex, flowTex)
        )
        val flowing = Registry.register(
            Registries.FLUID,
            Identifier(modId, "flowing_$name"),
            Ic2Fluid.Flowing(name, stillTex, flowTex)
        )

        // 2. 注册 FluidBlock
        val block = Registry.register(
            Registries.BLOCK,
            Identifier(modId, name),
            FluidBlock(still as FlowableFluid, AbstractBlock.Settings.copy(Blocks.WATER).noCollision().dropsNothing())
        )

        // 3. 注册 Bucket
        // 使用自定义 Ic2BucketItem 确保与第三方 Storage 正确交互，避免事务冲突崩溃
        val bucketItem = Ic2BucketItem(
            still,
            if (name == "distilled_water") Fluids.WATER else null, // 蒸馏水放置时转为普通水
            FabricItemSettings().recipeRemainder(Items.BUCKET).maxCount(1)
        )
        val bucket = Registry.register(
            Registries.ITEM,
            Identifier(modId, "${name}_bucket"),
            bucketItem
        )

        // 4. 设置流体类的 block 和 bucket 引用（通过反射或延迟初始化）
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
        }

        // 5. 添加到创造模式物品栏
        val ic2MaterialsKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(modId, CreativeTab.IC2_MATERIALS.id))
        ItemGroupEvents.modifyEntriesEvent(ic2MaterialsKey).register { entries ->
            entries.add(bucket)
        }
    }

    /**
     * IC2 抽象流体基类。
     * 行为类似水：非无限、流动速度 4、每格衰减 1、更新间隔 5 tick。
     */
    abstract class Ic2Fluid(
        protected val name: String,
        private val stillTex: String,
        private val flowTex: String
    ) : FlowableFluid() {

        override fun getStill(): Fluid = getStillFluid()
        override fun getFlowing(): Fluid = getFlowingFluid()
        override fun getBucketItem(): Item = getBucket()
        override fun toBlockState(state: FluidState): BlockState =
            getBlock().defaultState.with(FluidBlock.LEVEL, getBlockStateLevel(state))

        protected abstract fun getStillFluid(): Fluid
        protected abstract fun getFlowingFluid(): Fluid
        protected abstract fun getBlock(): Block
        protected abstract fun getBucket(): Item

        override fun matchesType(fluid: Fluid): Boolean = fluid == getStillFluid() || fluid == getFlowingFluid()
        override fun isInfinite(world: World): Boolean = false
        override fun beforeBreakingBlock(world: net.minecraft.world.WorldAccess, pos: net.minecraft.util.math.BlockPos, state: BlockState) {
            val entity = if (state.hasBlockEntity()) world.getBlockEntity(pos) else null
            Block.dropStacks(state, world, pos, entity)
        }
        override fun canBeReplacedWith(
            state: FluidState,
            world: BlockView,
            pos: BlockPos,
            fluid: Fluid,
            direction: Direction
        ): Boolean = false
        override fun getFlowSpeed(world: WorldView): Int = 4
        override fun getLevelDecreasePerBlock(world: WorldView): Int = 1
        override fun getTickRate(world: WorldView): Int = 5
        override fun getBlastResistance(): Float = 100f

        class Still(name: String, stillTex: String, flowTex: String) : Ic2Fluid(name, stillTex, flowTex) {
            override fun getLevel(state: FluidState): Int = 8
            override fun isStill(state: FluidState): Boolean = true
            override fun getStillFluid(): Fluid = when (name) {
                "coolant" -> COOLANT_STILL
                "hot_coolant" -> HOT_COOLANT_STILL
                "uu_matter" -> UU_MATTER_STILL
                "weed_ex" -> WEED_EX_STILL
                "pahoehoe_lava" -> PAHOEHOE_LAVA_STILL
                "biofuel" -> BIOFUEL_STILL
                "biomass" -> BIOMASS_STILL
                "distilled_water" -> DISTILLED_WATER_STILL
                "construction_foam" -> CONSTRUCTION_FOAM_STILL
                "creosote" -> CREOSOTE_STILL
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getFlowingFluid(): Fluid = when (name) {
                "coolant" -> COOLANT_FLOWING
                "hot_coolant" -> HOT_COOLANT_FLOWING
                "uu_matter" -> UU_MATTER_FLOWING
                "weed_ex" -> WEED_EX_FLOWING
                "pahoehoe_lava" -> PAHOEHOE_LAVA_FLOWING
                "biofuel" -> BIOFUEL_FLOWING
                "biomass" -> BIOMASS_FLOWING
                "distilled_water" -> DISTILLED_WATER_FLOWING
                "construction_foam" -> CONSTRUCTION_FOAM_FLOWING
                "creosote" -> CREOSOTE_FLOWING
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getBlock(): Block = when (name) {
                "coolant" -> COOLANT_BLOCK
                "hot_coolant" -> HOT_COOLANT_BLOCK
                "uu_matter" -> UU_MATTER_BLOCK
                "weed_ex" -> WEED_EX_BLOCK
                "pahoehoe_lava" -> PAHOEHOE_LAVA_BLOCK
                "biofuel" -> BIOFUEL_BLOCK
                "biomass" -> BIOMASS_BLOCK
                "distilled_water" -> DISTILLED_WATER_BLOCK
                "construction_foam" -> CONSTRUCTION_FOAM_BLOCK
                "creosote" -> CREOSOTE_BLOCK
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getBucket(): Item = when (name) {
                "coolant" -> COOLANT_BUCKET
                "hot_coolant" -> HOT_COOLANT_BUCKET
                "uu_matter" -> UU_MATTER_BUCKET
                "weed_ex" -> WEED_EX_BUCKET
                "pahoehoe_lava" -> PAHOEHOE_LAVA_BUCKET
                "biofuel" -> BIOFUEL_BUCKET
                "biomass" -> BIOMASS_BUCKET
                "distilled_water" -> DISTILLED_WATER_BUCKET
                "construction_foam" -> CONSTRUCTION_FOAM_BUCKET
                "creosote" -> CREOSOTE_BUCKET
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
        }

        class Flowing(name: String, stillTex: String, flowTex: String) : Ic2Fluid(name, stillTex, flowTex) {
            override fun appendProperties(builder: StateManager.Builder<Fluid, FluidState>) {
                super.appendProperties(builder)
                builder.add(LEVEL)
            }
            override fun getLevel(state: FluidState): Int = state.get(LEVEL)
            override fun isStill(state: FluidState): Boolean = false
            override fun getStillFluid(): Fluid = when (name) {
                "coolant" -> COOLANT_STILL
                "hot_coolant" -> HOT_COOLANT_STILL
                "uu_matter" -> UU_MATTER_STILL
                "weed_ex" -> WEED_EX_STILL
                "pahoehoe_lava" -> PAHOEHOE_LAVA_STILL
                "biofuel" -> BIOFUEL_STILL
                "biomass" -> BIOMASS_STILL
                "distilled_water" -> DISTILLED_WATER_STILL
                "construction_foam" -> CONSTRUCTION_FOAM_STILL
                "creosote" -> CREOSOTE_STILL
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getFlowingFluid(): Fluid = when (name) {
                "coolant" -> COOLANT_FLOWING
                "hot_coolant" -> HOT_COOLANT_FLOWING
                "uu_matter" -> UU_MATTER_FLOWING
                "weed_ex" -> WEED_EX_FLOWING
                "pahoehoe_lava" -> PAHOEHOE_LAVA_FLOWING
                "biofuel" -> BIOFUEL_FLOWING
                "biomass" -> BIOMASS_FLOWING
                "distilled_water" -> DISTILLED_WATER_FLOWING
                "construction_foam" -> CONSTRUCTION_FOAM_FLOWING
                "creosote" -> CREOSOTE_FLOWING
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getBlock(): Block = when (name) {
                "coolant" -> COOLANT_BLOCK
                "hot_coolant" -> HOT_COOLANT_BLOCK
                "uu_matter" -> UU_MATTER_BLOCK
                "weed_ex" -> WEED_EX_BLOCK
                "pahoehoe_lava" -> PAHOEHOE_LAVA_BLOCK
                "biofuel" -> BIOFUEL_BLOCK
                "biomass" -> BIOMASS_BLOCK
                "distilled_water" -> DISTILLED_WATER_BLOCK
                "construction_foam" -> CONSTRUCTION_FOAM_BLOCK
                "creosote" -> CREOSOTE_BLOCK
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
            override fun getBucket(): Item = when (name) {
                "coolant" -> COOLANT_BUCKET
                "hot_coolant" -> HOT_COOLANT_BUCKET
                "uu_matter" -> UU_MATTER_BUCKET
                "weed_ex" -> WEED_EX_BUCKET
                "pahoehoe_lava" -> PAHOEHOE_LAVA_BUCKET
                "biofuel" -> BIOFUEL_BUCKET
                "biomass" -> BIOMASS_BUCKET
                "distilled_water" -> DISTILLED_WATER_BUCKET
                "construction_foam" -> CONSTRUCTION_FOAM_BUCKET
                "creosote" -> CREOSOTE_BUCKET
                else -> throw IllegalStateException("Unknown fluid: $name")
            }
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
    private open class Ic2BucketItem(
        internal val bucketFluid: FlowableFluid, // 改名为 bucketFluid 避免与父类冲突
        private val placeFluidOverride: FlowableFluid? = null,
        settings: FabricItemSettings
    ) : BucketItem(bucketFluid, settings.recipeRemainder(Items.BUCKET).maxCount(1)) {

        /** 实际放置到世界的流体（用于蒸馏水等特殊情况） */
        private val fluidToPlace: FlowableFluid
            get() = placeFluidOverride ?: bucketFluid

        @Environment(EnvType.CLIENT)
        override fun appendTooltip(
            stack: ItemStack,
            world: World?,
            tooltip: MutableList<Text>,
            context: TooltipContext
        ) {
            super.appendTooltip(stack, world, tooltip, context)
            if (placeFluidOverride != null) {
                tooltip.add(Text.translatable("tooltip.ic2_120.distilled_water_places_water").formatted(Formatting.GRAY))
            }
        }

        override fun placeFluid(
            player: net.minecraft.entity.player.PlayerEntity?,
            world: World,
            pos: BlockPos,
            hitResult: BlockHitResult?
        ): Boolean {
            val actualFluid = fluidToPlace
            val state = world.getBlockState(pos)
            val block = state.block

            // FluidFillable：如炼药锅等可注入液体的方块
            if (block is FluidFillable) {
                if (block.canFillWithFluid(world, pos, state, actualFluid)) {
                    block.tryFillWithFluid(world, pos, state, actualFluid.defaultState)
                    actualFluid.getBucketFillSound().ifPresent { world.playSound(player, pos, it, SoundCategory.BLOCKS, 1f, 1f) }
                    world.emitGameEvent(player, GameEvent.FLUID_PLACE, pos)
                    return true
                }
            }

            // 普通方块：在点击位置放置流体（需可替换，如空气、流体本身）
            if (state.isReplaceable || state.fluidState.isStill) {
                if (world.setBlockState(pos, actualFluid.defaultState.blockState)) {
                    actualFluid.getBucketFillSound().ifPresent { world.playSound(player, pos, it, SoundCategory.BLOCKS, 1f, 1f) }
                    world.emitGameEvent(player, GameEvent.FLUID_PLACE, pos)
                    return true
                }
            }

            // 固体方块：在点击面朝向的相邻格放置
            val adjacentPos = hitResult?.side?.let { pos.offset(it) } ?: pos.up()
            val adjacentState = world.getBlockState(adjacentPos)
            if (adjacentState.isReplaceable || adjacentState.fluidState.isStill) {
                if (world.setBlockState(adjacentPos, actualFluid.defaultState.blockState)) {
                    actualFluid.getBucketFillSound().ifPresent { world.playSound(player, adjacentPos, it, SoundCategory.BLOCKS, 1f, 1f) }
                    world.emitGameEvent(player, GameEvent.FLUID_PLACE, adjacentPos)
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
        private val bucketFluid: FlowableFluid
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
}
