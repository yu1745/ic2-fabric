package ic2_120.content.block.machines

import ic2_120.content.block.ManualKineticGeneratorBlock
import ic2_120.content.block.transmission.IKineticMachinePort
import ic2_120.content.item.CrankHandleItem
import ic2_120.content.item.CrankMaterial
import ic2_120.content.sync.ManualKineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.text.Text
import net.minecraft.world.World
import net.minecraft.registry.RegistryWrapper

@ModBlockEntity(block = ManualKineticGeneratorBlock::class)
class ManualKineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory, IKineticMachinePort {

    companion object {
        const val CRANK_SLOT = 0
        private const val MAX_USE_DISTANCE = 6.0
        private val ALLOWED_CRANKS = setOf("wooden_crank_handle", "iron_crank_handle", "steel_crank_handle", "carbon_crank_handle")
    }

    override val tier: Int = 1
    override val activeProperty: net.minecraft.state.property.BooleanProperty = ManualKineticGeneratorBlock.ACTIVE

    private val inventory = DefaultedList.ofSize(1, ItemStack.EMPTY)
    private val syncedData = SyncedData(this)
    val sync = ManualKineticGeneratorSync(syncedData)

    private var pendingOutputKu: Int = 0

    private val playerLastUseTick = mutableMapOf<PlayerEntity, Long>()

    var clientTurnAngle: Float = 0f
        private set

    private val scanOffset: Int = (pos.asLong().toInt() and Int.MAX_VALUE) % 10

    constructor(pos: BlockPos, state: BlockState) : this(
        ManualKineticGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this

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
        val id = net.minecraft.registry.Registries.ITEM.getId(stack.item)
        return ALLOWED_CRANKS.contains(id.path)
    }

    fun onUse(player: PlayerEntity, hand: Hand, world: World, pos: BlockPos, state: BlockState): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS

        val heldStack = player.getStackInHand(hand)
        val crankStack = inventory[CRANK_SLOT]

        when {
            heldStack.item is CrankHandleItem && !crankStack.isEmpty -> {
                val extracted = crankStack.copy()
                inventory[CRANK_SLOT] = heldStack.split(1)
                markDirtyAndSync()
                if (!player.inventory.insertStack(extracted)) {
                    player.dropItem(extracted, false)
                }
                return ActionResult.SUCCESS
            }

            heldStack.item is CrankHandleItem && crankStack.isEmpty -> {
                inventory[CRANK_SLOT] = heldStack.split(1)
                markDirtyAndSync()
                return ActionResult.SUCCESS
            }

            heldStack.isEmpty && hasCrank() -> {
                playerLastUseTick[player] = world.time
                return ActionResult.SUCCESS
            }

            heldStack.isEmpty && !hasCrank() -> {
                player.sendMessage(Text.translatable("message.ic2_120.manual_kinetic_no_crank"), true)
                return ActionResult.SUCCESS
            }

            else -> return ActionResult.PASS
        }
    }

    private fun isPlayerActivelyUsing(player: PlayerEntity): Boolean {
        val lastTick = playerLastUseTick[player] ?: return false
        val world = world ?: return false
        if (world.time - lastTick > 6) return false
        val distance = player.pos.distanceTo(Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5))
        if (distance > MAX_USE_DISTANCE) return false
        return true
    }

    fun dropCrank() {
        val world = world ?: return
        if (world.isClient || inventory[CRANK_SLOT].isEmpty) return
        net.minecraft.util.ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), inventory[CRANK_SLOT])
        inventory[CRANK_SLOT] = ItemStack.EMPTY
        markDirty()
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) {
            if (sync.isTurning) {
                clientTurnAngle += 15f
                if (clientTurnAngle >= 360f) clientTurnAngle -= 360f
            }
            return
        }

        playerLastUseTick.entries.removeIf { (player, _) ->
            !player.isAlive || !isPlayerActivelyUsing(player)
        }

        val wasTurning = sync.isTurning
        val isTurning = playerLastUseTick.isNotEmpty() && hasCrank()

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
            markDirty()
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
            markDirty()
        }
        return extracted
    }

    private fun markDirtyAndSync() {
        markDirty()
        val world = world ?: return
        if (!world.isClient) {
            val state = world.getBlockState(pos)
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS)
            (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
        }
    }

    override fun size(): Int = 1

    override fun isEmpty(): Boolean = inventory[CRANK_SLOT].isEmpty

    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val result = Inventories.splitStack(inventory, slot, amount)
        if (!result.isEmpty) {
            markDirtyAndSync()
        }
        return result
    }

    override fun removeStack(slot: Int): ItemStack {
        val result = Inventories.removeStack(inventory, slot)
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

    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        pendingOutputKu = nbt.getInt("PendingKu").coerceAtLeast(0)
        inventory[CRANK_SLOT] = ItemStack.EMPTY
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        nbt.putInt("PendingKu", pendingOutputKu)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
    }

    override fun toInitialChunkDataNbt(): NbtCompound = createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)
}