package ic2_120.content.block.machines

<<<<<<< HEAD
import ic2_120.content.RecipeCacheEpoch
=======
>>>>>>> backup/1.21.1
import ic2_120.content.recipes.compressor.CompressorRecipe
import ic2_120.content.sync.CompressorSync
import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.CompressorBlock
import ic2_120.content.sound.MachineSoundConfig
import ic2_120.content.block.ITieredMachine
import ic2_120.content.screen.CompressorScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.content.upgrade.EjectorUpgradeComponent
<<<<<<< HEAD
import ic2_120.content.upgrade.EnergyDebtAccounting
=======
>>>>>>> backup/1.21.1
import ic2_120.content.upgrade.PullingUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.IEjectorUpgradeSupport
import ic2_120.content.upgrade.IEnergyStorageUpgradeSupport
import ic2_120.content.upgrade.IOverclockerUpgradeSupport
import ic2_120.content.upgrade.ITransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
<<<<<<< HEAD
import ic2_120.content.storage.IRoutedSidedInventory
=======
>>>>>>> backup/1.21.1
import ic2_120.content.recipes.compressor.CompressorRecipeSerializer
import ic2_120.content.recipes.getRecipeType
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.ModMachineRecipeBinding
<<<<<<< HEAD
import ic2_120.registry.type
=======
>>>>>>> backup/1.21.1
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluids
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
<<<<<<< HEAD
import net.minecraft.inventory.SimpleInventory
=======
>>>>>>> backup/1.21.1
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.Ingredient
<<<<<<< HEAD
import net.minecraft.recipe.RecipeManager
=======
import net.minecraft.recipe.input.SingleStackRecipeInput
>>>>>>> backup/1.21.1
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
<<<<<<< HEAD
import ic2_120.content.upgrade.IValidInputItem
=======
import net.minecraft.registry.RegistryWrapper
import io.netty.buffer.Unpooled
>>>>>>> backup/1.21.1

@ModBlockEntity(block = CompressorBlock::class)
@ModMachineRecipeBinding(CompressorRecipeSerializer::class)
class CompressorBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
<<<<<<< HEAD
) : MachineBlockEntity(type, pos, state), Inventory, IRoutedSidedInventory, ITieredMachine, IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory, IValidInputItem {
=======
) : MachineBlockEntity(type, pos, state), Inventory, ITieredMachine, IOverclockerUpgradeSupport, IEnergyStorageUpgradeSupport, ITransformerUpgradeSupport, IEjectorUpgradeSupport, ExtendedScreenHandlerFactory<PacketByteBuf> {
>>>>>>> backup/1.21.1

    override val activeProperty: net.minecraft.state.property.BooleanProperty = CompressorBlock.ACTIVE

    override val soundConfig: MachineSoundConfig = MachineSoundConfig.operate(
        soundId = "machine.compressor.operate",
        volume = 0.5f,
        pitch = 1.0f,
        intervalTicks = 20
    )

    override fun getInventory(): net.minecraft.inventory.Inventory = this

    override val tier: Int = COMPRESSOR_TIER

    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

<<<<<<< HEAD
    /** 浮点进度（内部记账，不参与同步/落盘；同步与存档仍用整型 [sync.progress]）。 */
    private var progressF = 0f

    /** 浮点耗能债务：1.6^n 的小数余数跨 tick 结转，停顿期间清零。 */
    private var energyDebtF = 0f

=======
>>>>>>> backup/1.21.1
    companion object {
        const val COMPRESSOR_TIER = 1
        const val SLOT_INPUT = 0
        const val SLOT_OUTPUT = 1
        const val SLOT_DISCHARGING = 2
        const val SLOT_UPGRADE_0 = 3
        const val SLOT_UPGRADE_1 = 4
        const val SLOT_UPGRADE_2 = 5
        const val SLOT_UPGRADE_3 = 6
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        val SLOT_OUTPUT_INDICES = intArrayOf(SLOT_OUTPUT)
        val SLOT_INPUT_INDICES = intArrayOf(SLOT_INPUT)
        const val INVENTORY_SIZE = 7
    }

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    /** 待输出的容器返还（输出槽满时缓存，等有空间再放入） */
    private var pendingContainerReturn: ItemStack = ItemStack.EMPTY
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(SLOT_UPGRADE_INDICES, matcher = { it.item is IUpgradeItem }),
<<<<<<< HEAD
            // 注意：配方输入路由必须排在放电路由之前——能量水晶粉/红石粉同时是配方输入和放电燃料，
            // 若放电路由在前，物流插入会把它们截进放电槽当燃料烧掉（每批截 1 个），自动化加工直接断流。
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isRecipeInput(it) }),
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) || it.item === Items.REDSTONE || it.item is ic2_120.content.item.EnergiumDust }, maxPerSlot = 1)
=======
            ItemInsertRoute(intArrayOf(SLOT_DISCHARGING), matcher = { isBatteryItem(it) }, maxPerSlot = 1),
            ItemInsertRoute(intArrayOf(SLOT_INPUT), matcher = { isRecipeInput(it) })
