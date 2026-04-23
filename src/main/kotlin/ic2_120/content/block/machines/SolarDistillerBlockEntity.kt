package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.SolarDistillerBlock
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.WaterCell
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.isWaterFuel
import ic2_120.content.item.setFluidCellVariant
import ic2_120.content.screen.SolarDistillerScreenHandler
import ic2_120.content.sync.SolarDistillerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.fluid.Fluid
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

/**
 * 太阳能蒸馏机方块实体
 *
 * 功能：在白天（有阳光直射）时将普通水蒸馏成蒸馏水
 * - 需要 10 桶水输入 → 产出 10 桶蒸馏水
 * - 每 2.5 秒（50 tick）处理 1 桶水
 * - 仅在主世界、白天、无雨、顶部无遮挡时工作
 *
 * 流体交互：
 * - 输入：接收普通水（支持流体管道/容器）
 * - 输出：提供蒸馏水（支持流体管道/容器）
 * - 正面（朝向面）禁用流体交互，其余面开放
 *
 * 升级支持：
 * - 流体排出升级：作为 provider 向管道输出蒸馏水
 * - 流体抽取升级：作为 receiver 从管道接收普通水
 */
@ModBlockEntity(block = SolarDistillerBlock::class)
class SolarDistillerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {
    // 流体管道升级支持属性（IFluidPipeUpgradeSupport 接口实现）
    override var fluidPipeProviderEnabled: Boolean = false  // 是否作为 provider 向管道输出流体
    override var fluidPipeReceiverEnabled: Boolean = false  // 是否作为 receiver 从管道接收流体
    override var fluidPipeProviderFilter: Fluid? = null     // provider 流体过滤器（null = 不过滤）
    override var fluidPipeReceiverFilter: Fluid? = null    // receiver 流体过滤器（null = 不过滤）
    override var fluidPipeProviderSide: Direction? = null   // provider 工作面（null = 任意面）
    override var fluidPipeReceiverSide: Direction? = null   // receiver 工作面（null = 任意面）

