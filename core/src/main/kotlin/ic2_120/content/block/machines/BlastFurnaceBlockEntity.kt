package ic2_120.content.block.machines

import ic2_120.Ic2_120
import ic2_120.content.block.BlastFurnaceBlock
import ic2_120.content.heat.IHeatConsumer
import ic2_120.content.item.AirCell
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipe
import ic2_120.content.recipes.blastfurnace.BlastFurnaceRecipeSerializer
import ic2_120.content.recipes.getRecipeType
import ic2_120.content.screen.BlastFurnaceScreenHandler
import ic2_120.content.sync.BlastFurnaceSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
import ic2_120.content.upgrade.FluidPipeUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IFluidPipeUpgradeSupport
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.ModMachineRecipeBinding
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.RecipeManager
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 高炉方块实体。
 *
 * 从背面接收热量（HU），消耗铁质材料与压缩空气，产出钢锭和炉渣。
 *
 * 槽位：
 * - 输入：铁质材料（铁粉、粉碎铁矿石、纯净粉碎铁矿石、铁锭、铁矿石）
 * - 空气输入：压缩空气单元
 * - 输出：钢锭、炉渣
 * - 空单元输出：消耗空气后产生的空单元
 * - 升级槽：4 个
 */
@ModBlockEntity(block = BlastFurnaceBlock::class)
@ModMachineRecipeBinding(BlastFurnaceRecipeSerializer::class)
class BlastFurnaceBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatConsumerBlockEntityBase(type, pos, state), Inventory, IFluidPipeUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory {

    override val activeProperty: net.minecraft.state.property.BooleanProperty = BlastFurnaceBlock.ACTIVE

    override fun getInventory(): Inventory = this

    override val tier: Int = 1

