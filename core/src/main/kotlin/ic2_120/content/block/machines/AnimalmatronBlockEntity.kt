package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.AnimalmatronBlock
import ic2_120.content.entity.AnimalFoodMapping
import ic2_120.content.entity.AnimalGrowthData
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.screen.AnimalmatronScreenHandler
import ic2_120.content.sync.AnimalmatronSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.minecraft.block.BlockState
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.world.World

@ModBlockEntity(block = AnimalmatronBlock::class)
class AnimalmatronBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state),
    Inventory,
    IRoutedSidedInventory,
    IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport,
    IFluidPipeUpgradeSupport,
    IEjectorUpgradeSupport,
    ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = AnimalmatronBlock.ACTIVE
    override val tier: Int = ANIMALMATRON_TIER

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 0
    override var fluidPipePullingCount: Int = 0

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_SHEARS), matcher = { !it.isEmpty && it.item == Items.SHEARS }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_WATER_INPUT), matcher = { isValid(SLOT_WATER_INPUT, it) }),
            ItemInsertRoute(intArrayOf(SLOT_WEED_EX_INPUT), matcher = { isValid(SLOT_WEED_EX_INPUT, it) }),
            ItemInsertRoute(SLOT_FEED_INDICES, matcher = { isValid(SLOT_FEED_0, it) })
        ),
        extractSlots = IntArray(INVENTORY_SIZE) { it },
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = AnimalmatronSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(ANIMALMATRON_TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    // 动物数据追踪
    private val animalDataMap = mutableMapOf<java.util.UUID, AnimalGrowthData>()

    private val waterTankCapacity = FluidConstants.BUCKET * AnimalmatronSync.WATER_TANK_CAPACITY_BUCKETS
    private val weedExTankCapacity = FluidConstants.BUCKET * AnimalmatronSync.WEED_EX_TANK_CAPACITY_BUCKETS

    private val waterTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = waterTankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = isWater(variant.fluid) && !variant.isBlank
        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun onFinalCommit() {
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
        }

        fun setStoredFluid(newAmount: Long) {
            amount = newAmount.coerceIn(0L, waterTankCapacity)
            variant = if (amount > 0L) FluidVariant.of(Fluids.WATER) else FluidVariant.blank()
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
        }

        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val space = waterTankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(Fluids.WATER)
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
            return actual
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.waterAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
            return actual
        }

        fun getStoredWater(): Long = amount
    }

    private val weedExTankInternal = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = weedExTankCapacity
        override fun canInsert(variant: FluidVariant): Boolean = isWeedEx(variant.fluid) && !variant.isBlank
        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun insert(insertedVariant: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            if (insertedVariant.isBlank) return 0L
            return super.insert(insertedVariant, maxAmount, transaction)
        }

        override fun onFinalCommit() {
            sync.weedExAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
        }

        fun setStoredFluid(newAmount: Long) {
            amount = newAmount.coerceIn(0L, weedExTankCapacity)
            variant = if (amount > 0L) FluidVariant.of(ModFluids.WEED_EX_STILL) else FluidVariant.blank()
            sync.weedExAmount = amount.toInt().coerceAtLeast(0)
        }

        fun insertInternal(toInsert: Long): Long {
            if (toInsert <= 0L) return 0L
            val space = weedExTankCapacity - amount
            val actual = minOf(toInsert, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.WEED_EX_STILL)
            sync.weedExAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
            return actual
        }

        fun consumeInternal(toConsume: Long): Long {
            if (toConsume <= 0L || variant.isBlank) return 0L
            val actual = minOf(toConsume, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.weedExAmount = amount.toInt().coerceAtLeast(0)
            markDirty()
            return actual
        }

        fun getStoredWeedEx(): Long = amount
    }

    private val ioStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (isWater(resource.fluid)) return waterTankInternal.insert(resource, maxAmount, transaction)
            if (isWeedEx(resource.fluid)) return weedExTankInternal.insert(resource, maxAmount, transaction)
            return 0L
        }

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            val views = mutableListOf<StorageView<FluidVariant>>()
            if (!waterTankInternal.variant.isBlank && waterTankInternal.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = waterTankInternal.variant
                    override fun getAmount(): Long = waterTankInternal.amount
                    override fun getCapacity(): Long = waterTankCapacity
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = false
                })
            }
            if (!weedExTankInternal.variant.isBlank && weedExTankInternal.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = weedExTankInternal.variant
                    override fun getAmount(): Long = weedExTankInternal.amount
                    override fun getCapacity(): Long = weedExTankCapacity
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = false
                })
            }
            return views.iterator()
        }
    }

    private var workOffset: Int = random.nextBetween(0, WORK_INTERVAL_TICKS - 1)

    private val emptyCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")) }
    private val fluidCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell")) }
    private val waterCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "water_cell")) }
    private val distilledWaterCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "distilled_water_cell")) }
    private val weedExCellItem by lazy { Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "weed_ex_cell")) }

    constructor(pos: BlockPos, state: BlockState) : this(AnimalmatronBlockEntity::class.type(), pos, state)

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_SHEARS && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    private fun matchesWaterInput(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val fluidCellId = Identifier(Ic2_120.MOD_ID, "fluid_cell")
        val waterCellId = Identifier(Ic2_120.MOD_ID, "water_cell")
        val distilledWaterCellId = Identifier(Ic2_120.MOD_ID, "distilled_water_cell")
        return when {
            stack.item == Items.WATER_BUCKET || stack.item == ModFluids.DISTILLED_WATER_BUCKET -> true
            Registries.ITEM.getId(stack.item) == waterCellId -> true
            Registries.ITEM.getId(stack.item) == distilledWaterCellId -> true
            Registries.ITEM.getId(stack.item) == fluidCellId && stack.item is FluidCellItem -> {
                val fluid = stack.getFluidCellVariant()?.fluid
                fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER ||
                    fluid == ModFluids.DISTILLED_WATER_STILL || fluid == ModFluids.DISTILLED_WATER_FLOWING
            }
            else -> false
        }
    }

    private fun matchesWeedExInput(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val fluidCellId = Identifier(Ic2_120.MOD_ID, "fluid_cell")
        val weedExCellId = Identifier(Ic2_120.MOD_ID, "weed_ex_cell")
        return when {
            stack.item == ModFluids.WEED_EX_BUCKET -> true
            Registries.ITEM.getId(stack.item) == weedExCellId -> true
            Registries.ITEM.getId(stack.item) == fluidCellId && stack.item is FluidCellItem -> {
                val fluid = stack.getFluidCellVariant()?.fluid
                fluid == ModFluids.WEED_EX_STILL || fluid == ModFluids.WEED_EX_FLOWING
            }
            else -> false
        }
    }

    private fun matchesFeedInput(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        // 检查是否是任何动物的食物
        val item = stack.item
        // 检查常见食物物品
        return item == Items.CARROT || item == Items.WHEAT || item == Items.WHEAT_SEEDS ||
            item == Items.DANDELION || item == Items.GOLDEN_APPLE || item == Items.GOLDEN_CARROT ||
            item == Items.HAY_BLOCK
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        slot == SLOT_WATER_OUTPUT || slot == SLOT_WEED_EX_OUTPUT || slot == SLOT_HARVEST_OUTPUT -> false
        slot == SLOT_WATER_INPUT -> matchesWaterInput(stack)
        slot == SLOT_WEED_EX_INPUT -> matchesWeedExInput(stack)
slot == SLOT_SHEARS -> stack.item == Items.SHEARS
        SLOT_FEED_INDICES.contains(slot) -> matchesFeedInput(stack)
        SLOT_UPGRADE_INDICES.contains(slot) -> stack.item is IUpgradeItem
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.animalmatron")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        AnimalmatronScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(AnimalmatronSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        waterTankInternal.setStoredFluid(nbt.getLong(NBT_WATER_MB).coerceIn(0, waterTankCapacity))
        weedExTankInternal.setStoredFluid(nbt.getLong(NBT_WEED_EX_MB).coerceIn(0, weedExTankCapacity))
        workOffset = nbt.getInt(NBT_WORK_OFFSET).coerceIn(0, WORK_INTERVAL_TICKS - 1)

        // 读取动物数据
        val animalDataList = nbt.getList(NBT_ANIMAL_DATA, 10) // 10 = NbtCompound type
        animalDataMap.clear()
        for (i in 0 until animalDataList.size) {
            val dataNbt = animalDataList.getCompound(i)
            val data = AnimalGrowthData.fromNbt(dataNbt)
            animalDataMap[data.uuid] = data
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(AnimalmatronSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_WATER_MB, waterTankInternal.amount)
        nbt.putLong(NBT_WEED_EX_MB, weedExTankInternal.amount)
        nbt.putInt(NBT_WORK_OFFSET, workOffset)

        // 写入动物数据
        val animalDataList = NbtList()
        animalDataMap.values.forEach { data ->
            animalDataList.add(data.toNbt())
        }
        nbt.put(NBT_ANIMAL_DATA, animalDataList)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_ITEM_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_ITEM_INPUT_INDICES)
        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, waterTankInternal, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }
        processWaterInputContainer()
        processWeedExInputContainer()

        // 有受监管动物时固定耗电 2 EU/t，不再随动物数量变化。
        val actualDrain = AnimalmatronSync.ENERGY_PER_TICK
        val canPower = if (actualDrain > 0L) {
            sync.animalCount <= 0 || sync.consumeEnergy(actualDrain) >= actualDrain
        } else {
            true
        }

        var active = false
        val interval = (WORK_INTERVAL_TICKS.toFloat() / speedMultiplier).toInt().coerceAtLeast(1)
        if ((world.time + workOffset) % interval.toLong() == 0L) {
            val report = if (canPower) runScan(world) else ScanReport()
            sync.animalCount = report.touched
            sync.touchedThisRun = report.touched
            sync.foodConsumedThisRun = report.foodConsumed
            sync.waterConsumedThisRun = report.waterConsumed
            sync.weedExConsumedThisRun = report.weedExConsumed
            sync.grewUpThisRun = report.grewUp
            sync.canBreedNowThisRun = report.canBreedNow
            sync.bredThisRun = report.bred
            active = report.touched > 0
        }

        sync.waterAmount = waterTankInternal.amount.toInt().coerceAtLeast(0)
        sync.weedExAmount = weedExTankInternal.amount.toInt().coerceAtLeast(0)
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun runScan(world: World): ScanReport {
        val report = ScanReport()

        // 获取范围内的动物实体（仅包含白名单中的动物）
        val box = Box(pos).expand(SCAN_RADIUS.toDouble())
        val animals = world.getEntitiesByClass(
            PassiveEntity::class.java,
            box
        ) { it.isAlive && AnimalFoodMapping.isManagedAnimal(it) }

        for (animal in animals) {

            report.touched++

            // 获取或创建动物数据
            val animalData = animalDataMap.computeIfAbsent(animal.uuid) {
                // 如果是成年动物（从外面牵来的），直接标记为已长成
                if (!animal.isBaby) {
                    // 检查原版繁殖冷却，防止通过抱出抱入绕过冷却
                    val hasCooldown = animal is AnimalEntity && animal.breedingAge > 0
                    AnimalGrowthData(animal.uuid, foodConsumed = FOOD_TO_GROW, canBreed = !hasCooldown)
                } else {
                    AnimalGrowthData(animal.uuid)
                }
            }

            // 尝试喂食
            tryFeedAnimal(animal, animalData, animals.size, report)

            // 尝试收获额外产物
            tryHarvestExtraProduct(animal, animalData, report)

            // 无水时缓慢伤害动物（不致死，保留 50% 血量）
            if (waterTankInternal.amount <= 0L && animal.health > animal.maxHealth * WATER_DAMAGE_HEALTH_FRACTION) {
                animal.health = (animal.health - WATER_DAMAGE_PER_TICK)
                    .coerceAtLeast(animal.maxHealth * WATER_DAMAGE_HEALTH_FRACTION)
                report.waterDamaged++
            }
        }

        if (world is ServerWorld) {
            tryBreedReadyAnimals(world, animals, report)
        }

        // 只清理已死亡的动物数据，活着的实体（包括离开范围的）保留状态
        if (world is ServerWorld) {
            animalDataMap.keys.removeAll { uuid ->
                val entity = world.getEntity(uuid)
                entity == null || !entity.isAlive
            }
        }

        return report
    }

    private fun tryFeedAnimal(
        animal: PassiveEntity,
        animalData: AnimalGrowthData,
        totalAnimals: Int,
        report: ScanReport
    ) {
        val foodItems = AnimalFoodMapping.getFoodForAnimal(animal)
        if (foodItems.isEmpty()) return

        val world = world ?: return
        val currentDay = (world.time / TICKS_PER_DAY).toInt()

        // 检查是否是新的一天，重置今日数据
        if (currentDay > animalData.currentDay) {
            animalData.foodToday = 0
            animalData.currentDay = currentDay
            animalData.insecticidePaidToday = false
        }

        // 检查今天是否已经喂够了5个
        if (animalData.foodToday >= FOOD_PER_DAY) return

        // 计算喂食间隔：12000 tick / 5 = 2400 tick = 2分钟喂1个
        val feedingInterval = TICKS_PER_DAY / FOOD_PER_DAY

        // 检查是否到了该喂食的时间
        val timeSinceLastFeed = world.time - animalData.lastFeedTick
        if (timeSinceLastFeed < feedingInterval) return

        // 喂食1个食物
        for (foodItem in foodItems) {
            if (tryConsumeFeed(foodItem)) {
                animalData.foodConsumed++
                animalData.foodToday++
                animalData.lastFeedTick = world.time
                report.foodConsumed++

                // 消耗水
                consumeWater(report)

                // 暂时停用除草剂的实际作用：允许灌入，但不消耗，也不因缺少除草剂产生负面效果。

                // 检查是否达到成长/繁殖条件
                checkGrowthAndBreeding(animal, animalData, totalAnimals, report)

                break
            }
        }
    }

    /**
     * 消耗水（加快生长速度——缩短喂食间隔）
     */
    private fun consumeWater(report: ScanReport) {
        val waterDroplets = waterTankInternal.getStoredWater().toInt()
        if (waterDroplets <= 0) return
        val toConsume = minOf(waterDroplets, WATER_PER_CARE)
        if (toConsume <= 0) return
        waterTankInternal.consumeInternal(mbToDroplets(toConsume))
        report.waterConsumed += toConsume
    }

    private fun checkGrowthAndBreeding(
        animal: PassiveEntity,
        animalData: AnimalGrowthData,
        totalAnimals: Int,
        report: ScanReport
    ) {
        // 检查是否达到10个食物
        if (animalData.foodConsumed >= FOOD_TO_GROW) {
            // 先让动物长大（如果还是幼崽）
            if (animal.isBaby) {
                animal.setBaby(false)
                report.grewUp++
            }

            // 检查是否可以繁殖（需要当日已支付杀虫剂）
            if (totalAnimals < MAX_ANIMALS_FOR_BREEDING && !animalData.canBreed) {
                if (animalData.insecticidePaidToday) {
                    animalData.canBreed = true
                    report.canBreedNow++
                }
                // 没有杀虫剂则无法繁殖，但动物仍可正常长大
            }
        }
    }

    private fun tryBreedReadyAnimals(
        world: ServerWorld,
        animals: List<PassiveEntity>,
        report: ScanReport
    ) {
        var animalCount = animals.size
        if (animalCount >= MAX_ANIMALS_FOR_BREEDING) return

        val readyAnimals = animals
            .filterIsInstance<AnimalEntity>()
            .filter { it.isAlive && !it.isBaby && animalDataMap[it.uuid]?.canBreed == true && it.breedingAge <= 0 }

        if (readyAnimals.size < 2) return

        val used = mutableSetOf<java.util.UUID>()

        for (animal in readyAnimals) {
            if (animalCount >= MAX_ANIMALS_FOR_BREEDING) break
            if (!used.add(animal.uuid)) continue

            val mate = readyAnimals.firstOrNull { other ->
                other.uuid != animal.uuid &&
                    other.uuid !in used &&
                    other.isAlive &&
                    !other.isBaby &&
                    other.type == animal.type
            } ?: run {
                used.remove(animal.uuid)
                continue
            }

            val animalData = animalDataMap[animal.uuid] ?: continue
            val mateData = animalDataMap[mate.uuid] ?: continue
            val child = animal.createChild(world, mate) ?: continue

            child.setBaby(true)
            child.refreshPositionAndAngles(animal.x, animal.y, animal.z, 0f, 0f)
            animal.breed(world, mate, child)
            world.spawnEntityAndPassengers(child)

            animalData.foodConsumed = 0
            animalData.canBreed = false
            mateData.foodConsumed = 0
            mateData.canBreed = false

            used.add(mate.uuid)
            animalCount++
            report.bred++
            markDirty()
        }
    }

    private fun processWaterInputContainer() {
        if (waterTankInternal.amount > waterTankCapacity - FluidConstants.BUCKET) return

        val input = getStack(SLOT_WATER_INPUT)
        if (input.isEmpty) return

        val emptyRemainder = when {
            input.item == Items.WATER_BUCKET || input.item == ModFluids.DISTILLED_WATER_BUCKET -> ItemStack(Items.BUCKET)
            input.item == waterCellItem || input.item == distilledWaterCellItem -> ItemStack(emptyCellItem)
            input.item == fluidCellItem && input.item is FluidCellItem -> {
                val fluid = input.getFluidCellVariant()?.fluid
                if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER ||
                    fluid == ModFluids.DISTILLED_WATER_STILL || fluid == ModFluids.DISTILLED_WATER_FLOWING
                ) ItemStack(emptyCellItem) else ItemStack.EMPTY
            }
            else -> ItemStack.EMPTY
        }
        if (emptyRemainder.isEmpty) return

        val out = getStack(SLOT_WATER_OUTPUT)
        if (!canMergeIntoSlot(out, emptyRemainder)) return

        waterTankInternal.insertInternal(FluidConstants.BUCKET)
        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_WATER_INPUT, ItemStack.EMPTY)
        if (out.isEmpty) setStack(SLOT_WATER_OUTPUT, emptyRemainder.copy()) else out.increment(1)
        markDirty()
    }

    private fun processWeedExInputContainer() {
        if (weedExTankInternal.amount > weedExTankCapacity - FluidConstants.BUCKET) return

        val input = getStack(SLOT_WEED_EX_INPUT)
        if (input.isEmpty) return

        val emptyRemainder = when {
            input.item == ModFluids.WEED_EX_BUCKET -> ItemStack(Items.BUCKET)
            input.item == weedExCellItem -> ItemStack(emptyCellItem)
            input.item == fluidCellItem && input.item is FluidCellItem -> {
                val fluid = input.getFluidCellVariant()?.fluid
                if (fluid == ModFluids.WEED_EX_STILL || fluid == ModFluids.WEED_EX_FLOWING) ItemStack(emptyCellItem)
                else ItemStack.EMPTY
            }
            else -> ItemStack.EMPTY
        }
        if (emptyRemainder.isEmpty) return

        val out = getStack(SLOT_WEED_EX_OUTPUT)
        if (!canMergeIntoSlot(out, emptyRemainder)) return

        weedExTankInternal.insertInternal(FluidConstants.BUCKET)
        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_WEED_EX_INPUT, ItemStack.EMPTY)
        if (out.isEmpty) setStack(SLOT_WEED_EX_OUTPUT, emptyRemainder.copy()) else out.increment(1)
        markDirty()
    }

    private fun tryConsumeFeed(foodItem: Item): Boolean {
        for (slot in SLOT_FEED_INDICES) {
            val stack = getStack(slot)
            if (!stack.isEmpty && stack.item == foodItem) {
                stack.decrement(1)
                if (stack.isEmpty) setStack(slot, ItemStack.EMPTY)
                markDirty()
                return true
            }
        }
        return false
    }

    /**
     * 获取动物的额外产物
     */
    private fun getExtraProduct(animal: PassiveEntity): ItemStack? {
        val entityId = Registries.ENTITY_TYPE.getId(animal.type).toString()
        return when (entityId) {
            "minecraft:chicken" -> ItemStack(Items.EGG, 1)
            "minecraft:sheep" -> {
                if (animal is SheepEntity && !animal.isSheared) {
                    val colorName = animal.color.asString()
                    val woolItem = Registries.ITEM.get(Identifier("minecraft", "${colorName}_wool"))
                    ItemStack(woolItem, 1)
                } else null
            }
            else -> null
        }
    }

    /**
     * 消耗剪刀耐久度
     */
    private fun tryConsumeShearsDurability(): Boolean {
        val shears = getStack(SLOT_SHEARS)
        if (shears.isEmpty || shears.item != Items.SHEARS) return false

        val currentDamage = shears.damage
        if (currentDamage >= shears.maxDamage - 1) {
            // 最后一次使用，剪刀损坏
            setStack(SLOT_SHEARS, ItemStack.EMPTY)
        } else {
            shears.damage = currentDamage + 1
        }
        markDirty()
        return true
    }

    /**
     * 尝试收获额外产物
     */
    private fun tryHarvestExtraProduct(animal: PassiveEntity, animalData: AnimalGrowthData, report: ScanReport) {
        val world = world ?: return

        // 检查收获间隔（羊依赖原版吃草恢复，用短间隔；其他产物 10 分钟）
        val interval = if (animal is SheepEntity) HARVEST_INTERVAL_TICKS else EXTRA_PRODUCT_INTERVAL_TICKS
        if (world.time - animalData.lastHarvestTick < interval) return

        val product = getExtraProduct(animal) ?: return

        // 鸡蛋不需要剪刀，其他产物需要消耗剪刀耐久
        val needsShears = product.item != Items.EGG
        if (needsShears && !tryConsumeShearsDurability()) return

        // 收获后处理（如标记羊已剪毛）
        if (animal is SheepEntity) {
            animal.isSheared = true
        }

        // 放入输出槽
        val out = getStack(SLOT_HARVEST_OUTPUT)
        if (!out.isEmpty) {
            if (!ItemStack.canCombine(out, product) || out.count >= out.maxCount) return
            out.increment(product.count)
        } else {
            setStack(SLOT_HARVEST_OUTPUT, product.copy())
        }

        animalData.lastHarvestTick = world.time
        report.harvested++
        markDirty()
    }

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.canCombine(current, toInsert) && current.count < current.maxCount)
    }

    private fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? {
        return ioStorage
    }

    private fun getFrontFacing(): Direction =
        world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH

    private fun isWater(fluid: net.minecraft.fluid.Fluid): Boolean =
        fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER ||
            fluid == ModFluids.DISTILLED_WATER_STILL || fluid == ModFluids.DISTILLED_WATER_FLOWING

    private fun isWeedEx(fluid: net.minecraft.fluid.Fluid): Boolean =
        fluid == ModFluids.WEED_EX_STILL || fluid == ModFluids.WEED_EX_FLOWING

    private fun mbToDroplets(mb: Int): Long = mb.toLong() * FluidConstants.BUCKET / 1000L

    /**
     * 获取指定动物的喂食进度数据（供 JADE 使用）
     * @return Pair(累计喂食数量, 今天已喂食数量) 或 null
     */
    fun getAnimalFeedProgress(uuid: java.util.UUID): Pair<Int, Int>? {
        val data = animalDataMap[uuid] ?: return null
        return Pair(data.foodConsumed, data.foodToday)
    }

    /**
     * 获取指定动物是否可以繁殖（供 JADE 使用）
     */
    fun isAnimalCanBreed(uuid: java.util.UUID): Boolean {
        val data = animalDataMap[uuid] ?: return false
        return data.canBreed
    }

    data class ScanReport(
        var touched: Int = 0,
        var foodConsumed: Int = 0,
        var waterConsumed: Int = 0,
        var weedExConsumed: Int = 0,
        var grewUp: Int = 0,
        var canBreedNow: Int = 0,
        var bred: Int = 0,
        var harvested: Int = 0,
        var waterDamaged: Int = 0
    )

    companion object {
        const val ANIMALMATRON_TIER = 1

        const val SLOT_WATER_INPUT = 0
        const val SLOT_WATER_OUTPUT = 1
        const val SLOT_WEED_EX_INPUT = 2
        const val SLOT_WEED_EX_OUTPUT = 3

        const val SLOT_FEED_0 = 4
        const val SLOT_FEED_1 = 5
        const val SLOT_FEED_2 = 6
        const val SLOT_FEED_3 = 7
        const val SLOT_FEED_4 = 8

        const val SLOT_UPGRADE_0 = 9
        const val SLOT_UPGRADE_1 = 10
        const val SLOT_UPGRADE_2 = 11
        const val SLOT_UPGRADE_3 = 12
        const val SLOT_SHEARS = 13
        const val SLOT_HARVEST_OUTPUT = 14

        val SLOT_FEED_INDICES = intArrayOf(
            SLOT_FEED_0,
            SLOT_FEED_1,
            SLOT_FEED_2,
            SLOT_FEED_3,
            SLOT_FEED_4
        )
        val SLOT_ITEM_INPUT_INDICES = intArrayOf(SLOT_SHEARS, SLOT_WATER_INPUT, SLOT_WEED_EX_INPUT, *SLOT_FEED_INDICES)
        val SLOT_ITEM_OUTPUT_INDICES = intArrayOf(SLOT_WATER_OUTPUT, SLOT_WEED_EX_OUTPUT, SLOT_HARVEST_OUTPUT)
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)

        const val INVENTORY_SIZE = 15

        private const val WORK_INTERVAL_TICKS = 20  // 每秒运行一次（20 tick = 1秒）
        private const val HARVEST_INTERVAL_TICKS = 2400L  // 每2分钟收获一次（羊）
        private const val EXTRA_PRODUCT_INTERVAL_TICKS = 12000L  // 每10分钟收获一次（其他动物）
        private const val SCAN_RADIUS = 4

        // 游戏机制常量
        const val FOOD_PER_DAY = 5
        const val FOOD_TO_GROW = 10
        const val FOOD_TO_BREED = 10
        const val MAX_ANIMALS_FOR_BREEDING = 32
        const val TICKS_PER_DAY = 12000L

        // 流体消耗
        const val WATER_PER_CARE = 100 // mB，每次喂食消耗
        const val INSECTICIDE_PER_DAY = 100 // mB，每只动物每天消耗
        const val WATER_DAMAGE_HEALTH_FRACTION = 0.5f // 无水时伤害下限（50%血量）
        const val WATER_DAMAGE_PER_TICK = 0.5f // 每次扫描造成的伤害值

        private const val NBT_WATER_MB = "WaterMb"
        private const val NBT_WEED_EX_MB = "WeedExMb"
        private const val NBT_WORK_OFFSET = "WorkOffset"
        private const val NBT_ANIMAL_DATA = "AnimalData"

        private val random: Random = Random.create()

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val type = AnimalmatronBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, type)
            fluidLookupRegistered = true
        } 
    }
}
