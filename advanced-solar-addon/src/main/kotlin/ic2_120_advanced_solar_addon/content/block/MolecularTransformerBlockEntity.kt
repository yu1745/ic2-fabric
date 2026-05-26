package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.sync.MolecularTransformerSync
import ic2_120_advanced_solar_addon.content.screen.MolecularTransformerScreenHandler
import ic2_120_advanced_solar_addon.content.recipe.MTRecipes
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.machines.MachineBlockEntity
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
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
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
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
        private const val LIGHT_TIMEOUT_TICKS = 60 // 3 seconds without energy -> stop light
    }

    override val tier: Int = TIER
    override val activeProperty = MolecularTransformerBlock.ACTIVE

    @Suppress("unused")
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = MolecularTransformerSync(
        schema = syncedData,
        tier = tier,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time },
        canAcceptEnergy = { currentRecipe != null },
        getRemainingEnergyNeeded = {
            val recipe = currentRecipe ?: return@MolecularTransformerSync 0L
            (recipe.energy - energyUsed).coerceAtLeast(0)
        }
    )

    val inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(MolecularTransformerBlock.INVENTORY_SIZE, ItemStack.EMPTY)

    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { 64 },
        slotValidator = { slot, stack -> isValidForSlot(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(
                slotIndices = intArrayOf(MolecularTransformerBlock.INPUT_SLOT),
                matcher = { stack -> MTRecipes.findRecipe(stack) != null }
            )
        ),
        extractSlots = intArrayOf(MolecularTransformerBlock.OUTPUT_SLOT),
        markDirty = { markDirty() }
    )

    private var energyUsed: Long = 0
    private var currentRecipe: MTRecipes.MTRecipe? = null
    private var consumedInputItem: ItemStack = ItemStack.EMPTY
    private var noEnergyTicks: Int = 0
    private var isLit: Boolean = false

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

    private fun isValidForSlot(slot: Int, stack: ItemStack): Boolean = when (slot) {
        MolecularTransformerBlock.INPUT_SLOT -> MTRecipes.findRecipe(stack) != null
        else -> false
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        val inputStack = inventory[MolecularTransformerBlock.INPUT_SLOT]
        val outputStack = inventory[MolecularTransformerBlock.OUTPUT_SLOT]

        // Start new recipe: immediately consume 1 input item
        if (currentRecipe == null && !inputStack.isEmpty) {
            val recipe = MTRecipes.findRecipe(inputStack)
            if (recipe != null && canOutput(recipe.output, outputStack)) {
                currentRecipe = recipe
                energyUsed = 0
                // Consume 1 item from input immediately
                consumedInputItem = inputStack.copyWithCount(1)
                inputStack.decrement(1)
                if (inputStack.isEmpty) {
                    inventory[MolecularTransformerBlock.INPUT_SLOT] = ItemStack.EMPTY
                }
                sync.inputItemId = Item.getRawId(consumedInputItem.item)
                sync.outputItemId = Item.getRawId(recipe.output.item)
            }
        }

        val recipe = currentRecipe
        var energyConsumedThisTick = false
        if (recipe != null) {
            // Validate: if output blocked, reset
            if (!canOutput(recipe.output, outputStack)) {
                resetRecipe(world, pos, state)
                markDirty()
                return
            }

            val energyNeeded = recipe.energy - energyUsed
            if (sync.amount > 0 && energyNeeded > 0) {
                val energyToUse = minOf(sync.amount, energyNeeded)
                sync.consumeEnergy(energyToUse)
                energyUsed += energyToUse
                energyConsumedThisTick = true
            }

            if (energyUsed >= recipe.energy) {
                // Output product
                if (outputStack.isEmpty) {
                    inventory[MolecularTransformerBlock.OUTPUT_SLOT] = recipe.output.copy()
                } else {
                    inventory[MolecularTransformerBlock.OUTPUT_SLOT].increment(recipe.output.count)
                }
                resetRecipe(world, pos, state)
            }
        }

        // Light management: 3-second timeout after last energy input
        if (recipe != null) {
            if (energyConsumedThisTick) {
                noEnergyTicks = 0
            } else {
                noEnergyTicks++
            }
            val shouldBeLit = noEnergyTicks < LIGHT_TIMEOUT_TICKS
            if (shouldBeLit != isLit) {
                isLit = shouldBeLit
                world.setBlockState(pos, state.with(MolecularTransformerBlock.LIT, isLit))
            }
        } else if (isLit) {
            isLit = false
            world.setBlockState(pos, state.with(MolecularTransformerBlock.LIT, false))
        }

        // Update sync data
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.progress = energyUsed.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        sync.requiredEnergy = (currentRecipe?.energy ?: 0).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        if (currentRecipe == null) {
            sync.inputItemId = 0
            sync.outputItemId = 0
        }

        sync.syncCurrentTickFlow()
        setActiveState(world, pos, state, currentRecipe != null)
        markDirty()
    }

    private fun resetRecipe(world: World, pos: BlockPos, state: BlockState) {
        currentRecipe = null
        energyUsed = 0
        consumedInputItem = ItemStack.EMPTY
        noEnergyTicks = 0
        sync.inputItemId = 0
        sync.outputItemId = 0
        sync.requiredEnergy = 0
        sync.progress = 0
        if (isLit) {
            isLit = false
            world.setBlockState(pos, state.with(MolecularTransformerBlock.LIT, false))
        }
    }

    private fun canOutput(output: ItemStack, currentOutput: ItemStack): Boolean {
        if (currentOutput.isEmpty) return true
        if (!ItemStack.canCombine(currentOutput, output)) return false
        return currentOutput.count + output.count <= currentOutput.maxCount
    }

    // ExtendedScreenHandlerFactory
    override fun getDisplayName(): Text = Text.translatable("block.ic2_120_advanced_solar_addon.molecular_transformer")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        MolecularTransformerScreenHandler(
            syncId, playerInventory, this,
            ScreenHandlerContext.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        energyUsed = nbt.getLong("energyUsed")
        consumedInputItem = ItemStack.fromNbt(nbt.getCompound("consumedInput"))
        noEnergyTicks = nbt.getInt("noEnergyTicks")
        isLit = nbt.getBoolean("isLit")
        sync.restoreEnergy(nbt.getLong(MolecularTransformerSync.NBT_ENERGY).coerceIn(0L, sync.capacity))
        syncedData.readNbt(nbt)
        // Restore recipe on load
        if (energyUsed > 0 && !consumedInputItem.isEmpty) {
            currentRecipe = MTRecipes.findRecipe(consumedInputItem)
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        nbt.putLong("energyUsed", energyUsed)
        nbt.put("consumedInput", consumedInputItem.writeNbt(NbtCompound()))
        nbt.putInt("noEnergyTicks", noEnergyTicks)
        nbt.putBoolean("isLit", isLit)
        nbt.putLong(MolecularTransformerSync.NBT_ENERGY, sync.amount)
        syncedData.writeNbt(nbt)
    }
}