    override var fluidPipeProviderEnabled: Boolean = false
    override var fluidPipeReceiverEnabled: Boolean = false
    override var fluidPipeProviderFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeReceiverFilter: net.minecraft.fluid.Fluid? = null
    override var fluidPipeProviderSide: Direction? = null
    override var fluidPipeReceiverSide: Direction? = null

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_AIR_INPUT = 1
        const val SLOT_OUTPUT_STEEL = 2
        const val SLOT_OUTPUT_SLAG = 3
        const val SLOT_OUTPUT_EMPTY = 4
        const val SLOT_UPGRADE_0 = 5
        const val SLOT_UPGRADE_1 = 6
        const val SLOT_UPGRADE_2 = 7
        const val SLOT_UPGRADE_3 = 8
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT_STEEL, SLOT_OUTPUT_SLAG, SLOT_OUTPUT_EMPTY)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT, SLOT_AIR_INPUT)
        const val INVENTORY_SIZE = 9
        private const val NBT_PREHEAT = "Preheat"
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
            ItemInsertRoute(intArrayOf(SLOT_AIR_INPUT), matcher = { !it.isEmpty && it.item is AirCell }),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isBlastRecipeInput(it) })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT_STEEL, SLOT_OUTPUT_SLAG, SLOT_OUTPUT_EMPTY),
        markDirty = { markDirty() }
    )
    val syncedData = SyncedData(this)
    val sync = BlastFurnaceSync(syncedData)

    private var preheat: Long = 0L
    private var heatReceivedLastTick: Boolean = false

    constructor(pos: BlockPos, state: BlockState) : this(
        BlastFurnaceBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = when (slot) {
        SLOT_INPUT -> isBlastRecipeInput(stack)
        SLOT_AIR_INPUT -> !stack.isEmpty && stack.item is AirCell
        SLOT_OUTPUT_STEEL, SLOT_OUTPUT_SLAG, SLOT_OUTPUT_EMPTY -> false
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        else -> false
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.blast_furnace")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        BlastFurnaceScreenHandler(syncId, playerInventory, this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        preheat = nbt.getLong(NBT_PREHEAT).coerceIn(0L, BlastFurnaceSync.PREHEAT_MAX.toLong())
        sync.preheat = preheat.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(NBT_PREHEAT, preheat)
    }

    override fun receiveHeatInternal(hu: Long): Long {
        if (hu <= 0L) return 0L
        heatReceivedLastTick = true  // 有热量输入即视为维持（即使已满无法再接收）
        val space = BlastFurnaceSync.PREHEAT_MAX.toLong() - preheat
        if (space <= 0L) return 0L
        val toAdd = minOf(hu, space)
        preheat += toAdd
        sync.preheat = preheat.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
        return toAdd
    }

    /**
     * 获取当前输入的配方
     */
    private fun getRecipeForInput(input: ItemStack): BlastFurnaceRecipe? {
        if (input.isEmpty) return null

        val inv = BlastFurnaceRecipe.Input(input)
        val recipeManager = world?.recipeManager ?: return null

        return recipeManager.getFirstMatch(getRecipeType<BlastFurnaceRecipe>(), inv, world ?: return null).orElse(null)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.preheat = preheat.toInt().coerceIn(0, Int.MAX_VALUE)

        val input = getStack(SLOT_INPUT)
        val outputSteel = getStack(SLOT_OUTPUT_STEEL)
        val outputSlag = getStack(SLOT_OUTPUT_SLAG)

        FluidPipeUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)

        val isFullyPreheated = preheat >= BlastFurnaceSync.PREHEAT_MAX

        // 无材料或输出满：重置进度，预热衰减
        if (input.isEmpty || !canAcceptOutput(outputSteel, outputSlag)) {
            if (sync.progress != 0) sync.progress = 0
            regressPreheat()
            setActiveState(world, pos, state, false)
            return
        }

        val recipe = getRecipeForInput(input)
        if (recipe == null) {
            if (sync.progress != 0) sync.progress = 0
            regressPreheat()
            setActiveState(world, pos, state, false)
            return
        }

        val steelOutput = BlastFurnaceRecipe.getSteelOutput(recipe)
        val slagOutput = BlastFurnaceRecipe.getSlagOutput(recipe)

        // 上一 tick 未收到热量：进度暂停，预热衰减
        if (!heatReceivedLastTick) {
            regressPreheat()
            setActiveState(world, pos, state, false)
            return
        }
        heatReceivedLastTick = false

        // 预热未满：不加工
        if (!isFullyPreheated) {
            setActiveState(world, pos, state, false)
            return
        }

        // 每 1000 ticks 需消耗 1 瓶压缩空气，在每段开始时立即消耗（炼钢开始第一 tick 就消耗第 1 瓶）
        val nextProgress = sync.progress + 1
        val needAirCell = (nextProgress - 1) % BlastFurnaceSync.TICKS_PER_AIR_CELL == 0
        if (needAirCell && !consumeOneAirCell()) {
            setActiveState(world, pos, state, false)
            return
        }

        // 加工完成
        if (nextProgress >= BlastFurnaceSync.PROGRESS_MAX) {
            input.decrement(1)
            if (outputSteel.isEmpty) setStack(SLOT_OUTPUT_STEEL, steelOutput)
            else outputSteel.increment(steelOutput.count)
            if (outputSlag.isEmpty) setStack(SLOT_OUTPUT_SLAG, slagOutput)
            else outputSlag.increment(slagOutput.count)
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            return
        }

        sync.progress = nextProgress
        markDirty()
        setActiveState(world, pos, state, true)
    }

    /** 从空气槽消耗 1 瓶压缩空气，空单元放入输出槽。成功返回 true。 */
    private fun consumeOneAirCell(): Boolean {
        val airSlot = getStack(SLOT_AIR_INPUT)
        if (airSlot.isEmpty || airSlot.item !is AirCell) return false

        val emptySlot = getStack(SLOT_OUTPUT_EMPTY)
        val emptyCell = ItemStack(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "empty_cell")))
        val canAcceptEmpty = emptySlot.isEmpty ||
            (ItemStack.areItemsEqual(emptySlot, emptyCell) && emptySlot.count + 1 <= emptyCell.maxCount)
        if (!canAcceptEmpty) return false

        airSlot.decrement(1)
        if (airSlot.isEmpty) setStack(SLOT_AIR_INPUT, ItemStack.EMPTY)
        if (emptySlot.isEmpty) setStack(SLOT_OUTPUT_EMPTY, emptyCell)
        else emptySlot.increment(1)
        return true
    }

    private fun regressPreheat() {
        if (preheat > 0L) {
            preheat = (preheat - BlastFurnaceSync.PREHEAT_REGRESS_PER_TICK).coerceAtLeast(0L)
            sync.preheat = preheat.toInt().coerceIn(0, Int.MAX_VALUE)
            markDirty()
        }
    }

    private fun hasValidHeatSource(): Boolean {
        val w = world ?: return false
        val myFace = getHeatTransferFace()
        val neighborPos = pos.offset(myFace)
        val neighbor = w.getBlockEntity(neighborPos) as? ic2_120.content.heat.IHeatNode ?: return false
        val neighborFace = neighbor.getHeatTransferFace()
        return neighborFace == myFace.opposite
    }

    private fun canAcceptOutput(steel: ItemStack, slag: ItemStack): Boolean {
        val recipe = getRecipeForInput(getStack(SLOT_INPUT)) ?: return true
        val steelOut = BlastFurnaceRecipe.getSteelOutput(recipe)
        val slagOut = BlastFurnaceRecipe.getSlagOutput(recipe)
        val steelOk = steel.isEmpty || (ItemStack.areItemsEqual(steel, steelOut) && steel.count + steelOut.count <= steel.maxCount)
        val slagOk = slag.isEmpty || (ItemStack.areItemsEqual(slag, slagOut) && slag.count + slagOut.count <= slag.maxCount)
        return steelOk && slagOk
    }

    private fun isBlastRecipeInput(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return getRecipeForInput(stack) != null
    }
}
