package ic2_120.content.block.machines

import ic2_120.content.block.CokeKilnBlock
import ic2_120.content.block.CokeKilnHatchBlock
import ic2_120.content.screen.CokeKilnScreenHandler
import ic2_120.content.sync.CokeKilnSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.item.Coke
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.ItemTags
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

@ModBlockEntity(block = CokeKilnBlock::class)
class CokeKilnBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Container, IRoutedSidedInventory, ExtendedScreenHandlerFactory {

    companion object {
        private const val SLOT_INPUT = 0
        private const val SLOT_OUTPUT = 1
        const val INVENTORY_SIZE = 2
        private const val PROCESS_TICKS = 1800
        private const val NBT_PROGRESS = "Progress"

        fun markKilnsDirtyAround(world: Level, origin: BlockPos) {
            for (dx in -2..2) {
                for (dy in -2..2) {
                    for (dz in -2..2) {
                        val p = origin.add(dx, dy, dz)
                        val be = world.getBlockEntity(p) as? CokeKilnBlockEntity ?: continue
                        be.markStructureDirty()
                    }
                }
            }
        }
    }

    private val inventory = NonNullList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    private val peatOreItem by lazy { BuiltInRegistries.ITEM.get(ResourceLocation.of("ic2_120", "peat_ore")) }

    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { getRecipe(it) != null })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        markDirty = { markDirty() }
    )

    override val routedItemStorage get() = itemStorage
    private var progress = 0
    private var structureDirty = true
    private var cachedStructureValid = false
    private var cachedGratePos: BlockPos? = null
    val syncedData = SyncedData()
    val sync = CokeKilnSync(syncedData)

    private data class Recipe(val output: ItemStack, val creosoteMb: Long)

    constructor(pos: BlockPos, state: BlockState) : this(CokeKilnBlockEntity::class.type(), pos, state)

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        refreshStructureCacheIfNeeded(world, pos)
        sync.structureValid = if (cachedStructureValid) 1 else 0
        sync.progress = progress
        sync.fluidAmount = 0
        sync.fluidRawId = -1
        val grateBe = getCachedGrateEntity(world)
        if (grateBe != null) {
            sync.fluidAmount = grateBe.getStoredAmount().toInt()
            sync.fluidRawId = grateBe.getStoredFluidRawId()
        }
        val recipe = getRecipe(getStack(SLOT_INPUT))
        if (recipe == null || !cachedStructureValid || !canOutput(recipe) || !hasTankSpace(recipe)) {
            progress = 0
            sync.progress = 0
            return
        }
        progress++
        if (progress < PROCESS_TICKS) return
        progress = 0
        sync.progress = 0
        val input = getStack(SLOT_INPUT)
        input.decrement(1)
        if (input.isEmpty) setStack(SLOT_INPUT, ItemStack.EMPTY)
        val out = getStack(SLOT_OUTPUT)
        if (out.isEmpty) setStack(SLOT_OUTPUT, recipe.output.copy())
        else out.increment(recipe.output.count)
        val grate = getCachedGrateEntity(world) ?: return
        grate.insertDroplets(recipe.creosoteMb * FluidConstants.BUCKET / 1000L)
        markDirty()
    }

    override fun writeScreenOpeningData(player: net.minecraft.server.level.ServerPlayer, buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.coke_kiln")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        CokeKilnScreenHandler(syncId, playerInventory, this, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData, itemStorage)

    fun tryInteractItem(player: Player, hand: InteractionHand): InteractionResult {
        val handStack = player.getStackInHand(hand)
        val out = getStack(SLOT_OUTPUT)
        if (!out.isEmpty) {
            if (handStack.isEmpty) {
                player.setStackInHand(hand, out.copy())
                setStack(SLOT_OUTPUT, ItemStack.EMPTY)
                return InteractionResult.SUCCESS
            }
            if (ItemStack.canCombine(handStack, out) && handStack.count + out.count <= handStack.maxCount) {
                handStack.increment(out.count)
                setStack(SLOT_OUTPUT, ItemStack.EMPTY)
                return InteractionResult.SUCCESS
            }
        }
        if (!handStack.isEmpty && getRecipe(handStack) != null) {
            val inSlot = getStack(SLOT_INPUT)
            if (inSlot.isEmpty) {
                val moved = handStack.copy()
                moved.count = 1
                setStack(SLOT_INPUT, moved)
                if (!player.abilities.creativeMode) handStack.decrement(1)
                return InteractionResult.SUCCESS
            }
        }
        val center = findKilnCenter(world ?: return InteractionResult.PASS, pos)
        player.sendMessage(
            Component.translatable(
                if (center != null) "message.ic2_120.coke_kiln.structure_ok" else "message.ic2_120.coke_kiln.structure_invalid"
            ),
            true
        )
        return InteractionResult.SUCCESS
    }

    private fun getRecipe(input: ItemStack): Recipe? {
        if (input.isEmpty) return null
        if (input.isIn(ItemTags.LOGS)) return Recipe(ItemStack(Items.CHARCOAL), 250)
        if (input.isOf(Items.COAL)) return Recipe(ItemStack(Coke::class.instance()), 500)
        if (input.isOf(peatOreItem)) return Recipe(ItemStack(Items.COAL), 125)
        return null
    }

    private fun canOutput(recipe: Recipe): Boolean {
        val out = getStack(SLOT_OUTPUT)
        if (out.isEmpty) return true
        return ItemStack.canCombine(out, recipe.output) && out.count + recipe.output.count <= out.maxCount
    }

    private fun hasTankSpace(recipe: Recipe): Boolean {
        val need = recipe.creosoteMb * FluidConstants.BUCKET / 1000L
        val grate = getCachedGrateEntity(world ?: return false) ?: return false
        return grate.canAcceptDroplets(need)
    }

    fun markStructureDirty() {
        structureDirty = true
    }

    private fun findKilnCenter(world: Level, kilnPos: BlockPos): BlockPos? {
        for (dir in Direction.Type.HORIZONTAL) {
            val center = kilnPos.offset(dir)
            val hatchPos = center.up()
            if (world.getBlockState(hatchPos).block is CokeKilnHatchBlock &&
                CokeKilnHatchBlock.isValidCokeKilnStructure(world, hatchPos)
            ) {
                return center
            }
        }
        return null
    }

    private fun refreshStructureCacheIfNeeded(world: Level, kilnPos: BlockPos) {
        if (!structureDirty) return
        structureDirty = false
        val center = findKilnCenter(world, kilnPos)
        if (center == null) {
            cachedStructureValid = false
            cachedGratePos = null
            return
        }
        cachedStructureValid = CokeKilnHatchBlock.isValidCokeKilnStructure(world, center.up())
        cachedGratePos = if (cachedStructureValid) center.down() else null
    }

    private fun getCachedGrateEntity(world: Level): CokeKilnGrateBlockEntity? {
        val gratePos = cachedGratePos ?: return null
        return world.getBlockEntity(gratePos) as? CokeKilnGrateBlockEntity
    }

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = ContainerHelper.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = ContainerHelper.removeStack(inventory, slot)
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }
    override fun canPlayerUse(player: Player): Boolean = Container.canPlayerUse(this, player)
    override fun clear() = inventory.clear()

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> getRecipe(stack) != null
        SLOT_OUTPUT -> false
        else -> false
    }

    override fun writeNbt(nbt: CompoundTag) {
        super.writeNbt(nbt)
        ContainerHelper.writeNbt(nbt, inventory)
        nbt.putInt(NBT_PROGRESS, progress)
        syncedData.writeNbt(nbt)
    }

    override fun readNbt(nbt: CompoundTag) {
        super.readNbt(nbt)
        ContainerHelper.readNbt(nbt, inventory)
        progress = nbt.getInt(NBT_PROGRESS)
        syncedData.readNbt(nbt)
        sync.progress = progress
        sync.structureValid = if (cachedStructureValid) 1 else 0
    }
}
