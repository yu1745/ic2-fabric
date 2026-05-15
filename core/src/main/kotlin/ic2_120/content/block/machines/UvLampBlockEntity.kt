package ic2_120.content.block.machines

import ic2_120.content.AdjacentEnergyTransferComponent
import ic2_120.content.block.CropBlock
import ic2_120.content.block.CropBlockEntity
import ic2_120.content.block.UvLampBlock
import ic2_120.content.energy.EnergyTier
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.OverclockerUpgrade
import ic2_120.content.screen.UvLampScreenHandler
import ic2_120.content.storage.ItemInsertRoute
import ic2_120.content.storage.RoutedItemStorage
import ic2_120.content.sync.UvLampSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import io.netty.buffer.Unpooled
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper
import net.minecraft.network.PacketByteBuf
import net.minecraft.particle.ParticleTypes
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.World

/**
 * 紫外线灯方块实体。
 * 1 个升级槽（超频升级，最大堆叠 4），从电缆获取能量。
 * 超频升级数量决定能量等级和生长加速倍率。
 */
@ModBlockEntity(block = UvLampBlock::class)
class UvLampBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, ExtendedScreenHandlerFactory<PacketByteBuf> {

    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { maxCountPerStack },
        slotValidator = { slot, stack -> isValid(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_UPGRADE), matcher = { it.item is IUpgradeItem }, maxPerSlot = MAX_OVERCLOCKERS)
        ),
        extractSlots = intArrayOf(SLOT_UPGRADE),
        markDirty = { markDirty() }
    )
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = UvLampSync(syncedData, { world?.time })

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private var particleOffset: Int = random.nextBetween(0, PARTICLE_INTERVAL_TICKS - 1)

    constructor(pos: BlockPos, state: BlockState) : this(UvLampBlockEntity::class.type(), pos, state)

    // ========== Inventory ==========

    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > MAX_OVERCLOCKERS) stack.count = MAX_OVERCLOCKERS
        markDirty()
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return stack.item is IUpgradeItem
    }

    // ========== ScreenHandler ==========

    override fun getScreenOpeningData(player: ServerPlayerEntity): PacketByteBuf {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        return buf
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.uv_lamp")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        UvLampScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData,
            itemStorage
        )

    // ========== NBT ==========

    override fun writeNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup) {
        super.writeNbt(nbt, registryLookup)
        Inventories.writeNbt(nbt, inventory, registryLookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(UvLampSync.NBT_ENERGY_STORED, sync.amount)
    }

    override fun readNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup) {
        super.readNbt(nbt, registryLookup)
        Inventories.readNbt(nbt, inventory, registryLookup)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLong(UvLampSync.NBT_ENERGY_STORED))
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    // ========== Tick ==========

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        sync.energy = sync.amount.toInt().coerceAtLeast(0)
        adjacentEnergyTransfer.tick()

        val ocCount = getOverclockerCount()
        val effectiveTier = ocCount + 1
        val energyPerTick = EnergyTier.euPerTickFromTier(effectiveTier)

        // 消耗能量
        val active = sync.consumeEnergy(energyPerTick) > 0L
        setActiveState(world, pos, state, active)

        val growthMultiplier = if (ocCount > 0) ocCount + 1 else 0
        sync.growthMultiplier = growthMultiplier
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        // 作物生长加速
        if (ocCount > 0 && active && world.time % CropBlockEntity.TICK_RATE.toLong() == 0L) {
            performCropScan(world, pos, ocCount)
        }

        // 紫色粒子效果
        if (ocCount > 0 && active && world is ServerWorld) {
            val particleTick = (world.time + particleOffset) % PARTICLE_INTERVAL_TICKS
            if (particleTick == 0L) {
                spawnParticles(world as ServerWorld, pos)
            }
        }

        sync.syncCurrentTickFlow()
        markDirty()
    }

    /** 统计升级槽中超频升级的数量（最大 MAX_OVERCLOCKERS） */
    fun getOverclockerCount(): Int {
        val stack = getStack(SLOT_UPGRADE)
        return if (!stack.isEmpty && stack.item is OverclockerUpgrade) {
            stack.count.coerceAtMost(MAX_OVERCLOCKERS)
        } else {
            0
        }
    }

    /** 扫描半径 3 内的 IC2 作物，注入额外生长点数 */
    private fun performCropScan(world: World, pos: BlockPos, ocCount: Int) {
        val radius = SCAN_RADIUS
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx == 0 && dy == 0 && dz == 0) continue
                    val cropPos = pos.add(dx, dy, dz)
                    val be = world.getBlockEntity(cropPos)
                    if (be is CropBlockEntity) {
                        val cropState = world.getBlockState(cropPos)
                        if (!cropState.contains(CropBlock.CROP_TYPE) || !cropState.contains(CropBlock.AGE)) continue
                        val age = cropState.get(CropBlock.AGE)
                        val cropType = cropState.get(CropBlock.CROP_TYPE)
                        val maxAge = ic2_120.content.crop.CropSystem.maxAge(cropType)
                        if (age >= maxAge) continue
                        // 注入额外生长点数：ocCount * ESTIMATED_BASE_GROWTH
                        val extraPoints = ocCount * ESTIMATED_BASE_GROWTH
                        be.addGrowthPoints(extraPoints)
                    }
                }
            }
        }
    }

    /** 在加速区域每个格子渲染紫色传送门粒子 */
    private fun spawnParticles(world: ServerWorld, pos: BlockPos) {
        val radius = SCAN_RADIUS
        val random = world.random
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx == 0 && dy == 0 && dz == 0) continue
                    val count = random.nextBetween(1, 3)
                    val px = pos.x + dx + 0.5
                    val py = pos.y + dy + 0.5
                    val pz = pos.z + dz + 0.5
                    world.spawnParticles(
                        ParticleTypes.PORTAL,
                        px, py, pz,
                        count,
                        0.35, 0.35, 0.35,
                        0.5
                    )
                }
            }
        }
    }

    private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.get(UvLampBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(UvLampBlock.ACTIVE, active))
        }
    }

    companion object {
        const val SLOT_UPGRADE = 0
        const val INVENTORY_SIZE = 1
        const val MAX_OVERCLOCKERS = 4

        private const val SCAN_RADIUS = 3
        /** 估计的基础生长点数：3 + avg(0..6) + 0 stats = ~6.5，取整为 9 以确保加速效果 */
        const val ESTIMATED_BASE_GROWTH = 9
        private const val PARTICLE_INTERVAL_TICKS = 40

        private val random: Random = Random.create()
    }
}
