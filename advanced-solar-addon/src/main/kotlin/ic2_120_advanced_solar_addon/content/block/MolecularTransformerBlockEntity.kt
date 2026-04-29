package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.sync.MolecularTransformerSync
import ic2_120_advanced_solar_addon.content.screen.MolecularTransformerScreenHandler
import ic2_120_advanced_solar_addon.content.recipe.MTRecipes
import ic2_120.content.energy.EnergyTier
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.machines.MachineBlockEntity
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

@ModBlockEntity(block = MolecularTransformerBlock::class)
class MolecularTransformerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(MolecularTransformerBlockEntity::class.type(), pos, state),
    Inventory, ITieredMachine, ExtendedScreenHandlerFactory {

    companion object {
        const val TIER = 10
    }

    override val tier: Int = TIER
    private val maxConsumePerTick: Long = EnergyTier.euPerTickFromTier(tier)
    override val activeProperty = MolecularTransformerBlock.ACTIVE

    @Suppress("unused")
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = MolecularTransformerSync(
        schema = syncedData,
        tier = tier,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    val inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(MolecularTransformerBlock.INVENTORY_SIZE, ItemStack.EMPTY)

    private var energyUsed: Long = 0
    private var currentRecipe: MTRecipes.MTRecipe? = null

    override fun getInventory(): Inventory = this

    // Inventory
    override fun size(): Int = inventory.size
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        val inputStack = inventory[MolecularTransformerBlock.INPUT_SLOT]
        val outputStack = inventory[MolecularTransformerBlock.OUTPUT_SLOT]

        if (currentRecipe == null && !inputStack.isEmpty) {
            val recipe = MTRecipes.findRecipe(inputStack)
            if (recipe != null && canOutput(recipe.output, outputStack)) {
                currentRecipe = recipe
                energyUsed = 0
            }
        }

        val recipe = currentRecipe
        if (recipe != null) {
            // Recipe may become invalid while processing (input changed/output blocked).
            if (!canOutput(recipe.output, outputStack) ||
                inputStack.isEmpty ||
                !ItemStack.canCombine(recipe.input, inputStack) ||
                inputStack.count < recipe.input.count
            ) {
                currentRecipe = null
                energyUsed = 0
                setActiveState(world, pos, state, false)
                markDirty()
                return
            }

            val energyNeeded = recipe.energy - energyUsed
            val energyToUse = minOf(sync.amount, energyNeeded, maxConsumePerTick)
            sync.consumeEnergy(energyToUse)
            energyUsed += energyToUse

            if (energyUsed >= recipe.energy) {
                if (inventory[MolecularTransformerBlock.OUTPUT_SLOT].isEmpty) {
                    inventory[MolecularTransformerBlock.OUTPUT_SLOT] = recipe.output.copy()
                } else {
                    inventory[MolecularTransformerBlock.OUTPUT_SLOT].increment(recipe.output.count)
                }

                inputStack.decrement(recipe.input.count)
                if (inputStack.isEmpty) {
                    inventory[MolecularTransformerBlock.INPUT_SLOT] = ItemStack.EMPTY
                }

                currentRecipe = null
                energyUsed = 0
            }

            markDirty()
        }

        // Update sync data
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.progress = energyUsed.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        sync.requiredEnergy = getRequiredEnergy().toInt()

        sync.syncCurrentTickFlow()
        setActiveState(world, pos, state, currentRecipe != null)
    }

    private fun canOutput(output: ItemStack, currentOutput: ItemStack): Boolean {
        if (currentOutput.isEmpty) return true
        if (!ItemStack.canCombine(currentOutput, output)) return false
        return currentOutput.count + output.count <= currentOutput.maxCount
    }

    private fun getRequiredEnergy(): Long = currentRecipe?.energy ?: 0

    // ExtendedScreenHandlerFactory
    override fun getDisplayName(): Text = Text.translatable("block.ic2_120_advanced_solar_addon.molecular_transformer")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MolecularTransformerScreenHandler(
            syncId, playerInventory, this,
            ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        energyUsed = nbt.getLong("energyUsed")
        sync.restoreEnergy(nbt.getLong(MolecularTransformerSync.NBT_ENERGY).coerceIn(0L, sync.capacity))
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        nbt.putLong("energyUsed", energyUsed)
        nbt.putLong(MolecularTransformerSync.NBT_ENERGY, sync.amount)
        syncedData.writeNbt(nbt)
    }
}