    companion object {
        // 物品槽位索引定义
        const val SLOT_INPUT_WATER = 0       // 水输入槽（水桶、水电池等）
        const val SLOT_OUTPUT_EMPTY = 1      // 空容器输出槽（空桶、空电池等）
        const val SLOT_INPUT_CELL = 2        // 电池输入槽（空电池）
        const val SLOT_OUTPUT_CELL = 3       // 电池输出槽（蒸馏水电池）
        const val SLOT_UPGRADE_0 = 4         // 升级槽 0
        const val SLOT_UPGRADE_1 = 5         // 升级槽 1
        const val SLOT_UPGRADE_2 = 6         // 升级槽 2
        const val SLOT_UPGRADE_3 = 7         // 升级槽 3
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_EMPTY, SLOT_OUTPUT_CELL)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT_WATER, SLOT_INPUT_CELL)
        const val INVENTORY_SIZE = 8

        // NBT 存储键
        private const val NBT_INPUT_TANK = "InputTank"    // 输入罐存储键
        private const val NBT_OUTPUT_TANK = "OutputTank"  // 输出罐存储键

        // 工作时间限制（游戏内时间，0-24000）
        private const val DAY_START_TICK = 333   // 白天开始时间（约 6:55 AM）
        private const val DAY_END_TICK = 11750   // 白天结束时间（约 6:10 PM）

        @Volatile
        private var fluidLookupRegistered = false

        /**
         * 注册流体存储查找器
         * 通过 Fabric Transfer 的 API Lookup 系统，使外部可以访问本机器的流体能力
         */
        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = SolarDistillerBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        }
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_WATER), matcher = { isWaterInputStack(it) }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT_CELL), matcher = { isInputCellStack(it) })
        ),
        extractSlots = intArrayOf(SLOT_INPUT_WATER, SLOT_OUTPUT_EMPTY, SLOT_INPUT_CELL, SLOT_OUTPUT_CELL),
        markDirty = { markDirty() }
    )
    val syncedData = SyncedData(this)
    val sync = SolarDistillerSync(syncedData)

    /**
     * 输入流体储罐（水）
     * - 容量：10 桶
     * - 只能插入水（WATER/FLOWING_WATER）
     * - 不支持外部提取（只能被机器内部消耗）
     */
    private val inputTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 10  // 10 桶容量

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity

        // 只允许插入水
        override fun canInsert(variant: FluidVariant): Boolean =
            variant.fluid == Fluids.WATER || variant.fluid == Fluids.FLOWING_WATER

        // 不允许外部提取（只能被机器内部消耗）
        override fun canExtract(variant: FluidVariant): Boolean = false

        // 提交后同步数据到客户端
        override fun onFinalCommit() {
            sync.waterInputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            markDirty()
        }

        // 直接设置存储的水量（用于 NBT 读取）
        fun setStoredWater(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(Fluids.WATER) else FluidVariant.blank()
            sync.waterInputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity

        // 尝试插入水（直接修改，不使用事务）
        fun tryInsertWater(toInsert: Long): Long {
            if (toInsert <= 0L || (amount > 0L && variant.fluid != Fluids.WATER)) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.fluid != Fluids.WATER) variant = FluidVariant.of(Fluids.WATER)
            sync.waterInputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }

        // 消耗内部水（用于蒸馏过程）
        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.fluid != Fluids.WATER) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.waterInputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }
    }

    /**
     * 输出流体储罐（蒸馏水）
     * - 容量：10 桶
     * - 不支持外部插入（只能由机器内部产出）
     * - 支持提取蒸馏水（DISTILLED_WATER_STILL/FLOWING）
     */
    private val outputTankInternal = object : SingleVariantStorage<FluidVariant>() {
        private val tankCapacity: Long = FluidConstants.BUCKET * 10  // 10 桶容量

        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = tankCapacity

        // 不允许外部插入（只能由机器内部产出）
        override fun canInsert(variant: FluidVariant): Boolean = false

        // 允许提取蒸馏水
        override fun canExtract(variant: FluidVariant): Boolean =
            variant.fluid == ModFluids.DISTILLED_WATER_STILL || variant.fluid == ModFluids.DISTILLED_WATER_FLOWING

        // 提交后同步数据到客户端
        override fun onFinalCommit() {
            sync.distilledOutputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            markDirty()
        }

        // 直接设置存储的蒸馏水量（用于 NBT 读取）
        fun setStoredDistilled(newAmount: Long) {
            amount = newAmount.coerceIn(0L, tankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.DISTILLED_WATER_STILL) else FluidVariant.blank()
            sync.distilledOutputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
        }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = tankCapacity

        // 插入蒸馏水（用于蒸馏过程）
        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            if (amount > 0L && variant.fluid != ModFluids.DISTILLED_WATER_STILL) return 0L
            val space = tankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.DISTILLED_WATER_STILL)
            sync.distilledOutputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }

        // 消耗内部蒸馏水（用于填充电池等）
        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.distilledOutputMb = (amount * 1000L / FluidConstants.BUCKET).toInt().coerceAtLeast(0)
            return actual
        }
    }

    /**
     * 外部流体存储接口
     * 统一处理输入/输出流体的外部访问（管道、流体容器等）
     * - 输入：统一转换为 WATER 变体存储到输入罐
     * - 输出：统一使用 STILL 变体暴露输出罐内容
     */
    private val ioStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        // 外部输入只接收普通水（输入罐统一规范为 WATER 变体，避免出现 FLOWING/WATER 混态）。
        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            return if (resource.fluid == Fluids.WATER || resource.fluid == Fluids.FLOWING_WATER) {
                inputTankInternal.insert(FluidVariant.of(Fluids.WATER), maxAmount, transaction)
            } else {
                0L
            }
        }

        // 外部输出只提供蒸馏水（对外暴露 still/flowing 均视为同一可提取资源）。
        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (resource.fluid != ModFluids.DISTILLED_WATER_STILL && resource.fluid != ModFluids.DISTILLED_WATER_FLOWING) return 0L
            return outputTankInternal.extract(FluidVariant.of(ModFluids.DISTILLED_WATER_STILL), maxAmount, transaction)
        }

        // 提供储罐视图（用于外部查询储罐内容，如 JEI、流体管道等）
        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            val views = mutableListOf<StorageView<FluidVariant>>()
            if (!inputTankInternal.variant.isBlank && inputTankInternal.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = inputTankInternal.variant
                    override fun getAmount(): Long = inputTankInternal.amount
                    override fun getCapacity(): Long = inputTankInternal.getTankCapacity()
                    override fun extract(
                        resource: FluidVariant,
                        maxAmount: Long,
                        transaction: TransactionContext
                    ): Long = 0L

                    override fun isResourceBlank(): Boolean = false
                })
            }
            if (!outputTankInternal.variant.isBlank && outputTankInternal.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = outputTankInternal.variant
                    override fun getAmount(): Long = outputTankInternal.amount
                    override fun getCapacity(): Long = outputTankInternal.getTankCapacity()
                    override fun extract(
                        resource: FluidVariant,
                        maxAmount: Long,
                        transaction: TransactionContext
                    ): Long =
                        outputTankInternal.extract(resource, maxAmount, transaction)

                    override fun isResourceBlank(): Boolean = false
                })
            }
            return views.iterator()
        }
    }

    constructor(pos: BlockPos, state: BlockState) : this(
        SolarDistillerBlockEntity::class.type(),
        pos,
        state
    )

    // Inventory 接口实现
    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() {
        super.markDirty()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT_WATER -> isWaterInputStack(stack)
        SLOT_OUTPUT_EMPTY -> false
        SLOT_INPUT_CELL -> isInputCellStack(stack)
        SLOT_OUTPUT_CELL -> false
        else -> SLOT_UPGRADE_INDICES.contains(slot) && stack.item is IUpgradeItem
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    // ExtendedScreenHandlerFactory 接口实现（用于打开 GUI）
    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.solar_distiller")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        SolarDistillerScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    // NBT 数据读写
    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        inputTankInternal.setStoredWater(nbt.getLong(NBT_INPUT_TANK))
        outputTankInternal.setStoredDistilled(nbt.getLong(NBT_OUTPUT_TANK))
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_INPUT_TANK, inputTankInternal.getStoredAmount())
        nbt.putLong(NBT_OUTPUT_TANK, outputTankInternal.getStoredAmount())
    }

    // 工具方法：毫巴转换为滴（1 桶 = 1000 毫巴）
    private fun mbToDroppers(mb: Int): Long = mb * FluidConstants.BUCKET / 1000

    /**
     * 检查当前是否可以工作
     * 条件：主世界 + 非雨天 + 白天 + 顶部无遮挡
     */
    private fun canWorkNow(world: World, pos: BlockPos): Boolean {
        if (world.registryKey != World.OVERWORLD) return false  // 只在主世界工作
        if (world.isRaining) return false  // 雨天不工作
        val time = world.timeOfDay % 24000
        if (time < DAY_START_TICK || time > DAY_END_TICK) return false  // 只在白天工作

        // 检查顶部是否有遮挡物
        val topY = world.topY
        var y = pos.y + 1
        while (y < topY) {
            val scanPos = BlockPos(pos.x, y, pos.z)
            val blockState = world.getBlockState(scanPos)
            if (blockState.isOpaqueFullCube(world, scanPos)) return false
            y++
        }
        return true
    }

    /**
     * 每 tick 更新逻辑
     * 1. 同步升级驱动的流体管道参与状态
     * 2. 处理物品槽（水输入、蒸馏水电池输出）
     * 3. 检查工作条件并执行蒸馏过程
     * 4. 更新方块激活状态
     */
    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return  // 客户端不执行逻辑

        // 每 tick 重新从升级槽同步”是否参与流体管网 + 过滤条件”
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, outputTankInternal, fluidPipeProviderFilter, fluidPipeProviderSide)
        }
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        // 机器物品槽处理放在产线逻辑前，确保本 tick 新放入的容器可立即参与计算
        handleWaterInputSlot()
        handleDistilledCellFill()

        // 检查是否可以工作（条件满足 + 有足够的水 + 输出罐未满）
        val canWork = canWorkNow(world, pos) &&
                inputTankInternal.getStoredAmount() >= mbToDroppers(SolarDistillerSync.PRODUCE_MB_PER_CYCLE) &&
                outputTankInternal.getStoredAmount() < outputTankInternal.getTankCapacity()
        sync.isWorking = if (canWork) 1 else 0

        if (canWork) {
            // 工作中：增加进度
            sync.progress += 1
            if (sync.progress >= SolarDistillerSync.PRODUCE_INTERVAL_TICKS) {
                // 达到处理间隔：执行蒸馏
                val produceDroppers = mbToDroppers(SolarDistillerSync.PRODUCE_MB_PER_CYCLE)
                val consumed = inputTankInternal.consumeInternal(produceDroppers)
                if (consumed >= produceDroppers) {
                    outputTankInternal.insertInternal(produceDroppers)
                }
                sync.progress = 0
            }
        } else if (sync.progress != 0) {
            // 不工作：重置进度
            sync.progress = 0
        }

        // 更新方块激活状态（用于渲染）
        val active = sync.isWorking != 0
        if (state.get(SolarDistillerBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(SolarDistillerBlock.ACTIVE, active))
        }
    }

    /**
     * 处理水输入槽
     * 支持的输入：水桶、水电池
     * 输出：空桶、空电池
     */
    private fun handleWaterInputSlot() {
        val input = getStack(SLOT_INPUT_WATER)
        if (input.isEmpty) return

        val emptyOutput = getStack(SLOT_OUTPUT_EMPTY)
        val emptyCellItem = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell"))

        // 确定输入容器类型和对应的空容器
        val emptyContainer = when {
            input.item == Items.WATER_BUCKET -> ItemStack(Items.BUCKET)
            input.item is WaterCell -> ItemStack(emptyCellItem)
            input.item is FluidCellItem && input.isWaterFuel() -> ItemStack(emptyCellItem)
            else -> ItemStack.EMPTY
        }
        if (emptyContainer.isEmpty) return  // 不支持的输入类型

        // 检查输入罐是否有空间（1 桶）
        if (inputTankInternal.getTankCapacity() - inputTankInternal.getStoredAmount() < FluidConstants.BUCKET) return

        // 检查输出槽是否有空间
        val canAcceptEmpty = emptyOutput.isEmpty ||
                (ItemStack.areItemsEqual(emptyOutput, emptyContainer) && emptyOutput.count < emptyOutput.maxCount)
        if (!canAcceptEmpty) return

        // 尝试插入水到输入罐
        if (inputTankInternal.tryInsertWater(FluidConstants.BUCKET) < FluidConstants.BUCKET) return

        // 消耗输入，输出空容器
        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_INPUT_WATER, ItemStack.EMPTY)
        if (emptyOutput.isEmpty) {
            setStack(SLOT_OUTPUT_EMPTY, emptyContainer)
        } else {
            emptyOutput.increment(1)
        }
        markDirty()
    }

    /**
     * 处理蒸馏水电池填充
     * 输入：空电池
     * 输出：蒸馏水电池
     */
    private fun handleDistilledCellFill() {
        if (outputTankInternal.getStoredAmount() < FluidConstants.BUCKET) return  // 输出罐至少需要 1 桶蒸馏水
        val input = getStack(SLOT_INPUT_CELL)
        if (input.isEmpty) return

        val distilledCell = ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "distilled_water_cell")))

        // 确定输出电池类型
        val outputCell = when {
            input.item == Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")) -> distilledCell
            input.item is FluidCellItem && input.isFluidCellEmpty() -> {
                ItemStack(input.item).apply { setFluidCellVariant(FluidVariant.of(ModFluids.DISTILLED_WATER_STILL)) }
            }

            else -> ItemStack.EMPTY
        }
        if (outputCell.isEmpty) return  // 不支持的输入类型

        // 检查输出槽是否有空间
        val output = getStack(SLOT_OUTPUT_CELL)
        val canAccept = output.isEmpty ||
                (ItemStack.canCombine(output, outputCell) && output.count < output.maxCount)
        if (!canAccept) return

        // 消耗蒸馏水，填充电池
        if (outputTankInternal.consumeInternal(FluidConstants.BUCKET) < FluidConstants.BUCKET) return

        // 消耗输入，输出电池
        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_INPUT_CELL, ItemStack.EMPTY)
        if (output.isEmpty) setStack(SLOT_OUTPUT_CELL, outputCell)
        else output.increment(1)
        markDirty()
    }

    /**
     * 获取指定方向的流体存储
     * 正面（朝向面）禁用流体口，保留为机器交互面
     * 其余面均开放管道/容器访问
     */
    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        // 正面（水平朝向面）禁用流体口，保留为机器交互面；其余面均开放管道/容器访问。
        // if (side == (world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH)) return null
        return ioStorage
    }

    private fun isWaterInputStack(stack: ItemStack): Boolean =
        !stack.isEmpty && (stack.item == Items.WATER_BUCKET || stack.item is WaterCell ||
            (stack.item is FluidCellItem && stack.isWaterFuel()))

    private fun isInputCellStack(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val emptyCell = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell"))
        return stack.item == emptyCell || (stack.item is FluidCellItem && stack.isFluidCellEmpty())
    }
}
