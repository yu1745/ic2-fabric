package ic2_120.content.block.machines

import ic2_120.content.block.AnimalSlaughtererBlock
import ic2_120.content.entity.AnimalFoodMapping
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.screen.AnimalSlaughtererScreenHandler
import ic2_120.content.sync.AnimalSlaughtererSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound

import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.ItemScatterer
import net.minecraft.world.World
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.Unpooled

@ModBlockEntity(block = AnimalSlaughtererBlock::class)
class AnimalSlaughtererBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state),
    Inventory,
    IEnergyStorageUpgradeSupport,
    IEjectorUpgradeSupport,
    ITransformerUpgradeSupport,
    ExtendedScreenHandlerFactory<PacketByteBuf> {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = AnimalSlaughtererBlock.ACTIVE
    override val tier: Int = ANIMAL_SLAUGHTERER_TIER

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
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && it.item is IBatteryItem }, maxPerSlot = 1)
        ),
        extractSlots = SLOT_CONTENT_INDICES + SLOT_UPGRADE_INDICES + intArrayOf(SLOT_DISCHARGING),
        markDirty = { markDirty() }
    )
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = AnimalSlaughtererSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(ANIMAL_SLAUGHTERER_TIER + voltageTierBonus) }
    )
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { ANIMAL_SLAUGHTERER_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(AnimalSlaughtererBlockEntity::class.type(), pos, state)

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
    override fun markDirty() = super.markDirty()

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when {
        stack.isEmpty -> false
        SLOT_CONTENT_INDICES.contains(slot) -> false
        slot == SLOT_DISCHARGING -> stack.item is IBatteryItem
        SLOT_UPGRADE_INDICES.contains(slot) -> stack.item is IUpgradeItem
        else -> false
    }

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.animal_slaughterer")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        AnimalSlaughtererScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(AnimalSlaughtererSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(AnimalSlaughtererSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_CONTENT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        var active = false
        if (world.time % WORK_INTERVAL_TICKS.toLong() == 0L) {
            val report = runScan(world)
            sync.checkedThisRun = report.checked
            sync.slaughteredThisRun = report.slaughtered
            sync.animalCount = report.checked
            active = report.slaughtered > 0
        }

        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun runScan(world: World): ScanReport {
        val report = ScanReport()

        // 获取范围内的所有受监管成年动物
        val box = Box(pos).expand(SCAN_RADIUS.toDouble())
        val animals = world.getEntitiesByClass(PassiveEntity::class.java, box) {
            it.isAlive &&
            AnimalFoodMapping.isManagedAnimal(it)
        }

        val animalCount = animals.size
        report.checked = animalCount

        // 只有达到 32 只才开始屠宰
        if (animalCount < TARGET_COUNT) return report

        // 计算要屠宰的数量（屠宰到剩余 16 只）
        val toSlaughter = animalCount - MIN_COUNT

        if (toSlaughter <= 0) return report
        if (sync.amount < (ENERGY_PER_SCAN * animalCount)) return report
        if (isOutputFull()) return report

        // 消耗扫描能量
        sync.consumeEnergy((ENERGY_PER_SCAN * animalCount).toLong())

        // 屠宰动物
        var slaughtered = 0
        for (animal in animals) {
            if (slaughtered >= toSlaughter) break
            if (!animal.isAlive) continue

            // 使用实体类型获取掉落物
            val drops = getDroppedItems(animal)
            if (drops.isEmpty()) continue

            // 杀死动物
            animal.discard()

            // 收集掉落物
            for (drop in drops) {
                val remaining = insertIntoContentSlots(drop.copy())
                if (!remaining.isEmpty) {
                    ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), remaining)
                }
                sync.consumeEnergy(ENERGY_PER_SLAUGHTER_STACK.toLong())
            }

            slaughtered++
            report.slaughtered++
        }

        markDirty()
        return report
    }

    private fun getDroppedItems(animal: PassiveEntity): List<ItemStack> {
        val drops = mutableListOf<ItemStack>()
        val entityId = Registries.ENTITY_TYPE.getId(animal.type)
        val rand = net.minecraft.util.math.random.Random.create()

        // 根据实体类型返回掉落物（模拟 LootTable 随机掉落）
        when (entityId.toString()) {
            "minecraft:pig" -> {
                // 2-3 猪排
                drops.add(ItemStack(net.minecraft.item.Items.PORKCHOP, 2 + rand.nextInt(2)))
            }
            "minecraft:cow", "minecraft:mooshroom" -> {
                // 2-3 牛肉 + 0-2 皮革
                drops.add(ItemStack(net.minecraft.item.Items.BEEF, 2 + rand.nextInt(2)))
                if (rand.nextFloat() < 0.5f) {
                    drops.add(ItemStack(net.minecraft.item.Items.LEATHER, 1 + rand.nextInt(2)))
                }
            }
            "minecraft:sheep" -> {
                // 1-2 羊肉 + 1 羊毛（羊被击杀时如果没剪毛就掉羊毛）
                drops.add(ItemStack(net.minecraft.item.Items.MUTTON, 1 + rand.nextInt(2)))
                if ((animal as? SheepEntity)?.isSheared != true) {
                    drops.add(ItemStack(net.minecraft.item.Items.WHITE_WOOL, 1))
                }
            }
            "minecraft:chicken" -> {
                // 1 鸡肉 + 0-2 羽毛
                drops.add(ItemStack(net.minecraft.item.Items.CHICKEN, 1))
                drops.add(ItemStack(net.minecraft.item.Items.FEATHER, rand.nextInt(3)))
            }
            "minecraft:rabbit" -> {
                // 1 兔肉 + 0-1 兔子皮
                drops.add(ItemStack(net.minecraft.item.Items.RABBIT, 1))
                if (rand.nextFloat() < 0.5f) {
                    drops.add(ItemStack(net.minecraft.item.Items.RABBIT_HIDE, 1))
                }
            }
            "minecraft:horse", "minecraft:donkey", "minecraft:mule" -> {
                // 1-2 皮革
                drops.add(ItemStack(net.minecraft.item.Items.LEATHER, 1 + rand.nextInt(2)))
            }
            "minecraft:llama" -> {
                // 1-2 皮革
                drops.add(ItemStack(net.minecraft.item.Items.LEATHER, 1 + rand.nextInt(2)))
            }
        }
        return drops
    }

    private fun isOutputFull(): Boolean {
        for (slot in SLOT_CONTENT_INDICES) {
            val stack = getStack(slot)
            if (stack.isEmpty) return false
            val limit = minOf(stack.maxCount, maxCountPerStack)
            if (stack.count < limit) return false
        }
        return true
    }

    private fun insertIntoContentSlots(stack: ItemStack): ItemStack {
        var remaining = stack.copy()

        for (slot in SLOT_CONTENT_INDICES) {
            if (remaining.isEmpty) break
            val existing = getStack(slot)
            if (existing.isEmpty) continue
            if (!ItemStack.areItemsAndComponentsEqual(existing, remaining)) continue
            val limit = minOf(existing.maxCount, maxCountPerStack)
            val room = (limit - existing.count).coerceAtLeast(0)
            if (room <= 0) continue
            val move = minOf(room, remaining.count)
            existing.increment(move)
            remaining.decrement(move)
        }

        for (slot in SLOT_CONTENT_INDICES) {
            if (remaining.isEmpty) break
            val existing = getStack(slot)
            if (!existing.isEmpty) continue
            val toInsert = minOf(remaining.count, maxCountPerStack)
            if (toInsert <= 0) break
            val inserted = remaining.copy()
            inserted.count = toInsert
            setStack(slot, inserted)
            remaining.decrement(toInsert)
        }

        return remaining
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return
        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return
        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }

    data class ScanReport(
        var checked: Int = 0,
        var slaughtered: Int = 0
    )

    companion object {
        const val ANIMAL_SLAUGHTERER_TIER = 1

        const val SLOT_CONTENT_0 = 0
        const val SLOT_CONTENT_1 = 1
        const val SLOT_CONTENT_2 = 2
        const val SLOT_CONTENT_3 = 3
        const val SLOT_CONTENT_4 = 4
        const val SLOT_CONTENT_5 = 5
        const val SLOT_CONTENT_6 = 6
        const val SLOT_CONTENT_7 = 7
        const val SLOT_CONTENT_8 = 8
        const val SLOT_CONTENT_9 = 9
        const val SLOT_CONTENT_10 = 10
        const val SLOT_CONTENT_11 = 11
        const val SLOT_CONTENT_12 = 12
        const val SLOT_CONTENT_13 = 13
        const val SLOT_CONTENT_14 = 14

        const val SLOT_UPGRADE_0 = 15
        const val SLOT_UPGRADE_1 = 16
        const val SLOT_UPGRADE_2 = 17
        const val SLOT_UPGRADE_3 = 18
        const val SLOT_DISCHARGING = 19

        val SLOT_CONTENT_INDICES = intArrayOf(
            SLOT_CONTENT_0, SLOT_CONTENT_1, SLOT_CONTENT_2, SLOT_CONTENT_3, SLOT_CONTENT_4,
            SLOT_CONTENT_5, SLOT_CONTENT_6, SLOT_CONTENT_7, SLOT_CONTENT_8, SLOT_CONTENT_9,
            SLOT_CONTENT_10, SLOT_CONTENT_11, SLOT_CONTENT_12, SLOT_CONTENT_13, SLOT_CONTENT_14
        )
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)

        const val INVENTORY_SIZE = 20

        private const val WORK_INTERVAL_TICKS = 20  // 1秒扫描一次
        private const val ENERGY_PER_SCAN = 1  // 每只动物 1 EU
        private const val ENERGY_PER_SLAUGHTER_STACK = 20  // 每堆掉落物 20 EU

        private const val SCAN_RADIUS = 4  // 9×9×3 范围
        private const val TARGET_COUNT = 32  // 达到此数量开始屠宰
        private const val MIN_COUNT = 16  // 屠宰到此数量停止
    }
}
