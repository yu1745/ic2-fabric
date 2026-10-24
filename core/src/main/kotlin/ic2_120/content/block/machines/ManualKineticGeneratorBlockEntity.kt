package ic2_120.content.block.machines

import ic2_120.compat.time

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

import ic2_120.content.block.ManualKineticGeneratorBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.item.CrankHandleItem
import ic2_120.content.item.CrankMaterial
import ic2_120.content.sync.ManualKineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.core.HolderLookup

@ModBlockEntity(block = ManualKineticGeneratorBlock::class)
class ManualKineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Container, IKineticMachinePort {

    companion object {
        const val CRANK_SLOT = 0
        private const val MAX_USE_DISTANCE = 6.0
        private val ALLOWED_CRANKS = setOf("wooden_crank_handle", "iron_crank_handle", "steel_crank_handle", "carbon_crank_handle")
    }

    override val tier: Int = 1
    override val activeProperty: net.minecraft.world.level.block.state.properties.BooleanProperty = ManualKineticGeneratorBlock.ACTIVE

    private val inventory = NonNullList.withSize(1, ItemStack.EMPTY)
    private val syncedData = SyncedData()
    val sync = ManualKineticGeneratorSync(syncedData)

    private var pendingOutputKu: Int = 0

    // 驱动者 tick 记录：Key 放宽为 Entity，兼容玩家与外部实体（如 TLM 女仆）。
    // 玩家通过 onUse 空手 + 有摇把路径写入；外部实体通过 driveByExternal 写入。
    private val driverTick = mutableMapOf<Entity, Long>()

    var clientTurnAngle: Float = 0f
        private set

    private val scanOffset: Int = (pos.asLong().toInt() and Int.MAX_VALUE) % 10

    constructor(pos: BlockPos, state: BlockState) : this(
        ManualKineticGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Container = this

    fun getCrankStack(): ItemStack = inventory[CRANK_SLOT]

    fun hasCrank(): Boolean = !inventory[CRANK_SLOT].isEmpty

    fun getCrankMaterial(): CrankMaterial? {
        val item = inventory[CRANK_SLOT].item as? CrankHandleItem ?: return null
        return item.material
    }

    fun getKuPerTick(): Int {
        val item = inventory[CRANK_SLOT].item as? CrankHandleItem ?: return 0
        return item.getKuPerTick()
    }

    private fun isAllowedCrank(stack: ItemStack): Boolean {
        val id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(stack.item)
        return ALLOWED_CRANKS.contains(id.path)
    }

    fun onUse(player: Player, hand: InteractionHand, world: Level, pos: BlockPos, state: BlockState): InteractionResult {
        if (world.isClient) return InteractionResult.SUCCESS

        val heldStack = player.getItemInHand(hand)
        val crankStack = inventory[CRANK_SLOT]

        when {
            heldStack.item is CrankHandleItem && !crankStack.isEmpty -> {
                val extracted = crankStack.copy()
                inventory[CRANK_SLOT] = heldStack.split(1)
                markDirtyAndSync()
                if (!player.inventory.safeInsert(extracted)) {
                    player.spawnAtLocation(extracted, false)
                }
                return InteractionResult.SUCCESS
            }

            heldStack.item is CrankHandleItem && crankStack.isEmpty -> {
                inventory[CRANK_SLOT] = heldStack.split(1)
                markDirtyAndSync()
                return InteractionResult.SUCCESS
            }

            heldStack.isEmpty && hasCrank() -> {
                driverTick[player] = world.time
                return InteractionResult.SUCCESS
            }

            heldStack.isEmpty && !hasCrank() -> {
                player.sendMessage(Component.translatable("message.ic2_120.manual_kinetic_no_crank"), true)
                return InteractionResult.SUCCESS
            }

            else -> return InteractionResult.PASS
        }
    }

    /**
     * 供外部实体（如 TLM 女仆）周期性调用以注册"正在驱动"。
     * 每 tick 调用一次，满足 6-tick 超时窗口即可持续输出 KU。
     * 调用者负责保证距离在 MAX_USE_DISTANCE 内，否则 tick 会被自动清理。
     */
    fun driveByExternal(driver: Entity) {
        val world = world ?: return
        driverTick[driver] = world.time
    }

    private fun isEntityActivelyUsing(entity: Entity): Boolean {
        val lastTick = driverTick[entity] ?: return false
        val world = world ?: return false
        if (world.time - lastTick > 6) return false
        val distance = entity.pos.distanceTo(Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5))
        if (distance > MAX_USE_DISTANCE) return false
        return true
    }

