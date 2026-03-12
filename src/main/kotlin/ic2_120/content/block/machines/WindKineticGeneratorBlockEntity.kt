package ic2_120.content.block.machines

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.screen.WindKineticGeneratorScreenHandler
import ic2_120.registry.annotation.ModBlockEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos

@ModBlockEntity(block = WindKineticGeneratorBlock::class)
class WindKineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    companion object {
        const val ROTOR_SLOT = 0
        private val ALLOWED_ROTORS = setOf("wooden_rotor", "iron_rotor", "steel_rotor", "carbon_rotor")
    }

    override val tier: Int = 1
    private val inventory = DefaultedList.ofSize(1, ItemStack.EMPTY)

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(WindKineticGeneratorBlockEntity::class),
        pos,
        state
    )

    override fun getInventory(): Inventory = this

    fun getRotorStack(): ItemStack = inventory[ROTOR_SLOT]

    fun onUse(player: PlayerEntity, hand: Hand): ActionResult {
        val held = player.getStackInHand(hand)
        val rotor = inventory[ROTOR_SLOT]

        // 空手右键取出转子
        if (held.isEmpty) {
            if (rotor.isEmpty) return ActionResult.PASS
            val extracted = rotor.copy()
            inventory[ROTOR_SLOT] = ItemStack.EMPTY
            markDirtyAndSync()
            if (!player.giveItemStack(extracted)) {
                player.dropItem(extracted, false)
            }
            return ActionResult.SUCCESS
        }

        // 手持非转子物品：不处理
        if (!isAllowedRotor(held)) return ActionResult.PASS

        // 已有转子时不覆盖
        if (!rotor.isEmpty) return ActionResult.PASS

        val inserted = held.copy()
        inserted.count = 1
        inventory[ROTOR_SLOT] = inserted
        if (!player.abilities.creativeMode) {
            held.decrement(1)
        }
        markDirtyAndSync()
        return ActionResult.SUCCESS
    }

    private fun isAllowedRotor(stack: ItemStack): Boolean {
        val id = Registries.ITEM.getId(stack.item)
        return ALLOWED_ROTORS.contains(id.path)
    }

    private fun markDirtyAndSync() {
        markDirty()
        val world = world ?: return
        if (!world.isClient) {
            val state = world.getBlockState(pos)
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS)
        }
    }

    override fun size(): Int = 1

    override fun isEmpty(): Boolean = inventory[ROTOR_SLOT].isEmpty

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
        if (slot != ROTOR_SLOT) return
        if (stack.isEmpty) {
            inventory[ROTOR_SLOT] = ItemStack.EMPTY
        } else if (isAllowedRotor(stack)) {
            val copy = stack.copy()
            copy.count = 1
            inventory[ROTOR_SLOT] = copy
        }
        markDirtyAndSync()
    }

    override fun clear() {
        inventory[ROTOR_SLOT] = ItemStack.EMPTY
        markDirtyAndSync()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
    }

    override fun toInitialChunkDataNbt(): NbtCompound = createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(0)
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.wind_kinetic_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        WindKineticGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            ScreenHandlerContext.create(world!!, pos),
            ArrayPropertyDelegate(0)
        )
}
