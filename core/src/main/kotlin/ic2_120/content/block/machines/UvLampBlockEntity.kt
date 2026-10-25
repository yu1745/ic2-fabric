package ic2_120.content.block.machines

import ic2_120.compat.ContainerHelper26

import ic2_120.compat.LegacyBlockEntity26

import net.minecraft.network.FriendlyByteBuf

import io.netty.buffer.Unpooled

import ic2_120.compat.time
import ic2_120.compat.isClient
import ic2_120.compat.pos
import ic2_120.compat.world
import net.minecraft.core.HolderLookup
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
import ic2_120.content.storage.IRoutedSidedInventory
import ic2_120.content.sync.UvLampSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.Component
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level

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
) : LegacyBlockEntity26(type, pos, state), Container, IRoutedSidedInventory, ExtendedMenuProvider<FriendlyByteBuf> {

    private val inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    @RegisterItemStorage
    val itemStorage = RoutedItemStorage(
        inventory = inventory,
        maxCountPerStackProvider = { net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE },
        slotValidator = { slot, stack -> canPlaceItem(slot, stack) },
        insertRoutes = listOf(
            ItemInsertRoute(intArrayOf(SLOT_UPGRADE), matcher = { it.item is IUpgradeItem }, maxPerSlot = MAX_OVERCLOCKERS)
        ),
        extractSlots = intArrayOf(SLOT_UPGRADE),
        setChanged = { setChanged() }
    )

    override val routedItemStorage get() = itemStorage
    val syncedData = SyncedData()

    @RegisterEnergy
    val sync = UvLampSync(syncedData, { world?.time })

    private val adjacentEnergyTransfer = AdjacentEnergyTransferComponent(this, sync)
    private var particleOffset: Int = random.nextInt(0, (PARTICLE_INTERVAL_TICKS - 1) + 1)

    constructor(pos: BlockPos, state: BlockState) : this(UvLampBlockEntity::class.type(), pos, state)

    // ========== Container ==========

    override fun getContainerSize(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(inventory, slot, amount)
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(inventory, slot)
    override fun clearContent() = inventory.clear()
    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

    override fun setItem(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > MAX_OVERCLOCKERS) stack.count = MAX_OVERCLOCKERS
        setChanged()
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return stack.item is IUpgradeItem
    }

    // ========== AbstractContainerMenu ==========

    override fun getScreenOpeningData(player: ServerPlayer): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
            buf.writeBlockPos(pos)
            buf.writeVarInt(syncedData.count)
        return buf
    }

    override fun getDisplayName(): Component = Component.translatable("block.ic2_120.uv_lamp")

    override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        UvLampScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.world.inventory.ContainerLevelAccess.create(world!!, pos),
            syncedData,
            itemStorage
        )

    // ========== NBT ==========

    override fun saveAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.saveAdditional(nbt, lookup)
        ContainerHelper26.saveAllItems(nbt, inventory, lookup)
        syncedData.writeNbt(nbt)
        nbt.putLong(UvLampSync.NBT_ENERGY_STORED, sync.amount)
    }

    override fun loadAdditional(nbt: CompoundTag, lookup: HolderLookup.Provider) {
        super.loadAdditional(nbt, lookup)
        ContainerHelper26.loadAllItems(nbt, inventory, lookup)
        syncedData.readNbt(nbt)
        sync.restoreEnergy(nbt.getLongOr(UvLampSync.NBT_ENERGY_STORED, 0L))
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    // ========== Tick ==========

    fun tick(world: Level, pos: BlockPos, state: BlockState) {
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
        if (ocCount > 0 && active && world is ServerLevel) {
            val particleTick = (world.time + particleOffset) % PARTICLE_INTERVAL_TICKS
            if (particleTick == 0L) {
                spawnParticles(world, pos)
            }
        }

        sync.syncCurrentTickFlow()
        setChanged()
    }

    /** 统计升级槽中超频升级的数量（最大 MAX_OVERCLOCKERS） */
    fun getOverclockerCount(): Int {
        val stack = getItem(SLOT_UPGRADE)
        return if (!stack.isEmpty && stack.item is OverclockerUpgrade) {
            stack.count.coerceAtMost(MAX_OVERCLOCKERS)
        } else {
            0
        }
    }

    /** 扫描半径 3 内的 IC2 作物，注入额外生长点数 */
    private fun performCropScan(world: Level, pos: BlockPos, ocCount: Int) {
        val radius = SCAN_RADIUS
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx == 0 && dy == 0 && dz == 0) continue
                    val cropPos = pos.offset(dx, dy, dz)
                    val be = world.getBlockEntity(cropPos)
                    if (be is CropBlockEntity) {
                        val cropState = world.getBlockState(cropPos)
                        if (!cropState.hasProperty(CropBlock.CROP_TYPE) || !cropState.hasProperty(CropBlock.AGE)) continue
                        val age = cropState.getValue(CropBlock.AGE)
                        val cropType = cropState.getValue(CropBlock.CROP_TYPE)
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
    private fun spawnParticles(world: ServerLevel, pos: BlockPos) {
        val radius = SCAN_RADIUS
        val random = world.random
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    if (dx == 0 && dy == 0 && dz == 0) continue
                    val count = random.nextInt(1, (3) + 1)
                    val px = pos.x + dx + 0.5
                    val py = pos.y + dy + 0.5
                    val pz = pos.z + dz + 0.5
                    world.sendParticles(
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

    private fun setActiveState(world: Level, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.getValue(UvLampBlock.ACTIVE) != active) {
            world.setBlock(pos, state.setValue(UvLampBlock.ACTIVE, active), net.minecraft.world.level.block.Block.UPDATE_ALL)
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

        private val random: RandomSource = RandomSource.create()
    }
}