>>>>>>> backup/1.21.1
        ),
        extractSlots = intArrayOf(SLOT_OUTPUT),
        markDirty = { markDirty() }
    )

<<<<<<< HEAD
    override val routedItemStorage get() = itemStorage

    val syncedData = SyncedData()
=======
    val syncedData = SyncedData(this)
>>>>>>> backup/1.21.1
    @RegisterEnergy
    val sync = CompressorSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(COMPRESSOR_TIER + voltageTierBonus) }
    )

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { COMPRESSOR_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        CompressorBlockEntity::class.type(),
        pos,
        state
    )

    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) {
            stack.count = 1
        }
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
        SLOT_INPUT -> isRecipeInput(stack)
        SLOT_OUTPUT -> false
<<<<<<< HEAD
        SLOT_DISCHARGING -> isBatteryItem(stack) || stack.item === Items.REDSTONE || stack.item is ic2_120.content.item.EnergiumDust
=======
        SLOT_DISCHARGING -> isBatteryItem(stack)
>>>>>>> backup/1.21.1
        in SLOT_UPGRADE_0..SLOT_UPGRADE_3 -> stack.item is IUpgradeItem
        else -> false
    }

<<<<<<< HEAD
    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
=======
    override fun getScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
>>>>>>> backup/1.21.1
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.compressor")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CompressorScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, itemStorage)

<<<<<<< HEAD
    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
=======
    override fun readNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, lookup)
        Inventories.readNbt(nbt, inventory, lookup)
