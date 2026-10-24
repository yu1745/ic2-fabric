package ic2_120.content.block.machines

import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.IronFenceBlock
import ic2_120.content.block.MagnetizerBlock
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.screen.MagnetizerScreenHandler
import ic2_120.content.sync.MagnetizerSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.EnergyDebtAccounting
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.IRedstoneInverterUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.RedstoneControlComponent
import ic2_120.content.upgrade.RedstoneInverterUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import kotlin.math.abs

@ModBlockEntity(block = MagnetizerBlock::class)
class MagnetizerBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IRedstoneInverterUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = MagnetizerBlock.ACTIVE

    override fun getInventory(): Container = this

    override val tier: Int = 1

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0
    override var redstoneInverted: Boolean = false

    /** 浮点耗能债务：整次脉冲成本 × 1.6^n 分摊 20 tick，小数余数跨 tick 结转。 */
    private var energyDebtF = 0f

    companion object {
        const val SLOT_DISCHARGING = 0
        const val SLOT_UPGRADE_0 = 1
        const val SLOT_UPGRADE_1 = 2
        const val SLOT_UPGRADE_2 = 3
        const val SLOT_UPGRADE_3 = 4
        const val SLOT_BOOTS = 5
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 6

        private val CONNECT_DIRECTIONS = arrayOf(
            Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
        )
    }

    private val inventory = NonNullList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { !it.isEmpty && (it.item is IBatteryItem || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_BOOTS), matcher = { isValid(SLOT_BOOTS, it) }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_DISCHARGING, SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3, SLOT_BOOTS),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = MagnetizerSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(tier + voltageTierBonus) }
    )

    private var interactingPlayer: ServerPlayer? = null

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { tier },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        MagnetizerBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_BOOTS) {
            handleBootSlotChange(stack)
        }
        if (slot == SLOT_DISCHARGING && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    private fun handleBootSlotChange(newStack: ItemStack) {
        val player = interactingPlayer ?: return
        val oldStack = getStack(SLOT_BOOTS)
        if (newStack.isEmpty && !oldStack.isEmpty) {
            player.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY)
        } else if (!newStack.isEmpty && isMetalBootsByStack(newStack) && (oldStack.isEmpty || !ItemStack.areEqual(newStack, oldStack))) {
            val oldBoots = player.getEquippedStack(EquipmentSlot.FEET).copy()
            player.equipStack(EquipmentSlot.FEET, newStack.copyWithCount(1))
            if (!oldBoots.isEmpty) {
                player.inventory.offerOrDrop(oldBoots)
            }
        }
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val result = ContainerHelper.splitStack(inventory, slot, amount)
        if (slot == SLOT_BOOTS && getStack(SLOT_BOOTS).isEmpty) {
            interactingPlayer?.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY)
        }
        return result
    }

    override fun removeStack(slot: Int): ItemStack {
        val result = ContainerHelper.removeStack(inventory, slot)
        if (slot == SLOT_BOOTS) {
            interactingPlayer?.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY)
        }
        return result
    }
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun canPlayerUse(player: Player): Boolean = Container.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_DISCHARGING -> !stack.isEmpty && (stack.item is IBatteryItem || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust)
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        SLOT_BOOTS -> !stack.isEmpty && isMetalBootsByStack(stack)
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.magnetizer")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu {
        interactingPlayer = player as? ServerPlayer
        // 将玩家脚部装备同步到鞋子槽位
        val boots = player?.getEquippedStack(EquipmentSlot.FEET)?.copy() ?: ItemStack.EMPTY
        if (!boots.isEmpty && isMetalBootsByStack(boots)) {
            inventory[SLOT_BOOTS] = boots
        } else {
            inventory[SLOT_BOOTS] = ItemStack.EMPTY
        }
        return MagnetizerScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )
    }

    override fun readNbt(nbt: CompoundTag) {
        super.readNbt(nbt)
        ContainerHelper.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(MagnetizerSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        energyDebtF = if (nbt.contains("EnergyDebt")) nbt.getFloat("EnergyDebt").coerceAtLeast(0f) else 0f
        redstoneInverted = nbt.getBoolean("RedstoneInverted")
        sync.pulseCooldown = nbt.getInt("PulseCooldown").coerceAtLeast(0)
        sync.pulseTicksRemaining = nbt.getInt("PulseTicksRemaining").coerceAtLeast(0)
    }

    override fun writeNbt(nbt: CompoundTag) {
        super.writeNbt(nbt)
        ContainerHelper.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(MagnetizerSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putFloat("EnergyDebt", energyDebtF)
        nbt.putBoolean("RedstoneInverted", redstoneInverted)
        nbt.putInt("PulseCooldown", sync.pulseCooldown)
        nbt.putInt("PulseTicksRemaining", sync.pulseTicksRemaining)
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        RedstoneInverterUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)

        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()
        extractFromDischargingSlot()

        val overclockerCount = OverclockerUpgradeComponent.countOverclockers(this, SLOT_UPGRADE_INDICES)
        val maxHeight = MagnetizerSync.BASE_HEIGHT + overclockerCount * MagnetizerSync.HEIGHT_BONUS_PER_OVERCLOCKER
        sync.effectiveHeight = maxHeight

        val redstoneAllowsRun = RedstoneControlComponent.canRun(world, pos, this)
        sync.redstonePowered = if (redstoneAllowsRun) 1 else 0

        val fences = collectConnectedFences(pos.up(), maxHeight)
        sync.fenceCount = fences.size

        if (!redstoneAllowsRun || fences.isEmpty()) {
            sync.pulseTicksRemaining = 0
            sync.pulseCooldown = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        val reachHeight = computeReachHeight(pos.up(), fences)
        val magnetizeCost = MagnetizerSync.ENERGY_PER_PULSE_BASE + MagnetizerSync.ENERGY_PER_HEIGHT * reachHeight.toLong()
        // 浮点耗能记账：整次脉冲成本 × 1.6^n 分摊 20 tick，小数余数跨 tick 结转
        // （避免原 (cost×1.6^n).toLong()/20 双重取整导致每 tick 欠收锯齿）
        energyDebtF = EnergyDebtAccounting.accrue(energyDebtF, magnetizeCost * energyMultiplier / 20f, sync.getEffectiveCapacity())
        val perTickCost = energyDebtF.toLong().coerceAtLeast(1L)
        val active = sync.consumeEnergy(perTickCost) > 0L
        if (active) {
            energyDebtF -= perTickCost
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.pulseTicksRemaining = 1
            applyMagneticLift(world, fences)
            markDirty()
        } else {
            // 停顿期间不累计债务：防恢复供电后 perTickCost 超容量 all-or-nothing 卡死
            energyDebtF = 0f
            sync.pulseTicksRemaining = 0
        }

        sync.isWearingMetalBoots = if (interactingPlayer?.let { isMetalBoots(it) } == true) 1 else 0

        setActiveState(world, pos, state, active)
        sync.syncCurrentTickFlow()
    }

    private fun collectConnectedFences(start: BlockPos, maxHeight: Int): List<BlockPos> {
        val world = world ?: return emptyList()
        val result = mutableListOf<BlockPos>()
        if (!isModIronFence(start)) return result

        val minY = start.y - maxHeight
        val maxY = start.y + maxHeight

        val queue = ArrayDeque<BlockPos>()
        val visited = HashSet<BlockPos>()
        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)
            for (dir in CONNECT_DIRECTIONS) {
                val next = current.offset(dir)
                if (next.y < minY || next.y > maxY) continue
                if (!visited.add(next)) continue
                if (isModIronFence(next)) {
                    queue.add(next)
                }
            }
        }

        return result
    }

    private fun isModIronFence(pos: BlockPos): Boolean {
        val world = world ?: return false
        return world.getBlockState(pos).block is IronFenceBlock
    }

    private fun computeReachHeight(start: BlockPos, fences: List<BlockPos>): Int {
        var maxDelta = 0
        for (fencePos in fences) {
            maxDelta = maxOf(maxDelta, abs(fencePos.y - start.y))
        }
        return maxDelta.coerceAtLeast(1)
    }

    private fun applyMagneticLift(world: Level, fences: List<BlockPos>) {
        if (fences.isEmpty()) return
        val fenceCenters = fences.map { Vec3(it.x + 0.5, it.y + 0.5, it.z + 0.5) }
        val topY = fences.maxOf { it.y } + 1.15

        val minX = fences.minOf { it.x } - 2
        val minY = fences.minOf { it.y } - 1
        val minZ = fences.minOf { it.z } - 2
        val maxX = fences.maxOf { it.x } + 3
        val maxY = fences.maxOf { it.y } + 3
        val maxZ = fences.maxOf { it.z } + 3

        val searchBox = AABB(minX.toDouble(), minY.toDouble(), minZ.toDouble(), maxX.toDouble(), maxY.toDouble(), maxZ.toDouble())
        val players = world.getEntitiesByClass(Player::class.java, searchBox) { it.isAlive && !it.isSpectator }

        for (player in players) {
            val playerPos = player.pos
            val nearest = fenceCenters.minByOrNull { it.squaredDistanceTo(playerPos) } ?: continue
            val dx = nearest.x - playerPos.x
            val dz = nearest.z - playerPos.z

            val wearingMetalBoots = isMetalBoots(player)
            val horizontalPull = if (wearingMetalBoots) 0.22 else 0.12
            val verticalBoost = if (wearingMetalBoots) 1.15 else 0.72

            val vx = dx.coerceIn(-horizontalPull, horizontalPull)
            val vz = dz.coerceIn(-horizontalPull, horizontalPull)
            val shouldLift = player.y < topY
            val vy = if (shouldLift) player.velocity.y.coerceAtLeast(0.0) + verticalBoost else player.velocity.y

            player.velocity = Vec3(vx, vy, vz)
            player.velocityModified = true
            player.fallDistance = 0f
        }
    }

    private fun isMetalBoots(player: Player): Boolean {
        val boots = player.getEquippedStack(EquipmentSlot.FEET)
        return isMetalBootsByStack(boots)
    }

    private fun isMetalBootsByStack(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val item = stack.item
        if (item == Items.IRON_BOOTS || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS) return true
        val path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(item).path
        return path == "bronze_boots" || path == "nano_boots" || path == "quantum_boots"
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)

        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return

        val inserted = sync.insertEnergy(extracted)
        if (extracted > inserted) sync.forceInsertEnergy(extracted - inserted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }
}

