package ic2_120.content.block.machines

import ic2_120.content.block.CokeKilnBlock
import ic2_120.content.block.CokeKilnHatchBlock
import ic2_120.content.screen.CokeKilnScreenHandler
import ic2_120.content.sync.CokeKilnSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.item.Coke
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.instance
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound

import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.RegistryWrapper
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

@ModBlockEntity(block = CokeKilnBlock::class)
class CokeKilnBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, ExtendedScreenHandlerFactory {

    companion object {
        private const val SLOT_INPUT = 0
        private const val SLOT_OUTPUT = 1
        private const val INVENTORY_SIZE = 2
        private const val PROCESS_TICKS = 1800
        private const val NBT_PROGRESS = "Progress"

        fun markKilnsDirtyAround(world: World, origin: BlockPos) {
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

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { getRecipe(it) != null }, maxPerSlot = 1)
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        markDirty = { markDirty() }
    )
    private var progress = 0
    private var structureDirty = true
    private var cachedStructureValid = false
    private var cachedGratePos: BlockPos? = null
    val syncedData = SyncedData(this)
    val sync = CokeKilnSync(syncedData)

    private data class Recipe(val output: ItemStack, val creosoteMb: Long)

    constructor(pos: BlockPos, state: BlockState) : this(CokeKilnBlockEntity::class.type(), pos, state)

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        refreshStructureCacheIfNeeded(world, pos)
        sync.structureValid = if (cachedStructureValid) 1 else 0
        sync.progress = progress
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

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.coke_kiln")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CokeKilnScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    fun tryInteractItem(player: PlayerEntity, hand: Hand): ActionResult {
        val handStack = player.getStackInHand(hand)
        val out = getStack(SLOT_OUTPUT)
        if (!out.isEmpty) {
            if (handStack.isEmpty) {
                player.setStackInHand(hand, out.copy())
                setStack(SLOT_OUTPUT, ItemStack.EMPTY)
                return ActionResult.SUCCESS
            }
            if (ItemStack.canCombine(handStack, out) && handStack.count + out.count <= handStack.maxCount) {
                handStack.increment(out.count)
                setStack(SLOT_OUTPUT, ItemStack.EMPTY)
                return ActionResult.SUCCESS
            }
        }
        if (!handStack.isEmpty && getRecipe(handStack) != null) {
            val inSlot = getStack(SLOT_INPUT)
            if (inSlot.isEmpty) {
                val moved = handStack.copy()
                moved.count = 1
                setStack(SLOT_INPUT, moved)
                if (!player.abilities.creativeMode) handStack.decrement(1)
                return ActionResult.SUCCESS
            }
        }
        val center = findKilnCenter(world ?: return ActionResult.PASS, pos)
        player.sendMessage(
            Text.translatable(
                if (center != null) "message.ic2_120.coke_kiln.structure_ok" else "message.ic2_120.coke_kiln.structure_invalid"
            ),
            true
        )
        return ActionResult.SUCCESS
    }

    private fun getRecipe(input: ItemStack): Recipe? {
        if (input.isEmpty) return null
        if (input.isIn(ItemTags.LOGS)) return Recipe(ItemStack(Items.CHARCOAL), 250)
        if (input.isOf(Items.COAL)) return Recipe(ItemStack(Coke::class.instance()), 500)
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

    private fun findKilnCenter(world: World, kilnPos: BlockPos): BlockPos? {
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

    private fun refreshStructureCacheIfNeeded(world: World, kilnPos: BlockPos) {
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

    private fun getCachedGrateEntity(world: World): CokeKilnGrateBlockEntity? {
        val gratePos = cachedGratePos ?: return null
        return world.getBlockEntity(gratePos) as? CokeKilnGrateBlockEntity
    }

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)
    override fun clear() = inventory.clear()

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> getRecipe(stack) != null
        SLOT_OUTPUT -> false
        else -> false
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory)
        nbt.putInt(NBT_PROGRESS, progress)
        syncedData.writeNbt(nbt)
    }

    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory)
        progress = nbt.getInt(NBT_PROGRESS)
        syncedData.readNbt(nbt)
        sync.progress = progress
        sync.structureValid = if (cachedStructureValid) 1 else 0
    }
}
