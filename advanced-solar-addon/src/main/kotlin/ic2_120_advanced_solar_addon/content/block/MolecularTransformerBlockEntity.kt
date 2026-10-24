package ic2_120_advanced_solar_addon.content.block

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import net.minecraft.core.HolderLookup
import ic2_120_advanced_solar_addon.content.sync.MolecularTransformerSync
import ic2_120_advanced_solar_addon.content.screen.MolecularTransformerScreenHandler
import ic2_120_advanced_solar_addon.content.recipe.MTRecipes
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.machines.MachineBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.type
import ic2_120.content.AdjacentEnergyTransferComponent
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory

@ModBlockEntity(block = MolecularTransformerBlock::class)
class MolecularTransformerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(MolecularTransformerBlockEntity::class.type(), pos, state),
    Container, ITieredMachine, ExtendedScreenHandlerFactory<FriendlyByteBuf> {

    companion object {
        const val TIER = 10
        private const val LIGHT_TIMEOUT_TICKS = 60 // 3 seconds without energy -> stop light
    }

    override val tier: Int = TIER
    override val activeProperty = MolecularTransformerBlock.ACTIVE

    @Suppress("unused")
    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = MolecularTransformerSync(
        schema = syncedData,
        tier = tier,
        getFacing = { world?.getBlockState(pos)?.getValue(BlockStateProperties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time },
        canAcceptEnergy = { currentRecipe != null },
        getRemainingEnergyNeeded = {
            val recipe = currentRecipe ?: return@MolecularTransformerSync 0L
            (recipe.energy - energyUsed).coerceAtLeast(0)
        }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)

    val inventory: NonNullList<ItemStack> = NonNullList.withSize(MolecularTransformerBlock.INVENTORY_SIZE, ItemStack.EMPTY)

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
        setChanged = { setChanged() }
    )

    private var energyUsed: Long = 0
    private var currentRecipe: MTRecipes.MTRecipe? = null
    private var consumedInputItem: ItemStack = ItemStack.EMPTY
    private var noEnergyTicks: Int = 0
    private var isLit: Boolean = false

    override fun getInventory(): Container = this

    // Container
    override fun getContainerSize(): Int = inventory.size
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setItem(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        setChanged()
    }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    private fun isValidForSlot(slot: Int, stack: ItemStack): Boolean = when (slot) {
        MolecularTransformerBlock.INPUT_SLOT -> MTRecipes.findRecipe(stack) != null
        else -> false
    }

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        adjacentEnergyTransfer.tick()

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
                inputStack.shrink(1)
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
                setChanged()
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
                    inventory[MolecularTransformerBlock.OUTPUT_SLOT].grow(recipe.output.count)
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
                world.setBlock(pos, state.setValue(MolecularTransformerBlock.LIT, isLit))
            }
        } else if (isLit) {
            isLit = false
            world.setBlock(pos, state.setValue(MolecularTransformerBlock.LIT, false))
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
        setChanged()
    }

    private fun resetRecipe(world: Level, pos: BlockPos, state: BlockState) {
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
            world.setBlock(pos, state.setValue(MolecularTransformerBlock.LIT, false))
        }
    }

    private fun canOutput(output: ItemStack, currentOutput: ItemStack): Boolean {
        if (currentOutput.isEmpty) return true
        if (!ItemStack.canCombine(currentOutput, output)) return false
        return currentOutput.count + output.count <= currentOutput.maxStackSize
    }

    // ExtendedScreenHandlerFactory<FriendlyByteBuf>
    override fun getDisplayName(): Component = Component.translatable("block.ic2_120_advanced_solar_addon.molecular_transformer")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        MolecularTransformerScreenHandler(
            syncId, playerInventory, this,
            ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.getContainerSize())
        return buf
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory)
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

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory)
        nbt.putLong("energyUsed", energyUsed)
        nbt.put("consumedInput", consumedInputItem.writeNbt(CompoundTag()))
        nbt.putInt("noEnergyTicks", noEnergyTicks)
        nbt.putBoolean("isLit", isLit)
        nbt.putLong(MolecularTransformerSync.NBT_ENERGY, sync.amount)
        syncedData.writeNbt(nbt)
    }
}
