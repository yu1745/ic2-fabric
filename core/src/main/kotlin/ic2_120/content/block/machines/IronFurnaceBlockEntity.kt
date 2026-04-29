package ic2_120.content.block.machines

import ic2_120.content.block.IronFurnaceBlock
import ic2_120.content.screen.IronFurnaceScreenHandler
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.sync.IronFurnaceSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import net.fabricmc.fabric.api.registry.FuelRegistry
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.registry.annotation.RegisterItemStorage
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.RecipeType
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 铁炉方块实体。
 *
 * 铁炉特点：
 * - 烧制速度比原版熔炉快 20%（8秒 vs 10秒）
 * - 燃料效率更高：煤炭可烧制 10 个物品（原版 8 个）
 * - 岩浆效率低：岩浆只能烧制 12.5 个物品（原版 100 个）
 * - 工作时不发光
 */
@ModBlockEntity(block = IronFurnaceBlock::class)
class IronFurnaceBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, ExtendedScreenHandlerFactory {

    private val activeProperty: net.minecraft.state.property.BooleanProperty = IronFurnaceBlock.ACTIVE

    private val soundConfig: MachineSoundConfig = MachineSoundConfig.operate(
        soundId = "machine.furnace.iron.operate",
        volume = 0.5f,
        pitch = 1.0f,
        intervalTicks = 20
    )

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_FUEL = 1
        const val SLOT_OUTPUT = 2
        const val INVENTORY_SIZE = 3

        /** 铁炉燃料倍数：煤炭在铁炉中燃烧时间是原版的 1.25 倍 */
        private const val FUEL_MULTIPLIER = 1.25