    fun dropCrank() {
        val world = world ?: return
        if (world.isClient || inventory[CRANK_SLOT].isEmpty) return
        net.minecraft.world.Containers.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), inventory[CRANK_SLOT])
        inventory[CRANK_SLOT] = ItemStack.EMPTY
        setChanged()
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) {
            if (sync.isTurning) {
                clientTurnAngle += 15f
                if (clientTurnAngle >= 360f) clientTurnAngle -= 360f
            }
            return
        }

        driverTick.entries.removeIf { (entity, _) ->
            !entity.isAlive || !isEntityActivelyUsing(entity)
        }

        val wasTurning = sync.isTurning
        val isTurning = driverTick.isNotEmpty() && hasCrank()

        if (isTurning) {
            val kuPerTick = getKuPerTick()
            pendingOutputKu += kuPerTick
            sync.storedKu = pendingOutputKu
            sync.outputKu = kuPerTick
        } else {
            sync.outputKu = 0
        }

        sync.isTurning = isTurning
        sync.crankMaterial = when (getCrankMaterial()) {
            CrankMaterial.WOOD -> 1
            CrankMaterial.IRON -> 2
            CrankMaterial.STEEL -> 3
            CrankMaterial.CARBON -> 4
            null -> 0
        }
        sync.hasCrank = hasCrank()
        
        sync.turnAngle = clientTurnAngle

        val active = pendingOutputKu > 0 || isTurning
        setActiveState(world, pos, state, active)

        if (wasTurning != isTurning) {
            markDirtyAndSync()
        } else if (isTurning) {
            setChanged()
        }
    }

    override fun canOutputKuTo(side: Direction): Boolean = side.axis.isHorizontal

    override fun getStoredKu(side: Direction): Int =
        if (canOutputKuTo(side)) pendingOutputKu.coerceAtLeast(0) else 0

    override fun getKuCapacity(side: Direction): Int =
        if (canOutputKuTo(side)) Int.MAX_VALUE else 0

    override fun getMaxExtractableKu(side: Direction): Int =
        if (canOutputKuTo(side)) pendingOutputKu.coerceAtLeast(0) else 0

    override fun extractKu(side: Direction, amount: Int, simulate: Boolean): Int {
        if (!canOutputKuTo(side) || amount <= 0) return 0
        val extracted = minOf(amount, pendingOutputKu.coerceAtLeast(0))
        if (!simulate && extracted > 0) {
            pendingOutputKu = (pendingOutputKu - extracted).coerceAtLeast(0)
            sync.extractedKu = (sync.extractedKu + extracted).coerceAtLeast(0)
            setChanged()
        }
        return extracted
    }

    private fun markDirtyAndSync() {
        setChanged()
        val world = world ?: return
        if (!world.isClient) {
            val state = world.getBlockState(pos)
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS)
            (world as? ServerLevel)?.chunkSource?.markForUpdate(pos)
        }
    }

    override fun size(): Int = 1

    override fun isEmpty(): Boolean = inventory[CRANK_SLOT].isEmpty

    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val result = ContainerHelper.removeItem(inventory, slot, amount)
        if (!result.isEmpty) {
            markDirtyAndSync()
        }
        return result
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val result = ContainerHelper.takeItem(inventory, slot)
        if (!result.isEmpty) {
            markDirtyAndSync()
        }
        return result
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot != CRANK_SLOT) return
        if (stack.isEmpty) {
            inventory[CRANK_SLOT] = ItemStack.EMPTY
        } else if (isAllowedCrank(stack)) {
            val copy = stack.copy()
            copy.count = 1
            inventory[CRANK_SLOT] = copy
        }
        markDirtyAndSync()
    }

    override fun clear() {
        inventory[CRANK_SLOT] = ItemStack.EMPTY
        markDirtyAndSync()
    }

    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun readNbt(nbt: CompoundTag, lookup: HolderLookup.WrapperLookup) {
        super.readNbt(nbt, lookup)
        pendingOutputKu = nbt.getInt("PendingKu").coerceAtLeast(0)
        inventory[CRANK_SLOT] = ItemStack.EMPTY
        ContainerHelper.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: CompoundTag, lookup: HolderLookup.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putInt("PendingKu", pendingOutputKu)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
    }

    override fun toInitialChunkDataNbt(lookup: HolderLookup.WrapperLookup): CompoundTag = createNbt(lookup)

    override fun toUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)
}