>>>>>>> backup/1.21.1
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CompressorSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
<<<<<<< HEAD
        progressF = if (nbt.contains("ProgressF")) {
            nbt.getFloat("ProgressF").coerceIn(0f, CompressorSync.PROGRESS_MAX.toFloat())
        } else {
            sync.progress.toFloat().coerceIn(0f, CompressorSync.PROGRESS_MAX.toFloat())
        }
        energyDebtF = if (nbt.contains("EnergyDebt")) nbt.getFloat("EnergyDebt").coerceAtLeast(0f) else 0f
        if (nbt.contains("pending_container_return")) {
            pendingContainerReturn = ItemStack.fromNbt(nbt.getCompound("pending_container_return"))
        }
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CompressorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putFloat("ProgressF", progressF)
        nbt.putFloat("EnergyDebt", energyDebtF)
        if (!pendingContainerReturn.isEmpty) {
            nbt.put("pending_container_return", pendingContainerReturn.writeNbt(NbtCompound()))
        }
    }

    /** 重置全部进度状态（输入空/无配方/输出堵时调用）。 */
    private fun resetProgress() {
        if (progressF != 0f || energyDebtF != 0f || sync.progress != 0) {
            progressF = 0f
            energyDebtF = 0f
            sync.progress = 0
=======
        if (nbt.contains("pending_container_return")) {
            pendingContainerReturn = ItemStack.fromNbt(lookup, nbt.getCompound("pending_container_return")).orElse(ItemStack.EMPTY)
        }
    }

    override fun writeNbt(nbt: NbtCompound, lookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, lookup)
        Inventories.writeNbt(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(CompressorSync.NBT_ENERGY_STORED, sync.amount)
        if (!pendingContainerReturn.isEmpty) {
            nbt.put("pending_container_return", pendingContainerReturn.encode(lookup))
>>>>>>> backup/1.21.1
        }
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 应用升级效果（加速、储能、高压）
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EjectorUpgradeComponent.ejectIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_OUTPUT_INDICES)
        PullingUpgradeComponent.pullIfUpgraded(world, pos, this, SLOT_UPGRADE_INDICES, SLOT_INPUT_INDICES)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        adjacentEnergyTransfer.tick()

        // 从放电槽提取能量
        extractFromDischargingSlot()

        // 先尝试输出缓存的容器返还
        if (!pendingContainerReturn.isEmpty) {
            val outputSlot = getStack(SLOT_OUTPUT)
            if (outputSlot.isEmpty()) {
                setStack(SLOT_OUTPUT, pendingContainerReturn.copy())
                pendingContainerReturn = ItemStack.EMPTY
                markDirty()
            } else if (ItemStack.areItemsEqual(outputSlot, pendingContainerReturn) && outputSlot.count + pendingContainerReturn.count <= outputSlot.maxCount) {
                outputSlot.increment(pendingContainerReturn.count)
                pendingContainerReturn = ItemStack.EMPTY
                markDirty()
            } else {
                // 缓存还没法输出，跳过配方处理
<<<<<<< HEAD
                resetProgress()
=======
                if (sync.progress != 0) sync.progress = 0
>>>>>>> backup/1.21.1
                setActiveState(world, pos, state, false)
                sync.syncCurrentTickFlow()
                return
            }
        }

        val input = getStack(SLOT_INPUT)
        val recipe = getRecipe(world, input) ?: run {
<<<<<<< HEAD
            resetProgress()
=======
            if (sync.progress != 0) sync.progress = 0
>>>>>>> backup/1.21.1
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }
        val inputCount = recipe.inputCount
        val result = recipe.output
        val outputSlot = getStack(SLOT_OUTPUT)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
<<<<<<< HEAD
            resetProgress()
=======
            if (sync.progress != 0) sync.progress = 0
>>>>>>> backup/1.21.1
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

<<<<<<< HEAD
        if (progressF >= CompressorSync.PROGRESS_MAX) {
=======
        val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
        if (sync.progress >= CompressorSync.PROGRESS_MAX) {
>>>>>>> backup/1.21.1
            input.decrement(inputCount)
            if (outputSlot.isEmpty()) setStack(SLOT_OUTPUT, result.copy())
            else outputSlot.increment(result.count)
            // 容器返还：缓存到 pending，等输出槽空了再放入
            val container = recipe.containerReturn
            if (!container.isEmpty) {
                pendingContainerReturn = container.copy()
            }
<<<<<<< HEAD
            progressF = 0f
            energyDebtF = 0f
            sync.progress = 0
            markDirty()
            // 完工不振荡（2026-08-26 perf）：下 tick 早退分支自然关灯
=======
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
>>>>>>> backup/1.21.1
            sync.syncCurrentTickFlow()
            return
        }

<<<<<<< HEAD
        // 耗能记账：1.6^n 按浮点累计，取整数部分消费、余数结转（EU 本身是整数）
        energyDebtF = EnergyDebtAccounting.accrue(energyDebtF, CompressorSync.ENERGY_PER_TICK * energyMultiplier, sync.getEffectiveCapacity())
        val need = energyDebtF.toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            energyDebtF -= need
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            // 浮点进度：1.4286^n 直接累加；封顶 PROGRESS_MAX 防极端超频（+Inf）免费完工
            progressF = (progressF + speedMultiplier).coerceAtMost(CompressorSync.PROGRESS_MAX.toFloat())
            sync.progress = progressF.toInt()
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            // 停顿期间不累计债务：防恢复供电后 need 超容量 all-or-nothing 卡死
            energyDebtF = 0f
=======
        val need = (CompressorSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress += progressIncrement
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
>>>>>>> backup/1.21.1
            setActiveState(world, pos, state, false)
        }
        sync.syncCurrentTickFlow()
    }

<<<<<<< HEAD
    // 配方缓存——安全处理多对一配方（inputCount > 1）：
    // CompressorRecipe.matches 检查 stack.count >= inputCount，匹配结果依赖输入数量。
    // 规则：非 null 缓存仅在 input.count >= recipe.inputCount 时有效；
    // null 缓存仅在输入数量未增加时有效（数量增加可能让此前不足量的配方匹配）。
    private var cachedRecipeItem: net.minecraft.item.Item? = null
    private var cachedRecipe: CompressorRecipe? = null
    private var cachedRecipeQueryCount = 0
    private var cachedRecipeEpoch = -1

    private fun getRecipe(world: World, input: ItemStack): CompressorRecipe? {
        if (input.isEmpty) return null
        // recipeManager 缓存与水容器判定分离：
        // 水容器配方依赖流体内容（NBT），不能并入按 item 的缓存——否则空罐缓存 null 后
        // 原位替换成满水罐（item/count 均不变）会永久命中 null 缓存导致不再加工。
        val recipe = recipeFromManagerCached(world, input)
        if (recipe != null) return recipe
        return waterContainerRecipeCached(input)
    }

    private fun recipeFromManagerCached(world: World, input: ItemStack): CompressorRecipe? {
        if (RecipeCacheEpoch.current() == cachedRecipeEpoch && cachedRecipeItem === input.item) {
            val cached = cachedRecipe
            if (cached != null && input.count >= cached.inputCount) return cached
            if (cached == null && input.count <= cachedRecipeQueryCount) return null
        }
        val inventory = SimpleInventory(input)
        val recipe = world.recipeManager
            .getFirstMatch(getRecipeType<CompressorRecipe>(), inventory, world)
            .orElse(null)
        cachedRecipeEpoch = RecipeCacheEpoch.current()
        cachedRecipeItem = input.item
        cachedRecipe = recipe
        cachedRecipeQueryCount = input.count
        return recipe
    }

    // 水容器判定缓存：按完整 ItemStack 相等（item/count/NBT）缓存——流体内容变化
    // （NBT 不同）必然失效，因此空罐→满水罐原位替换也能正确重判。
    private var cachedWaterStack: ItemStack = ItemStack.EMPTY
    private var cachedWaterRecipe: CompressorRecipe? = null

    private fun waterContainerRecipeCached(input: ItemStack): CompressorRecipe? {
        if (input == cachedWaterStack) return cachedWaterRecipe
        val recipe = getWaterContainerRecipe(input)
        cachedWaterStack = input.copy()
        cachedWaterRecipe = recipe
        return recipe
=======
    private fun getRecipe(world: World, input: ItemStack): CompressorRecipe? {
        if (input.isEmpty) return null
        val recipeManager = world.recipeManager
        val optionalRecipe = recipeManager.getFirstMatch(getRecipeType<CompressorRecipe>(), SingleStackRecipeInput(input), world)
        return optionalRecipe.map { it.value }.orElse(null) ?: getWaterContainerRecipe(input)
>>>>>>> backup/1.21.1
    }

    private fun getWaterContainerRecipe(input: ItemStack): CompressorRecipe? {
        val emptyContainer = extractWaterContainerRemainder(input) ?: return null
        return CompressorRecipe(
<<<<<<< HEAD
            Identifier("ic2_120", "compressing/water_container_to_snow_block"),
=======
            Identifier.of("ic2_120", "compressing/water_container_to_snow_block"),
>>>>>>> backup/1.21.1
            Ingredient.EMPTY,
            1,
            ItemStack(Items.SNOW_BLOCK),
            emptyContainer
        )
    }

    private fun extractWaterContainerRemainder(input: ItemStack): ItemStack? {
        if (input.isEmpty) return null
<<<<<<< HEAD
        @Suppress("DEPRECATION")
        val ctx = ContainerItemContext.withInitial(input.copyWithCount(1))
=======
        val ctx = ContainerItemContext.withConstant(input.copyWithCount(1))
>>>>>>> backup/1.21.1
        val itemStorage = ctx.find(FluidStorage.ITEM) ?: return null
        for (view in itemStorage) {
            val resource = view.resource
            if (resource.isBlank || view.amount < FluidConstants.BUCKET || !isWater(resource)) continue
            Transaction.openOuter().use { tx ->
                val extracted = view.extract(resource, FluidConstants.BUCKET, tx)
                if (extracted != FluidConstants.BUCKET) return@use
                tx.commit()
                return ctx.itemVariant.toStack(ctx.amount.toInt().coerceAtLeast(0))
            }
        }
        return null
    }

    private fun isWater(variant: FluidVariant): Boolean {
        return variant.fluid == Fluids.WATER || variant.fluid == Fluids.FLOWING_WATER
    }

    private fun isBatteryItem(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is IBatteryItem

<<<<<<< HEAD
    // 类型判定缓存：isRecipeInput 始终用 maxCount 查询（仅依赖物品类型），按 item 缓存安全。
    private var cachedInputItem: net.minecraft.item.Item? = null
    private var cachedIsInput = false
    private var cachedInputEpoch = -1

    private fun isRecipeInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack)) return false
        if (RecipeCacheEpoch.current() == cachedInputEpoch && cachedInputItem === stack.item) return cachedIsInput
        val w = world ?: return true
        val inv = SimpleInventory(stack.copyWithCount(stack.maxCount))
        val found = w.recipeManager.getFirstMatch(getRecipeType<CompressorRecipe>(), inv, w).isPresent
        cachedInputEpoch = RecipeCacheEpoch.current()
        cachedInputItem = stack.item
        cachedIsInput = found
        return found
=======
    private fun isRecipeInput(stack: ItemStack): Boolean {
        if (stack.isEmpty || isBatteryItem(stack)) return false
        val w = world ?: return true
        return w.recipeManager.getFirstMatch(getRecipeType<CompressorRecipe>(), SingleStackRecipeInput(stack.copyWithCount(stack.maxCount)), w).isPresent ||
            extractWaterContainerRemainder(stack) != null
>>>>>>> backup/1.21.1
    }

    /**
     * 从放电槽提取能量（如果需要）
     */
    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
<<<<<<< HEAD
=======
        if (space <= 0L) return
>>>>>>> backup/1.21.1

        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return

<<<<<<< HEAD
        val inserted = sync.insertEnergy(extracted)
        if (extracted > inserted) sync.forceInsertEnergy(extracted - inserted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }
    /** P1（2026-08-26）：pulling 候选快速否定——(配方类型, Item) 布尔缓存零分配作答。
     *  返回 false = 该 item 必然不被输入槽接受；true = 值得走完整 isValid 裁决。 */
    override fun isValidInputItem(item: net.minecraft.item.Item): Boolean {
        if (isBatteryItem(net.minecraft.item.ItemStack(item, 1))) return false
        val w = world as? net.minecraft.server.world.ServerWorld ?: return true
        return ic2_120.content.RecipeInputLookupCache.hasAnyRecipe(getRecipeType<CompressorRecipe>(), item, w)
    }

=======
        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }
>>>>>>> backup/1.21.1
}
