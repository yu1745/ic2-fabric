package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.AnimalmatronBlock
import ic2_120.content.entity.AnimalFoodMapping
import ic2_120.content.entity.AnimalGrowthData
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.FluidCellItem
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.getFluidCellVariant
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.AnimalmatronScreenHandler
import ic2_120.content.sync.AnimalmatronSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.passive.AnimalEntity
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
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.random.Random
import net.minecraft.world.World

@ModBlockEntity(block = AnimalmatronBlock::class)
class AnimalmatronBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state),
    Inventory,
    IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport,
    ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = AnimalmatronBlock.ACTIVE
    override val tier: Int = ANIMALMATRON_TIER

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
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && it.item is IBatteryItem }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_WATER_INPUT), matcher = { isValid(SLOT_WATER_INPUT, it) }),
            ItemInsertRoute(intArrayOf(SLOT_WEED_EX_INPUT), matcher = { isValid(SLOT_WEED_EX_INPUT, it) }),
            ItemInsertRoute(SLOT_FEED_INDICES, matcher = { isValid(SLOT_FEED_0, it) })
        ),
        extractSlots = IntArray(INVENTORY_SIZE) { it },
        markDirty = { markDirty() }
    )
    val syncedData = SyncedData(this)
    private val discharger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { ANIMALMATRON_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    @RegisterEnergy
    val sync = AnimalmatronSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(ANIMALMATRON_TIER + voltageTierBonus) }
    )

    // 动物数据追踪
    private val animalDataMap = mutableMapOf<java.util.UUID, AnimalGrowthData>()

    private var waterAmountMb: Int = 0
    private var weedExAmountMb: Int = 0
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
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
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
        slot == SLOT_WATER_OUTPUT || slot == SLOT_WEED_EX_OUTPUT -> false
        slot == SLOT_WATER_INPUT -> matchesWaterInput(stack)
        slot == SLOT_WEED_EX_INPUT -> matchesWeedExInput(stack)
        slot == SLOT_DISCHARGING -> stack.item is IBatteryItem
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
            syncedData
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(AnimalmatronSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        waterAmountMb = nbt.getInt(NBT_WATER_MB).coerceIn(0, AnimalmatronSync.WATER_TANK_CAPACITY_MB)
        weedExAmountMb = nbt.getInt(NBT_WEED_EX_MB).coerceIn(0, AnimalmatronSync.WEED_EX_TANK_CAPACITY_MB)
        workOffset = nbt.getInt(NBT_WORK_OFFSET).coerceIn(0, WORK_INTERVAL_TICKS - 1)
        sync.waterAmountMb = waterAmountMb
        sync.weedExAmountMb = weedExAmountMb

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
        nbt.putInt(NBT_WATER_MB, waterAmountMb)
        nbt.putInt(NBT_WEED_EX_MB, weedExAmountMb)
        nbt.putInt(NBT_WORK_OFFSET, workOffset)

        // 写入动物数据
        val animalDataList = net.minecraft.nbt.NbtList()
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

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()
        processWaterInputContainer()
        processWeedExInputContainer()

        var active = false
        val interval = (WORK_INTERVAL_TICKS.toFloat() / speedMultiplier).toInt().coerceAtLeast(1)
        if ((world.time + workOffset) % interval.toLong() == 0L) {
            val report = runScan(world)
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

        sync.waterAmountMb = waterAmountMb
        sync.weedExAmountMb = weedExAmountMb
        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun runScan(world: World): ScanReport {
        val report = ScanReport()
        val scanEnergyCost = (ENERGY_PER_SCAN * energyMultiplier).toLong().coerceAtLeast(1L)

        // 获取范围内的动物实体（仅包含白名单中的动物）
        val box = Box(pos).expand(SCAN_RADIUS.toDouble())
        val animals = world.getEntitiesByClass(
            PassiveEntity::class.java,
            box
        ) { it.isAlive && AnimalFoodMapping.isManagedAnimal(it) }

        // 检查水和除草剂是否充足
        val canGrow = waterAmountMb > 0 && weedExAmountMb > 0

        for (animal in animals) {
            if (sync.amount < scanEnergyCost) return report
            sync.consumeEnergy(scanEnergyCost)

            report.touched++

            // 获取或创建动物数据
            val animalData = animalDataMap.computeIfAbsent(animal.uuid) {
                // 如果是成年动物（从外面牵来的），直接标记为已长成且可繁殖
                if (!animal.isBaby) {
                    AnimalGrowthData(animal.uuid, foodConsumed = FOOD_TO_GROW, canBreed = true)
                } else {
                    AnimalGrowthData(animal.uuid)
                }
            }

            // 尝试喂食
            tryFeedAnimal(animal, animalData, canGrow, animals.size, report)
        }

        // 清理不在范围内的动物数据
        val currentUuids = animals.map { it.uuid }.toSet()
        animalDataMap.keys.filter { it !in currentUuids }.forEach { animalDataMap.remove(it) }

        if (world is ServerWorld) {
            tryBreedReadyAnimals(world, animals, report)
        }

        return report
    }

    private fun tryFeedAnimal(
        animal: PassiveEntity,
        animalData: AnimalGrowthData,
        canGrow: Boolean,
        totalAnimals: Int,
        report: ScanReport
    ) {
        val foodItems = AnimalFoodMapping.getFoodForAnimal(animal)
        if (foodItems.isEmpty()) return

        val world = world ?: return
        val currentDay = (world.time / TICKS_PER_DAY).toInt()

        // 检查是否是新的一天，重置今日喂食计数
        if (currentDay > animalData.currentDay) {
            animalData.foodToday = 0
            animalData.currentDay = currentDay
        }

        // 检查今天是否已经喂够了5个
        if (animalData.foodToday >= FOOD_PER_DAY) return

        // 计算喂食间隔：将5个食物平均分配到整个MC天
        // 12000 tick / 5 = 2400 tick = 2分钟喂1个
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

                // 消耗水和除草剂
                consumeFluids(canGrow, report)

                // 检查是否达到成长/繁殖条件
                checkGrowthAndBreeding(animal, animalData, totalAnimals, report)

                break
            }
        }
    }

    private fun consumeFluids(canGrow: Boolean, report: ScanReport) {
        if (canGrow) {
            val waterToConsume = minOf(waterAmountMb, WATER_PER_CARE)
            if (waterToConsume > 0) {
                waterAmountMb -= waterToConsume
                report.waterConsumed += waterToConsume
            }

            val weedExToConsume = minOf(weedExAmountMb, WEED_EX_PER_CARE)
            if (weedExToConsume > 0) {
                weedExAmountMb -= weedExToConsume
                report.weedExConsumed += weedExToConsume
            }
            markDirty()
        }
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

            // 检查是否可以繁殖
            if (totalAnimals < MAX_ANIMALS_FOR_BREEDING && !animalData.canBreed) {
                animalData.canBreed = true
                report.canBreedNow++
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
            .filter { it.isAlive && !it.isBaby && animalDataMap[it.uuid]?.canBreed == true }

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
        if (waterAmountMb > AnimalmatronSync.WATER_TANK_CAPACITY_MB - 1000) return

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

        waterAmountMb = (waterAmountMb + 1000).coerceAtMost(AnimalmatronSync.WATER_TANK_CAPACITY_MB)
        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_WATER_INPUT, ItemStack.EMPTY)
        if (out.isEmpty) setStack(SLOT_WATER_OUTPUT, emptyRemainder.copy()) else out.increment(1)
        markDirty()
    }

    private fun processWeedExInputContainer() {
        if (weedExAmountMb > AnimalmatronSync.WEED_EX_TANK_CAPACITY_MB - 1000) return

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

        weedExAmountMb = (weedExAmountMb + 1000).coerceAtMost(AnimalmatronSync.WEED_EX_TANK_CAPACITY_MB)
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

    private fun canMergeIntoSlot(current: ItemStack, toInsert: ItemStack): Boolean {
        if (toInsert.isEmpty) return false
        return current.isEmpty || (ItemStack.canCombine(current, toInsert) && current.count < current.maxCount)
    }

    private fun extractFromDischargingSlot() {
        val capacity = sync.getEffectiveCapacity().coerceAtLeast(0L)
        val maxDemand = (capacity - sync.amount).coerceAtLeast(0L)
        if (maxDemand <= 0L) return
        val extracted = discharger.tick(maxDemand)
        if (extracted <= 0L) return
        sync.amount = (sync.amount + extracted).coerceAtMost(capacity)
        sync.energy = sync.amount.toInt().coerceAtMost(Int.MAX_VALUE)
        markDirty()
    }

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
        var bred: Int = 0
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
        const val SLOT_FEED_5 = 9
        const val SLOT_FEED_6 = 10

        const val SLOT_UPGRADE_0 = 11
        const val SLOT_UPGRADE_1 = 12
        const val SLOT_UPGRADE_2 = 13
        const val SLOT_UPGRADE_3 = 14
        const val SLOT_DISCHARGING = 15

        val SLOT_FEED_INDICES = intArrayOf(
            SLOT_FEED_0,
            SLOT_FEED_1,
            SLOT_FEED_2,
            SLOT_FEED_3,
            SLOT_FEED_4,
            SLOT_FEED_5,
            SLOT_FEED_6
        )
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)

        const val INVENTORY_SIZE = 16

        private const val WORK_INTERVAL_TICKS = 20  // 每秒运行一次（20 tick = 1秒）
        private const val SCAN_RADIUS = 4
        private const val ENERGY_PER_SCAN = 1
        private const val ENERGY_PER_FEED = 10

        // 游戏机制常量
        const val FOOD_PER_DAY = 5
        const val FOOD_TO_GROW = 10
        const val FOOD_TO_BREED = 10
        const val MAX_ANIMALS_FOR_BREEDING = 32
        const val TICKS_PER_DAY = 12000L

        // 流体消耗（参考作物监管机）
        const val WATER_PER_CARE = 100 // mB
        const val WEED_EX_PER_CARE = 50 // mB

        private const val NBT_WATER_MB = "WaterMb"
        private const val NBT_WEED_EX_MB = "WeedExMb"
        private const val NBT_WORK_OFFSET = "WorkOffset"
        private const val NBT_ANIMAL_DATA = "AnimalData"

        private val random: Random = Random.create()
    }
}
