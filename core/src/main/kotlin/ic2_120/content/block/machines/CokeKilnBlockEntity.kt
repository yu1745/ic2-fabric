package ic2_120.content.block.machines

import net.minecraft.core.HolderLookup

import net.minecraft.server.level.ServerPlayer

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world

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
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
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
) : BlockEntity(type, pos, state), Container, IRoutedSidedInventory, ExtendedScreenHandlerFactory<FriendlyByteBuf> {

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
                        val p = origin.offset(dx, dy, dz)
                        val be = world.getBlockEntity(p) as? CokeKilnBlockEntity ?: continue
                        be.markStructureDirty()
                    }
                }
            }
        }
    }

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    private val peatOreItem by lazy { BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2_120", "peat_ore")) }

    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { getRecipe(it) != null })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        setChanged = { setChanged() }
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
        val recipe = getRecipe(getItem(SLOT_INPUT))
        if (recipe == null || !cachedStructureValid || !canOutput(recipe) || !hasTankSpace(recipe)) {
            progress = 0
            sync.progress = 0
            return
        }
        progress++
        if (progress < PROCESS_TICKS) return
        progress = 0
        sync.progress = 0
        val input = getItem(SLOT_INPUT)
        input.shrink(1)
        if (input.isEmpty) setItem(SLOT_INPUT, ItemStack.EMPTY)
        val out = getItem(SLOT_OUTPUT)
        if (out.isEmpty) setItem(SLOT_OUTPUT, recipe.output.copy())
        else out.grow(recipe.output.count)
        val grate = getCachedGrateEntity(world) ?: return
        grate.insertDroplets(recipe.creosoteMb * FluidConstants.BUCKET / 1000L)
        setChanged()
    }

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.size)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.coke_kiln")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player?): AbstractContainerMenu =
        CokeKilnScreenHandler(syncId, playerInventory, this, net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos), syncedData, itemStorage)

    fun tryInteractItem(player: Player, hand: InteractionHand): InteractionResult {
        val handStack = player.getItemInHand(hand)
        val out = getItem(SLOT_OUTPUT)
        if (!out.isEmpty) {
            if (handStack.isEmpty) {
                player.setItemInHand(hand, out.copy())
                setItem(SLOT_OUTPUT, ItemStack.EMPTY)
                return InteractionResult.SUCCESS
            }
            if (ItemStack.isSameItemSameComponents(handStack, out) && handStack.count + out.count <= handStack.maxStackSize) {
                handStack.grow(out.count)
                setItem(SLOT_OUTPUT, ItemStack.EMPTY)
                return InteractionResult.SUCCESS
            }
        }
        if (!handStack.isEmpty && getRecipe(handStack) != null) {
            val inSlot = getItem(SLOT_INPUT)
            if (inSlot.isEmpty) {
                val moved = handStack.copy()
                moved.count = 1
                setItem(SLOT_INPUT, moved)
                if (!player.abilities.instabuild) handStack.shrink(1)
                return InteractionResult.SUCCESS
            }
        }
        val center = findKilnCenter(world ?: return InteractionResult.PASS, pos)
        player.displayClientMessage(
            Component.translatable(
                if (center != null) "message.ic2_120.coke_kiln.structure_ok" else "message.ic2_120.coke_kiln.structure_invalid"
            ),
            true
        )
        return InteractionResult.SUCCESS
    }

    private fun getRecipe(input: ItemStack): Recipe? {
        if (input.isEmpty) return null
        if (input.`is`(ItemTags.LOGS)) return Recipe(ItemStack(Items.CHARCOAL), 250)
        if (input.`is`(Items.COAL)) return Recipe(ItemStack(Coke::class.instance()), 500)
        if (input.`is`(peatOreItem)) return Recipe(ItemStack(Items.COAL), 125)
        return null
    }

    private fun canOutput(recipe: Recipe): Boolean {
        val out = getItem(SLOT_OUTPUT)
        if (out.isEmpty) return true
        return ItemStack.isSameItemSameComponents(out, recipe.output) && out.count + recipe.output.count <= out.maxStackSize
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
        for (dir in Direction.Plane.HORIZONTAL) {
            val center = kilnPos.relative(dir)
            val hatchPos = center.above()
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
        cachedStructureValid = CokeKilnHatchBlock.isValidCokeKilnStructure(world, center.above())
        cachedGratePos = if (cachedStructureValid) center.below() else null
    }

    private fun getCachedGrateEntity(world: Level): CokeKilnGrateBlockEntity? {
        val gratePos = cachedGratePos ?: return null
        return world.getBlockEntity(gratePos) as? CokeKilnGrateBlockEntity
    }

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun setItem(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE) stack.count = net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE
        setChanged()
    }
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)
    override fun clearContent() = inventory.clear()

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> getRecipe(stack) != null
        SLOT_OUTPUT -> false
        else -> false
    }

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper.saveAllItems(nbt, inventory, lookup)
        nbt.putInt(NBT_PROGRESS, progress)
        syncedData.writeNbt(nbt)
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper.loadAllItems(nbt, inventory, lookup)
        progress = nbt.getInt(NBT_PROGRESS)
        syncedData.readNbt(nbt)
        sync.progress = progress
        sync.structureValid = if (cachedStructureValid) 1 else 0
    }
}
