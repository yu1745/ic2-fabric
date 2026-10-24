package ic2_120_industrial_upgrade.content.block

import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.machines.MachineBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.OverclockerUpgrade
import ic2_120.content.item.isFluidCellEmpty
import ic2_120.content.item.setFluidCellVariant
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.IRedstoneInverterUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.content.upgrade.RedstoneInverterUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120_industrial_upgrade.content.fluid.NeutronFluid
import ic2_120_industrial_upgrade.content.screen.NeutronFabricatorScreenHandler
import ic2_120_industrial_upgrade.content.sync.NeutronFabricatorSync
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

@ModBlockEntity(block = NeutronFabricatorBlock::class)
class NeutronFabricatorBlockEntity(pos: BlockPos, state: BlockState) :
    MachineBlockEntity(NeutronFabricatorBlockEntity::class.type(), pos, state),
    ITieredMachine, ExtendedScreenHandlerFactory, Inventory, IRoutedSidedInventory,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport,
    IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, IRedstoneInverterUpgradeSupport {

    companion object {
        const val TIER = 11
        /** 充满一槽所需能量（每产出 1 mB 中子流体消耗）。对齐 1.4.0 原版 15,625,000 EU/mB */
        const val ENERGY_PER_MB: Long = 15_625_000L
        const val TANK_CAPACITY_DROPLETS = FluidConstants.BUCKET * 10L // 10 桶

        // 槽位索引（对齐物质生成机：升级槽 0-3 + 容器输入 4 + 容器输出 5）
        const val SLOT_UPGRADE_0 = 0
        const val SLOT_UPGRADE_1 = 1
        const val SLOT_UPGRADE_2 = 2
        const val SLOT_UPGRADE_3 = 3
        const val SLOT_CONTAINER_INPUT = 4
        const val SLOT_CONTAINER_OUTPUT = 5
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_CONTAINER_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_CONTAINER_INPUT)
        const val INVENTORY_SIZE = 6

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            FluidStorage.SIDED.registerForBlockEntity(
                { be, side -> (be as NeutronFabricatorBlockEntity).getFluidStorageForSide(side) },
                NeutronFabricatorBlockEntity::class.type()
            )
            fluidLookupRegistered = true
        }
    }

    override val tier: Int = TIER
    override val activeProperty = NeutronFabricatorBlock.ACTIVE

    // ====== 升级接口实现（对齐物质生成机）======
    override var capacityBonus: Long = 0L
    override var redstoneInverted: Boolean = false
    override var voltageTierBonus: Int = 0

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    @Suppress("unused")
    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = NeutronFabricatorSync(
        schema = syncedData,
        capacity = ENERGY_PER_MB,
        tier = TIER,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time },
        capacityBonusProvider = { capacityBonus },
        maxInsertPerTickProvider = { TransformerUpgradeComponent.maxInsertForTier(TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    /** ic2 通用流体单元（万能单元，可装任意流体） */
    private val emptyCellItem by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "empty_cell")) }
    private val fluidCellItem by lazy { Registries.ITEM.get(Identifier.of("ic2_120", "fluid_cell")) }

    /** 中子流体槽（内部产出写入，外部只能取出） */
    private val tankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = TANK_CAPACITY_DROPLETS
        // 外部不能灌入（玩家/管道只能抽取产出的中子流体）
        override fun canInsert(variant: FluidVariant): Boolean = false
        override fun canExtract(variant: FluidVariant): Boolean = !variant.isBlank
        override fun onFinalCommit() { markDirty() }

        fun getStoredAmount(): Long = amount
        fun getTankCapacity(): Long = TANK_CAPACITY_DROPLETS
        fun availableSpace(): Long = (TANK_CAPACITY_DROPLETS - amount).coerceAtLeast(0L)

        /** 机器内部产出写入（绕过 canInsert 限制） */
        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val actual = minOf(toInsert, availableSpace())
            if (actual <= 0L) return 0L
            val tx = Transaction.openOuter()
            try {
                updateSnapshots(tx)
                amount += actual
                if (variant.isBlank) variant = FluidVariant.of(NeutronFluid.NEUTRON_STILL)
                tx.commit()
            } finally {
                tx.close()
            }
            sync.fluidAmount = amount.toInt().coerceIn(0, Int.MAX_VALUE)
            markDirty()
            return actual
        }

        /** 内部抽取（用于灌入容器，绕过外部 extract 的事务） */
        fun extractInternal(toExtract: Long): Long {
            if (toExtract <= 0L || amount <= 0L) return 0L
            val actual = minOf(toExtract, amount)
            val tx = Transaction.openOuter()
            try {
                updateSnapshots(tx)
                amount -= actual
                if (amount <= 0L) variant = FluidVariant.blank()
                tx.commit()
            } finally {
                tx.close()
            }
            sync.fluidAmount = amount.toInt().coerceIn(0, Int.MAX_VALUE)
            markDirty()
            return actual
        }
    }

    /** 对外暴露的流体存储（只读抽取，供管道/泵抽出中子流体；正面朝向不接受） */
    private val outputStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = false
        override fun supportsExtraction(): Boolean = true
        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (!isNeutron(resource.fluid)) return 0L
            return tankInternal.extract(FluidVariant.of(NeutronFluid.NEUTRON_STILL), maxAmount, transaction)
        }

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            if (tankInternal.amount <= 0L || tankInternal.variant.isBlank) {
                return mutableListOf<StorageView<FluidVariant>>().iterator()
            }
            return mutableListOf(object : StorageView<FluidVariant> {
                override fun getResource(): FluidVariant = tankInternal.variant
                override fun getAmount(): Long = tankInternal.amount
                override fun getCapacity(): Long = tankInternal.getTankCapacity()
                override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
                    StoragePreconditions.notBlankNotNegative(resource, maxAmount)
                    if (!isNeutron(resource.fluid)) return 0L
                    return tankInternal.extract(FluidVariant.of(NeutronFluid.NEUTRON_STILL), maxAmount, transaction)
                }
                override fun isResourceBlank(): Boolean = false
            }).iterator()
        }
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        val facing = world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
        // 正面（玩家面对的面）不暴露流体存储，避免与 GUI 容器槽冲突
        if (side == facing) return null
        return outputStorage
    }

    // ====== Inventory ======
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    /**
     * 对外物品自动化通道（对齐 core 复制机）：AE2 输出总线等 Transfer API 消费方经
     * ItemStorage.SIDED lookup 访问；漏斗等 vanilla 消费方经 IRoutedSidedInventory 访问。
     * 容器输入槽 maxPerSlot=1（逐个装填，与复制机一致）；升级槽沿用 isValid 过滤（排除超频）。
     */
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_CONTAINER_INPUT), matcher = { isValid(SLOT_CONTAINER_INPUT, it) }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_CONTAINER_INPUT, SLOT_CONTAINER_OUTPUT),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) { inventory[slot] = stack; markDirty() }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    /** 槽位校验：升级槽接受已注册且本机实现的升级（不含超频）；容器输入槽接受空桶/空单元 */
    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        in SLOT_UPGRADE_INDICES -> stack.item is IUpgradeItem && stack.item !is OverclockerUpgrade
        SLOT_CONTAINER_INPUT -> isFillableContainer(stack)
        else -> false
    }

    /** 判断物品是否可用于盛装中子流体（空桶或空流体单元） */
    private fun isFillableContainer(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val item = stack.item
        return item == Items.BUCKET ||
            item == emptyCellItem ||
            (item == fluidCellItem && stack.isFluidCellEmpty())
    }

    private fun isNeutron(fluid: net.minecraft.fluid.Fluid): Boolean =
        fluid == NeutronFluid.NEUTRON_STILL || fluid == NeutronFluid.NEUTRON_FLOWING

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.canCombine(current, toInsert) && current.count < current.maxCount)
    }

    // ====== tick 逻辑：应用升级 → 产中子流体；容器槽自动装填 ======
    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        // 应用升级效果（对齐物质生成机）：储能/高压/红石反转/流体管道/物品弹出抽入
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        RedstoneInverterUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(
                world, pos, tankInternal,
                fluidPipeProviderFilter, fluidPipeProviderSides,
                upgradeCount = fluidPipeEjectorCount
            )
        }
        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(
                world, pos, tankInternal,
                fluidPipeReceiverFilter, fluidPipeReceiverSides,
                upgradeCount = fluidPipePullingCount
            )
        }
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        adjacentEnergyTransfer.tick()

        // 容器槽自动装填：把产出的中子流体灌入空桶/空单元（不受红石门控，与物质生成机一致）
        fillContainersFromTank()

        var active = false
        // 红石门控 + 能量达到阈值且流体槽未满 → 消耗能量产流体（1 mB = BUCKET/1000 droplets）
        val canProduce = RedstoneControlComponent.canRun(world, pos, this) &&
            sync.amount >= ENERGY_PER_MB && tankInternal.amount < TANK_CAPACITY_DROPLETS
        if (canProduce) {
            val produced = tankInternal.insertInternal(FluidConstants.BUCKET / 1000L) // 1 mB
            if (produced > 0L) {
                sync.consumeEnergy(ENERGY_PER_MB)
                active = true
            }
        }

        // 同步 GUI 数据
        sync.fluidAmount = tankInternal.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        // 进度仅在允许运行时展示充能百分比：红石关断/流体槽满时归零，
        // 避免“机器不工作却显示 100%”（能量充能不受红石门控，会一直充到满）。
        sync.progress = if (canProduce ||
            (RedstoneControlComponent.canRun(world, pos, this) && tankInternal.amount < TANK_CAPACITY_DROPLETS)
        ) {
            ((sync.amount.toFloat() / ENERGY_PER_MB) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        setActiveState(world, pos, state, active)
        markDirtyThrottled()
    }

    /** 把流体槽中的中子流体灌入容器输入槽的物品，产物进入容器输出槽 */
    private fun fillContainersFromTank() {
        if (tankInternal.getStoredAmount() < FluidConstants.BUCKET) return // 至少 1 桶才能灌桶
        val input = getStack(SLOT_CONTAINER_INPUT)
        if (input.isEmpty) return

        // 空桶需要 1 桶流体；空单元也按 1 桶计（与 core 物质制造机一致）
        val result = when (input.item) {
            Items.BUCKET -> ItemStack(NeutronFluid.NEUTRON_BUCKET)
            emptyCellItem -> ItemStack(fluidCellItem).apply {
                setFluidCellVariant(FluidVariant.of(NeutronFluid.NEUTRON_STILL))
            }
            fluidCellItem -> {
                if (input.isFluidCellEmpty()) {
                    ItemStack(fluidCellItem).apply {
                        setFluidCellVariant(FluidVariant.of(NeutronFluid.NEUTRON_STILL))
                    }
                } else {
                    ItemStack.EMPTY
                }
            }
            else -> ItemStack.EMPTY
        }
        if (result.isEmpty) return

        val output = getStack(SLOT_CONTAINER_OUTPUT)
        if (!canMergeIntoSlot(output, result)) return

        val drained = tankInternal.extractInternal(FluidConstants.BUCKET)
        if (drained < FluidConstants.BUCKET) return

        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_CONTAINER_INPUT, ItemStack.EMPTY)
        if (output.isEmpty) setStack(SLOT_CONTAINER_OUTPUT, result.copy())
        else output.increment(result.count)
        markDirty()
    }

    // ====== ExtendedScreenHandlerFactory ======
    override fun getDisplayName(): Text = Text.translatable("block.ic2_120_industrial_upgrade.neutron_fabricator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        NeutronFabricatorScreenHandler(
            syncId, playerInventory,
            ScreenHandlerContext.create(world!!, pos),
            syncedData, this,
            INVENTORY_SIZE
        )

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    // ====== NBT ======
    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(NeutronFabricatorSync.NBT_ENERGY).coerceIn(0L, sync.capacity))
        syncedData.readNbt(nbt)
        readInventoryWithMigration(nbt)
        val tankAmount = nbt.getLong("TankAmount")
        tankInternal.amount = tankAmount.coerceIn(0L, TANK_CAPACITY_DROPLETS)
        if (tankInternal.amount > 0L) tankInternal.variant = FluidVariant.of(NeutronFluid.NEUTRON_STILL)
        redstoneInverted = if (nbt.contains("RedstoneInverted")) nbt.getBoolean("RedstoneInverted") else false
    }

    /**
     * 读档并迁移旧 5 槽布局（0.4：0-2 充电 + 3 容器输入 + 4 容器输出）→ 新 6 槽布局
     * （0-3 升级 + 4 容器输入 + 5 容器输出）：旧槽 3/4 顺移为 4/5；旧充电槽 0-2 保留原位
     * （新布局下变成升级槽，若存有电池由玩家自行取出）。
     */
    private fun readInventoryWithMigration(nbt: NbtCompound) {
        val invNbt = nbt.getCompound("Inv")
        val oldSize = if (invNbt.contains("Size")) invNbt.getInt("Size") else INVENTORY_SIZE
        val items = invNbt.getList("Items", NbtElement.COMPOUND_TYPE.toInt())
        for (i in 0 until items.size) {
            val slotNbt = items.getCompound(i)
            val oldSlot = slotNbt.getInt("Slot")
            val newSlot = if (oldSize <= 5 && oldSlot >= SLOT_CONTAINER_INPUT) oldSlot + 1 else oldSlot
            if (newSlot !in inventory.indices) continue
            inventory[newSlot] = ItemStack.fromNbt(slotNbt)
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putLong(NeutronFabricatorSync.NBT_ENERGY, sync.amount)
        syncedData.writeNbt(nbt)
        nbt.put("Inv", Inventories.writeNbt(NbtCompound(), inventory))
        nbt.putLong("TankAmount", tankInternal.amount)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)
    }
}
