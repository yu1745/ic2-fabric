package ic2_120.content.block.machines

import ic2_120.content.block.SteamKineticGeneratorBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.fluid.ModFluids
import ic2_120.content.item.SteamTurbine
import ic2_120.content.screen.SteamKineticGeneratorScreenHandler
import ic2_120.content.sync.SteamKineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
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
import net.minecraft.fluid.Fluid
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 蒸汽动能发电机（蒸汽轮机）：消耗蒸汽 → 产出动能 (KU)。
 *
 * 参考 ic2_origin TileEntitySteamKineticGenerator 的数值：
 * - 普通蒸汽: 2 KU/mB
 * - 过热蒸汽: 4 KU/mB
 * - 蒸汽罐: 21,000 mB
 * - 蒸馏水罐: 1,000 mB
 * - 涡轮耐久损耗: 普通 2/次, 过热 1/次
 *
 * 适配 ic2-fabric 动能系统：实现 IKineticMachinePort，从正面输出 KU。
 */
@ModBlockEntity(block = SteamKineticGeneratorBlock::class)
class SteamKineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), IKineticMachinePort, Inventory,
    IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = SteamKineticGeneratorBlock.ACTIVE
    override val tier: Int = 3
    override fun getInventory(): net.minecraft.inventory.Inventory? = this

    // IFluidPipeUpgradeSupport — 默认开启推拉，无需流体升级
    override var fluidPipeProviderEnabled: Boolean = true
    override var fluidPipeReceiverEnabled: Boolean = true
    override var fluidPipeProviderFilter: Fluid? = null
    override var fluidPipeReceiverFilter: Fluid? = null
    override var fluidPipeProviderSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeReceiverSides: MutableSet<Direction> = mutableSetOf()
    override var fluidPipeEjectorCount: Int = 1
    override var fluidPipePullingCount: Int = 1

    companion object {
        const val SLOT_TURBINE = 0
        const val SLOT_UPGRADE = 1
        const val INVENTORY_SIZE = 2
        private const val NBT_STEAM_TANK = "SteamTank"
        private const val NBT_WATER_TANK = "WaterTank"
        private const val NBT_KU_BUFFER = "KuBuffer"
        private const val NBT_CONDENSATION = "Condensation"

        private const val STEAM_TANK_CAPACITY = FluidConstants.BUCKET * 21  // 21,000 mB
        private const val WATER_TANK_CAPACITY = FluidConstants.BUCKET       // 1,000 mB

        @Volatile
        private var fluidLookupRegistered = false

        @RegisterFluidStorage
        fun registerFluidStorageLookup() {
            if (fluidLookupRegistered) return
            val beType = SteamKineticGeneratorBlockEntity::class.type()
            FluidStorage.SIDED.registerForBlockEntity({ be, side -> be.getFluidStorageForSide(side) }, beType)
            fluidLookupRegistered = true
        }

        fun toMb(droplets: Long): Int =
            (droplets * 1000 / FluidConstants.BUCKET).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

        fun mbToDroplets(mb: Long): Long = mb * FluidConstants.BUCKET / 1000
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_TURBINE), matcher = { isValid(SLOT_TURBINE, it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_UPGRADE), matcher = { it.item is ic2_120.content.item.IUpgradeItem && ic2_120.content.upgrade.UpgradeItemRegistry.canAccept(this@SteamKineticGeneratorBlockEntity, it.item) })
        ),
        extractSlots = intArrayOf(SLOT_TURBINE, SLOT_UPGRADE),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)
    val sync = SteamKineticGeneratorSync(syncedData)

    /** 内部 KU 缓冲区 */
    private var kuBuffer: Int = 0

    /** 冷凝进度 (0-99, 满100产1mB蒸馏水) */
    private var condensationProgress: Int = 0

    /** 上一 tick 的 KU 输出 */
    private var lastKuOutput: Int = 0

    /** turbine ticker（每20 tick扣耐久） */
    private var turbineTicker: Int = 0

    /** 蒸馏水满导致涡轮阻塞 */
    private var isTurbineFilledWithWater: Boolean = false

    /** 是否在空排蒸汽 */
    private var ventingSteam: Boolean = false

    // ==== 流体槽 ====

    /** 蒸汽输入槽 */
    private val steamTank = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = STEAM_TANK_CAPACITY

        override fun canInsert(variant: FluidVariant): Boolean = ModFluids.isSteam(variant.fluid)

        override fun canExtract(variant: FluidVariant): Boolean = false

        override fun onFinalCommit() {
            sync.steamAmount = toMb(amount)
            markDirty()
        }

        fun consumeInternal(droplets: Long): Long {
            if (droplets <= 0L || variant.isBlank) return 0L
            val actual = minOf(droplets, amount)
            if (actual <= 0L) return 0L
            amount -= actual
            if (amount <= 0L) variant = FluidVariant.blank()
            sync.steamAmount = toMb(amount)
            markDirty()
            return actual
        }

        fun isSuperheated(): Boolean = variant.fluid == ModFluids.SUPERHEATED_STEAM_STILL
    }

    /** 蒸馏水输出槽 */
    private val distilledWaterTank = object : SingleVariantStorage<FluidVariant>() {
        override fun getBlankVariant(): FluidVariant = FluidVariant.blank()
        override fun getCapacity(variant: FluidVariant): Long = WATER_TANK_CAPACITY

        override fun canInsert(variant: FluidVariant): Boolean = false
        override fun canExtract(variant: FluidVariant): Boolean = ModFluids.isFluid(variant.fluid)

        override fun onFinalCommit() {
            sync.distilledWaterAmount = toMb(amount)
            markDirty()
        }

        fun availableSpace(): Long = (WATER_TANK_CAPACITY - amount).coerceAtLeast(0L)

        fun insertInternal(droplets: Long): Long {
            if (droplets <= 0L) return 0L
            val space = WATER_TANK_CAPACITY - amount
            val actual = minOf(droplets, space)
            if (actual <= 0L) return 0L
            amount += actual
            if (variant.isBlank) variant = FluidVariant.of(ModFluids.DISTILLED_WATER_STILL)
            sync.distilledWaterAmount = toMb(amount)
            markDirty()
            return actual
        }

        fun ejectToNeighbors(world: World, pos: BlockPos) {
            if (amount <= 0L || variant.isBlank) return
            for (side in Direction.entries) {
                if (amount <= 0L) break
                val neighborPos = pos.offset(side)
                val storage = FluidStorage.SIDED.find(world, neighborPos, side.opposite) ?: continue
                try {
                    val extracted = storage.insert(variant, amount, null)
                    if (extracted > 0L) {
                        amount -= extracted
                        if (amount <= 0L) variant = FluidVariant.blank()
                        sync.distilledWaterAmount = toMb(amount)
                        markDirty()
                    }
                } catch (_: Exception) { }
            }
        }
    }

    /** 对外流体 I/O 接口 */
    private val ioStorage = object : Storage<FluidVariant> {
        override fun supportsInsertion(): Boolean = true
        override fun supportsExtraction(): Boolean = true

        override fun insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (resource.fluid != ModFluids.STEAM_STILL && resource.fluid != ModFluids.SUPERHEATED_STEAM_STILL) return 0L
            return steamTank.insert(resource, maxAmount, transaction)
        }

        override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount)
            if (resource.fluid != ModFluids.DISTILLED_WATER_STILL) return 0L
            return distilledWaterTank.extract(resource, maxAmount, transaction)
        }

        override fun iterator(): MutableIterator<StorageView<FluidVariant>> {
            val views = mutableListOf<StorageView<FluidVariant>>()
            if (!steamTank.variant.isBlank && steamTank.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = steamTank.variant
                    override fun getAmount(): Long = steamTank.amount
                    override fun getCapacity(): Long = STEAM_TANK_CAPACITY
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = 0L
                    override fun isResourceBlank(): Boolean = false
                })
            }
            if (!distilledWaterTank.variant.isBlank && distilledWaterTank.amount > 0L) {
                views.add(object : StorageView<FluidVariant> {
                    override fun getResource(): FluidVariant = distilledWaterTank.variant
                    override fun getAmount(): Long = distilledWaterTank.amount
                    override fun getCapacity(): Long = WATER_TANK_CAPACITY
                    override fun extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
                        distilledWaterTank.extract(resource, maxAmount, transaction)
                    override fun isResourceBlank(): Boolean = false
                })
            }
            return views.iterator()
        }
    }

    /** 流体存储方向访问 (SolarDistiller 模式) */
    fun getFluidStorageForSide(side: Direction?): Storage<FluidVariant>? = ioStorage

    constructor(pos: BlockPos, state: BlockState) : this(
        SteamKineticGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    // ==== Inventory ====

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_TURBINE && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_TURBINE -> stack.item is SteamTurbine
        SLOT_UPGRADE -> stack.item is ic2_120.content.item.IUpgradeItem && ic2_120.content.upgrade.UpgradeItemRegistry.canAccept(this, stack.item)
        else -> false
    }

    // ==== IKineticMachinePort — 所有面均可输出动能 ====

    override fun canOutputKuTo(side: Direction): Boolean = true

    override fun canInputKuFrom(side: Direction): Boolean = false

    override fun getStoredKu(side: Direction): Int = kuBuffer.coerceAtLeast(0)

    override fun getKuCapacity(side: Direction): Int = SteamKineticGeneratorSync.MAX_KU_OUTPUT * 2

    override fun extractKu(side: Direction, amount: Int, simulate: Boolean): Int {
        if (amount <= 0) return 0
        val extracted = minOf(amount, kuBuffer)
        if (!simulate) {
            kuBuffer -= extracted
            markDirty()
        }
        return extracted
    }

    override fun insertKu(side: Direction, amount: Int, simulate: Boolean): Int = 0

    // ==== ScreenHandler ====

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.steam_kinetic_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        SteamKineticGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData,
            itemStorage
        )

    // ==== NBT ====

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        kuBuffer = nbt.getInt(NBT_KU_BUFFER)
        condensationProgress = nbt.getInt(NBT_CONDENSATION)

        if (nbt.contains(NBT_STEAM_TANK)) {
            val tankNbt = nbt.getCompound(NBT_STEAM_TANK)
            steamTank.variant = FluidVariant.fromNbt(tankNbt.getCompound("variant"))
            steamTank.amount = tankNbt.getLong("amount")
        }
        if (nbt.contains(NBT_WATER_TANK)) {
            val tankNbt = nbt.getCompound(NBT_WATER_TANK)
            distilledWaterTank.variant = FluidVariant.fromNbt(tankNbt.getCompound("variant"))
            distilledWaterTank.amount = tankNbt.getLong("amount")
        }
        sync.steamAmount = toMb(steamTank.amount)
        sync.distilledWaterAmount = toMb(distilledWaterTank.amount)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putInt(NBT_KU_BUFFER, kuBuffer)
        nbt.putInt(NBT_CONDENSATION, condensationProgress)

        val steamTankNbt = NbtCompound()
        steamTankNbt.put("variant", steamTank.variant.toNbt())
        steamTankNbt.putLong("amount", steamTank.amount)
        nbt.put(NBT_STEAM_TANK, steamTankNbt)

        val waterTankNbt = NbtCompound()
        waterTankNbt.put("variant", distilledWaterTank.variant.toNbt())
        waterTankNbt.putLong("amount", distilledWaterTank.amount)
        nbt.put(NBT_WATER_TANK, waterTankNbt)
    }

    // ==== Tick ====

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        // 流体管道输入蒸汽
        if (fluidPipeReceiverEnabled) {
            FluidPipeUpgradeComponent.pullFluidFromNeighbors(world, pos, steamTank, fluidPipeReceiverFilter, fluidPipeReceiverSides, upgradeCount = fluidPipePullingCount)
        }

        val turbineStack = getStack(SLOT_TURBINE)
        val hasTurbine = !turbineStack.isEmpty && turbineStack.item is SteamTurbine
        val isSuperheated = steamTank.isSuperheated()

        // 蒸馏水输出（通过升级系统）
        if (fluidPipeProviderEnabled) {
            FluidPipeUpgradeComponent.ejectFluidToNeighbors(world, pos, distilledWaterTank, fluidPipeProviderFilter, fluidPipeProviderSides, upgradeCount = fluidPipeEjectorCount)
        }

        // 蒸馏水罐已空 → 解除涡轮阻塞
        if (distilledWaterTank.availableSpace() >= FluidConstants.BUCKET / 1000 && isTurbineFilledWithWater) {
            isTurbineFilledWithWater = false
        }

        lastKuOutput = 0
        var steamConsumed = 0L

        // 有涡轮 + 有蒸汽 + 未阻塞
        if (hasTurbine && steamTank.amount > 0L && !isTurbineFilledWithWater) {
            val superheated = isSuperheated

            // ---- handleSteam: 消耗全部蒸汽 ----
            val totalMb = toMb(steamTank.amount)
            steamTank.amount = 0L
            steamTank.variant = FluidVariant.blank()
            sync.steamAmount = 0
            steamConsumed = totalMb.toLong()

            // KU = totalMb × 2 × (superheated ? 2 : 1)
            val kuProduced = totalMb * (if (superheated) 4 else 2)

            // ----
            if (superheated) {
                // 过热蒸汽 → 输出等量普通蒸汽（通用弹出）
                outputSteam(world, pos, totalMb)
            } else {
                // 普通蒸汽 → 10% 冷凝, 90% 输出给冷凝机（特殊方法，只认冷凝机）
                condensationProgress += totalMb / 10
                outputCondenserSteam(world, pos, totalMb - totalMb / 10)
            }

            // ---- KU 节流: 蒸馏水罐有水时按比例降低 ----
            val waterFill = toMb(distilledWaterTank.amount)
            val kuMultiplier = if (waterFill == 0) 1f
                else (1f - waterFill.toFloat() / WATER_TANK_CAPACITY.toFloat())
            val finalKu = (kuProduced * kuMultiplier).toInt().coerceAtLeast(0)

            // KU 缓冲区
            val kuSpace = (SteamKineticGeneratorSync.MAX_KU_OUTPUT * 2 - kuBuffer).coerceAtLeast(0)
            val actualKu = minOf(finalKu, kuSpace)
            kuBuffer += actualKu
            lastKuOutput = actualKu

            // ---- 冷凝产蒸馏水 ----
            if (condensationProgress >= 100) {
                if (distilledWaterTank.availableSpace() >= 1) {
                    distilledWaterTank.insertInternal(mbToDroplets(1L))
                    condensationProgress -= 100
                } else {
                    isTurbineFilledWithWater = true
                }
            }

            // ---- 涡轮耐久（每 20 tick，对齐 ic2_origin getTickRate） ----
            turbineTicker++
            if (turbineTicker >= 20) {
                turbineTicker = 0
                val wear = if (superheated) SteamKineticGeneratorSync.TURBINE_WEAR_SUPERHEATED
                    else SteamKineticGeneratorSync.TURBINE_WEAR_NORMAL
                if (turbineStack.damage + wear < turbineStack.maxDamage) {
                    turbineStack.damage += wear
                } else {
                    inventory[SLOT_TURBINE] = ItemStack.EMPTY
                }
            }
        } else if (hasTurbine && steamTank.amount > 0L && isTurbineFilledWithWater) {
            // 蒸馏水满阻塞 → 强制排汽，否则爆炸
            val totalMb = toMb(steamTank.amount)
            steamTank.amount = 0L
            steamTank.variant = FluidVariant.blank()
            sync.steamAmount = 0
            outputSteam(world, pos, totalMb)
        }

        // 更新同步数据
        sync.steamAmount = toMb(steamTank.amount)
        sync.distilledWaterAmount = toMb(distilledWaterTank.amount)
        sync.kuOutput = lastKuOutput
        sync.steamConsume = steamConsumed.toInt()
        sync.isSuperheated = if (isSuperheated) 1 else 0
        sync.condensationProgress = condensationProgress
        sync.waterBlocked = if (isTurbineFilledWithWater) 1 else 0
        sync.hasTurbine = if (hasTurbine) 1 else 0

        val active = hasTurbine && steamTank.amount > 0L && lastKuOutput > 0
        setActiveState(world, pos, state, active)

        markDirty()
    }

    /**
     * 输出蒸汽到相邻方块 — 通用方法。
     * 推给任意支持插入的 FluidStorage 邻居。
     * 找不到接收方时 vent（10% 爆炸/tick）。
     */
    private fun outputSteam(world: World, pos: BlockPos, amountMb: Int) {
        if (amountMb <= 0) return
        var remaining = amountMb
        val steamVariant = FluidVariant.of(ModFluids.STEAM_STILL)

        for (side in Direction.entries) {
            if (remaining <= 0) break
            val neighborStorage = FluidStorage.SIDED.find(world, pos.offset(side), side.opposite)
            if (neighborStorage == null || !neighborStorage.supportsInsertion()) continue

            Transaction.openOuter().use { tx ->
                val accepted = neighborStorage.insert(steamVariant, mbToDroplets(remaining.toLong()), tx)
                if (accepted > 0) {
                    tx.commit()
                    remaining -= toMb(accepted)
                }
            }
        }

        if (remaining > 0) {
            ventingSteam = true
            if (world.random.nextInt(10) == 0) {
                world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                    1f, World.ExplosionSourceType.NONE)
            }
        } else {
            ventingSteam = false
        }
    }

    /**
     * 输出 90% 普通蒸汽到相邻冷凝机 — 特殊方法。
     * 只推给 CondenserBlockEntity，不推给其他方块。
     * 找不到接收方时 vent（10% 爆炸/tick）。
     */
    private fun outputCondenserSteam(world: World, pos: BlockPos, amountMb: Int) {
        if (amountMb <= 0) return
        var remaining = amountMb
        val steamVariant = FluidVariant.of(ModFluids.STEAM_STILL)

        for (side in Direction.entries) {
            if (remaining <= 0) break
            val neighborPos = pos.offset(side)
            if (world.getBlockEntity(neighborPos) !is CondenserBlockEntity) continue
            val neighborStorage = FluidStorage.SIDED.find(world, neighborPos, side.opposite)
            if (neighborStorage == null || !neighborStorage.supportsInsertion()) continue

            Transaction.openOuter().use { tx ->
                val accepted = neighborStorage.insert(steamVariant, mbToDroplets(remaining.toLong()), tx)
                if (accepted > 0) {
                    tx.commit()
                    remaining -= toMb(accepted)
                }
            }
        }

        if (remaining > 0) {
            ventingSteam = true
            if (world.random.nextInt(10) == 0) {
                world.createExplosion(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                    1f, World.ExplosionSourceType.NONE)
            }
        } else {
            ventingSteam = false
        }
    }

}