        /** 岩浆在铁炉中的燃烧时间（tick） */
        private const val LAVA_BURN_TIME = 2000  // 12.5 个物品 * 160 tick/物品
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    private var storedExperience: Float = 0f
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isSmeltingInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_FUEL), matcher = { getFuelTime(it) > 0 })
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        markDirty = { markDirty() }
    )

    val syncedData = SyncedData(this)
    val sync = IronFurnaceSync(syncedData)

    constructor(pos: BlockPos, state: BlockState) : this(
        IronFurnaceBlockEntity::class.type(),
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
    override fun canPlayerUse(player: PlayerEntity): Boolean =
        Inventory.canPlayerUse(this, player)

    override fun isValid(slot: Int, stack: ItemStack): Boolean = canPlaceInSlot(slot, stack)

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text =
        Text.translatable("block.ic2_120.iron_furnace")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        IronFurnaceScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.burnTime = nbt.getInt(IronFurnaceSync.NBT_BURN_TIME)
        sync.totalBurnTime = nbt.getInt(IronFurnaceSync.NBT_TOTAL_BURN_TIME)
        sync.cookTime = nbt.getInt(IronFurnaceSync.NBT_COOK_TIME)
        storedExperience = nbt.getFloat(FurnaceExperienceHelper.NBT_EXPERIENCE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putInt(IronFurnaceSync.NBT_BURN_TIME, sync.burnTime)
        nbt.putInt(IronFurnaceSync.NBT_TOTAL_BURN_TIME, sync.totalBurnTime)
        nbt.putInt(IronFurnaceSync.NBT_COOK_TIME, sync.cookTime)
        nbt.putFloat(FurnaceExperienceHelper.NBT_EXPERIENCE, storedExperience)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        val fuelItem = getStack(SLOT_FUEL)
        val inputItem = getStack(SLOT_INPUT)
        val outputItem = getStack(SLOT_OUTPUT)

        // 检查是否有有效的烧制配方
        val hasRecipe = if (!inputItem.isEmpty) {
            val inputInv = SimpleInventory(1).apply { setStack(0, inputItem) }
            val match = world.recipeManager.getFirstMatch(RecipeType.SMELTING, inputInv, world)
            !match.isEmpty
        } else false

        // 检查输出槽是否可以接收物品
        val canAcceptOutput = if (hasRecipe) {
            val inputInv = SimpleInventory(1).apply { setStack(0, inputItem) }
            val recipe = world.recipeManager.getFirstMatch(RecipeType.SMELTING, inputInv, world).get()
            val result = recipe.getOutput(world.registryManager)
            outputItem.isEmpty ||
                (ItemStack.areItemsEqual(outputItem, result) &&
                 outputItem.count + result.count <= result.maxCount)
        } else false

        // 燃料燃烧逻辑
        val isBurning = sync.burnTime > 0
        if (!isBurning && hasRecipe && canAcceptOutput && !fuelItem.isEmpty) {
            // 尝试点燃新燃料
            val fuelTicks = getFuelTime(fuelItem)
            if (fuelTicks > 0) {
                sync.totalBurnTime = fuelTicks
                sync.burnTime = fuelTicks
                val remainder = fuelItem.item.getRecipeRemainder(fuelItem)
                fuelItem.decrement(1)
                if (fuelItem.isEmpty && !remainder.isEmpty) {
                    setStack(SLOT_FUEL, remainder.copy())
                }
                markDirty()
            }
        }

        // 烧制逻辑
        if (isBurning && hasRecipe && canAcceptOutput) {
            sync.cookTime++

            // 烧制完成
            if (sync.cookTime >= IronFurnaceSync.COOK_TIME_MAX) {
                val inputInv = SimpleInventory(1).apply { setStack(0, inputItem) }
                val recipe = world.recipeManager.getFirstMatch(RecipeType.SMELTING, inputInv, world).get()
                val result = recipe.getOutput(world.registryManager).copy()

                inputItem.decrement(1)
                if (outputItem.isEmpty) {
                    setStack(SLOT_OUTPUT, result)
                } else {
                    outputItem.increment(result.count)
                }
                storedExperience += FurnaceExperienceHelper.getExperienceFromRecipe(recipe)
                sync.cookTime = 0
                markDirty()
            }
        } else if (!hasRecipe || !canAcceptOutput) {
            // 无法继续烧制，重置进度
            sync.cookTime = 0
        }

        // 消耗燃烧时间
        if (sync.burnTime > 0) {
            sync.burnTime--
            if (sync.burnTime == 0) {
                sync.totalBurnTime = 0
            }
            markDirty()
        }

        // 同步经验显示值（×10 保留一位小数）
        sync.experienceDisplay = (storedExperience * 10).toInt()

        // 更新方块状态（active）
        val active = sync.burnTime > 0
        setActiveState(world, pos, state, active)
    }

    /**
     * 获取燃料在铁炉中的燃烧时间（tick）。
     * - 岩浆：固定 2000 tick（可烧制 12.5 个物品）
     * - 其他燃料：原版燃烧时间 * 1.25
     */
    private fun getFuelTime(stack: ItemStack): Int {
        if (stack.isEmpty) return 0

        // 检查是否是岩浆桶
        if (net.minecraft.item.Items.LAVA_BUCKET == stack.item) {
            return LAVA_BURN_TIME
        }

        // 获取原版熔炉燃烧时间
        val furnaceTicks = FuelRegistry.INSTANCE.get(stack.item) ?: return 0
        if (furnaceTicks <= 0) return 0

        // 应用铁炉燃料倍数（1.25倍），使煤炭可烧制 10 个物品
        return (furnaceTicks * FUEL_MULTIPLIER).toInt()
    }

    /**
     * 检查物品是否可以放入指定槽位
     */
    fun canPlaceInSlot(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return when (slot) {
            SLOT_INPUT -> {
                // 输入槽：检查是否有烧制配方
                val world = this.world
                if (world != null && !world.isClient) {
                    val inputInv = SimpleInventory(1).apply { setStack(0, stack) }
                    val match = world.recipeManager.getFirstMatch(RecipeType.SMELTING, inputInv, world)
                    match.isPresent
                } else {
                    true  // 客户端或无世界时允许，由服务器端验证
                }
            }
            SLOT_FUEL -> {
                // 燃料槽：检查是否是有效燃料
                getFuelTime(stack) > 0
            }
            SLOT_OUTPUT -> false  // 输出槽只能从机器内部放入
            else -> false
        }
    }

    private fun isSmeltingInput(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val w = world ?: return true
        val inv = SimpleInventory(stack.copyWithCount(1))
        return w.recipeManager.getFirstMatch(RecipeType.SMELTING, inv, w).isPresent
    }

    fun dropStoredExperience() {
        val world = this.world as? ServerWorld ?: return
        FurnaceExperienceHelper.dropExperience(world, pos, storedExperience)
        storedExperience = 0f
        markDirty()
    }

    private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        val wasActive = state.get(activeProperty)
        if (wasActive != active) {
            world.setBlockState(pos, state.with(activeProperty, active))
        }
        if (world.isClient) return
        if (soundConfig.soundType == ic2_120.content.sound.SoundType.NONE) return

        when (soundConfig.soundType) {
            ic2_120.content.sound.SoundType.START_STOP -> {
                when {
                    !wasActive && active -> {
                        soundConfig.startSound?.let {
                            world.playSound(null, pos, it, SoundCategory.BLOCKS, soundConfig.startVolume, soundConfig.startPitch)
                        }
                    }
                    wasActive && !active -> {
                        soundConfig.stopSound?.let {
                            world.playSound(null, pos, it, SoundCategory.BLOCKS, soundConfig.stopVolume, soundConfig.stopPitch)
                        }
                    }
                }
            }
            ic2_120.content.sound.SoundType.LOOP,
            ic2_120.content.sound.SoundType.OPERATE,
            ic2_120.content.sound.SoundType.NONE -> Unit
        }
    }
}
