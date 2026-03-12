package ic2_120.content.block.machines

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.MachineBlock
import ic2_120.content.block.NuclearReactorBlock
import ic2_120.Ic2_120
import ic2_120.content.block.ReactorChamberBlock
import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.reactor.IReactor
import ic2_120.content.reactor.IBaseReactorComponent
import ic2_120.content.reactor.IReactorComponent
import ic2_120.content.network.NetworkManager
import ic2_120.content.network.ReactorHeatInfoPacket
import ic2_120.content.network.SlotHeatEnergyInfo
import ic2_120.content.screen.NuclearReactorScreenHandler
import ic2_120.content.sync.NuclearReactorSync
import net.minecraft.registry.Registries
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.AbstractFireBlock
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 核反应堆方块实体。
 *
 * 多方块结构：中心为核反应堆，六面可各接触 0 或 1 个核反应仓。
 * 容量 = 27 + 相邻反应仓数 * 9，最大 81 格。
 * 所有 NBT 存在反应堆 BE 上。
 * 电力等级 5，视为发电机（IGenerator），不实现电池充电。
 */
@ModBlockEntity(block = NuclearReactorBlock::class)
class NuclearReactorBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, IGenerator, ITieredMachine, IReactor,
    ExtendedScreenHandlerFactory {

    override val tier: Int = NuclearReactorSync.REACTOR_TIER

    private val inventory = DefaultedList.ofSize(MAX_SLOTS, ItemStack.EMPTY)

    /** 计算周期偏移（0..19），使 (world.time + tickOffset) % 20 == 0 时执行 */
    private var tickOffset: Int = 0

    /** 本周期组件散热蒸发累加（负值表示降温），Pass 1 结束后加回堆温 */
    private var emitHeatBuffer: Int = 0

    /** 本周期发电累加（每脉冲 1.0），周期结束后转为 EU */
    private var outputAccumulator: Float = 0f

    /** 本周期总产热 */
    private var totalHeatProduced: Int = 0

    /** 本周期总散热 */
    private var totalHeatDissipated: Int = 0

    /** 每个槽位的产热、散热和发电 */
    val slotHeatInfo = mutableMapOf<Int, SlotHeatEnergyInfo>()

    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = NuclearReactorSync(
        syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(NuclearReactorBlockEntity::class),
        pos,
        state
    )

    override fun size(): Int = MAX_SLOTS
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (!stack.isEmpty) {
            if (stack.item !is IBaseReactorComponent) return
            if (stack.item is IBaseReactorComponent && !(stack.item as IBaseReactorComponent).canBePlacedIn(
                    stack,
                    this
                )
            ) return
            if (slot >= currentCapacity()) return  // 防止物流 mod 向超出容量的槽位强制插入
        }
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() {
        super.markDirty()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        buf.writeVarInt(currentCapacity())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.nuclear_reactor")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        NuclearReactorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData,
            currentCapacity(),
            this
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(NuclearReactorSync.NBT_ENERGY_STORED).coerceIn(0L, NuclearReactorSync.ENERGY_CAPACITY)
        // sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        sync.temperature = nbt.getInt(NuclearReactorSync.NBT_HEAT_STORED).coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        tickOffset = if (nbt.contains("TickOffset")) nbt.getInt("TickOffset").coerceIn(0, 19) else -1
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(NuclearReactorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putInt(NuclearReactorSync.NBT_HEAT_STORED, sync.temperature)
        if (tickOffset >= 0) nbt.putInt("TickOffset", tickOffset)
    }

    /** 当前有效容量（27 + 相邻反应仓数 * 9），供 setStack 等服务端逻辑校验 */
    private fun currentCapacity(): Int {
        val w = world ?: return NuclearReactorSync.BASE_SLOTS
        var chamberCount = 0
        for (dir in Direction.values()) {
            if (w.getBlockState(pos.offset(dir)).block is ReactorChamberBlock) chamberCount++
        }
        return NuclearReactorSync.BASE_SLOTS + chamberCount * NuclearReactorSync.SLOTS_PER_CHAMBER
    }

    // ---------- IReactor ----------
    override fun getWorld(): World? = world
    override fun getPos(): BlockPos = pos
    override fun getHeat(): Int = sync.temperature
    override fun setHeat(heat: Int) {
        sync.temperature = heat.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
    }

    override fun addHeat(amount: Int): Int {
        sync.temperature = (sync.temperature + amount).coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        return sync.temperature
    }

    override fun getMaxHeat(): Int = NuclearReactorSync.HEAT_CAPACITY
    override fun setMaxHeat(maxHeat: Int) {} // 暂不动态修改
    override fun addEmitHeat(heat: Int) {
        emitHeatBuffer += heat
    }

    override fun getHeatEffectModifier(): Float = 1f
    override fun setHeatEffectModifier(hem: Float) {}
    override fun getReactorEnergyOutput(): Float = outputAccumulator
    override fun addOutput(energy: Float): Float {
        // println("addOutput: $energy")  
        outputAccumulator += energy; return outputAccumulator
    }

    override fun getReactorCols(): Int = currentCapacity() / 9
    override fun getReactorRows(): Int = 9
    override fun getItemAt(x: Int, y: Int): ItemStack? {
        if (x < 0 || x >= getReactorCols() || y < 0 || y >= 9) return null
        val stack = getStack(x * 9 + y)
        return if (stack.isEmpty) null else stack
    }

    override fun setItemAt(x: Int, y: Int, stack: ItemStack?) {
        if (x in 0 until getReactorCols() && y in 0 until 9) {
            setStack(x * 9 + y, stack ?: ItemStack.EMPTY)
        }
    }

    override fun produceEnergy(): Boolean = true
    override fun getTickRate(): Int = 20
    override fun isFluidCooled(): Boolean = false

    override fun addHeatProduced(amount: Int) {
        totalHeatProduced += amount
    }

    override fun addHeatDissipated(amount: Int) {
        totalHeatDissipated += amount
    }

    override fun addSlotHeatInfo(slot: Int, heatProduced: Int, heatDissipated: Int, energyOutput: Float) {
        val current = slotHeatInfo.getOrDefault(slot, SlotHeatEnergyInfo(0, 0, 0f))
        slotHeatInfo[slot] = SlotHeatEnergyInfo(
            current.heatProduced + heatProduced,
            current.heatDissipated + heatDissipated,
            current.energyOutput + energyOutput
        )
    }

    /**
     * 容量减少时，将超出新容量的槽位物品散落掉落。
     * 在 tick 与 neighborUpdate（反应仓被拆）时调用。
     */
    fun dropOverflowItems(world: World, pos: BlockPos) {
        val cap = currentCapacity()
        if (cap >= MAX_SLOTS) return
        for (i in cap until MAX_SLOTS) {
            val stack = getStack(i)
            if (!stack.isEmpty) {
                net.minecraft.util.ItemScatterer.spawn(
                    world,
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                    stack
                )
                setStack(i, ItemStack.EMPTY)
            }
        }
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        val newCapacity = currentCapacity()
        sync.capacity1 = newCapacity

        // 容量减少时，溢出槽位的物品掉落
        dropOverflowItems(world, pos)

        // 初始化 tickOffset（首次加载时）
        if (tickOffset < 0) {
            tickOffset = world.random.nextBetween(0, 19)
            markDirty()
        }

        // 仅当 (world.time + tickOffset) % 20 == 0 时执行核电计算（每秒一次）
        val shouldTick = (world.time + tickOffset) % 20L == 0L
        if (shouldTick) {
            dropAllUnfittingStuff(world, pos)
            outputAccumulator = 0f
            emitHeatBuffer = 0
            totalHeatProduced = 0
            totalHeatDissipated = 0
            slotHeatInfo.clear()

            processChambers()

            // 将 emitHeatBuffer 加回堆温（组件散热蒸发为负值，即降温）
            sync.temperature = (sync.temperature + emitHeatBuffer).coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)

            // 将 output 转为 EU
            val euToAdd = (outputAccumulator * NuclearReactorSync.EU_PER_OUTPUT).toLong()
            if (euToAdd > 0) {
                sync.generateEnergy(euToAdd)
                sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
                markDirty()
            }

            // 同步产热和散热数据
            sync.totalHeatProduced = totalHeatProduced
            sync.totalHeatDissipated = totalHeatDissipated

            // 发送槽位产热散热信息到客户端（发电数值乘以5用于显示）
            val displaySlotHeatInfo = slotHeatInfo.mapValues { (_, info) ->
                SlotHeatEnergyInfo(info.heatProduced, info.heatDissipated, info.energyOutput * 5)
            }
            val packet = ReactorHeatInfoPacket(pos, displaySlotHeatInfo)
            for (player in world.players) {
                NetworkManager.sendToClient(player as net.minecraft.server.network.ServerPlayerEntity, packet)
            }

            if (calculateHeatEffects(world, pos)) return
        }

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        if (sync.temperature >= NuclearReactorSync.HEAT_EXPLODE_THRESHOLD) {
            explode()
            return
        }

        if (shouldTick) {
            applyHeatEffects(world, pos)
        }

        val hasFuel = (0 until newCapacity).any { !getStack(it).isEmpty }
        val active = hasFuel
        if (state.get(NuclearReactorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(NuclearReactorBlock.ACTIVE, active))
        }

        sync.syncCurrentTickFlow()
    }

    /** 移除无效组件、超出容量的物品 */
    private fun dropAllUnfittingStuff(world: World, pos: BlockPos) {
        val cap = currentCapacity()
        for (i in 0 until cap) {
            val stack = getStack(i)
            if (!stack.isEmpty && (stack.item !is IBaseReactorComponent || (stack.item is IBaseReactorComponent && !(stack.item as IBaseReactorComponent).canBePlacedIn(
                    stack,
                    this
                )))
            ) {
                setStack(i, ItemStack.EMPTY)
                net.minecraft.util.ItemScatterer.spawn(
                    world,
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                    stack
                )
            }
        }
        for (i in cap until MAX_SLOTS) {
            val stack = getStack(i)
            if (!stack.isEmpty) {
                setStack(i, ItemStack.EMPTY)
                net.minecraft.util.ItemScatterer.spawn(
                    world,
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                    stack
                )
            }
        }
    }

    /** 双阶段 processChambers：Pass 0 发电，Pass 1 热量分配 */
    private fun processChambers() {
        val cols = getReactorCols()
        for (pass in 0..1) {
            val heatRun = pass == 1
            for (y in 0 until 9) {
                for (x in 0 until cols) {
                    val stack = getItemAt(x, y) ?: continue
                    if (stack.item is IReactorComponent) {
                        (stack.item as IReactorComponent).processChamber(stack, this, x, y, heatRun)
                    }
                }
            }
        }
    }

    /** 堆温对环境的影响，返回 true 表示已爆炸 */
    private fun calculateHeatEffects(world: World, pos: BlockPos): Boolean {
        if (sync.temperature < NuclearReactorSync.HEAT_FIRE_THRESHOLD) return false
        val power = sync.temperature.toFloat() / NuclearReactorSync.HEAT_CAPACITY
        if (power >= 1f) {
            explode()
            return true
        }
        return false
    }

    override fun explode() {
        var boomPower = 10f
        var boomMod = 1f
        val cols = getReactorCols()

        for (y in 0 until 9) {
            for (x in 0 until cols) {
                val stack = getItemAt(x, y) ?: continue
                if (stack.item is IReactorComponent) {
                    val inf = (stack.item as IReactorComponent).influenceExplosion(stack, this)
                    if (inf > 0f && inf < 1f) boomMod *= inf
                    else if (inf >= 1f) boomPower += inf
                }
                setItemAt(x, y, null)
            }
        }
        boomPower *= boomMod

        val w = world ?: return
        for (dir in Direction.values()) {
            val neighborPos = pos.offset(dir)
            if (w.getBlockState(neighborPos).block is ReactorChamberBlock) {
                w.breakBlock(neighborPos, false)
            }
        }
        w.breakBlock(pos, false)
        val cx = pos.x + 0.5
        val cy = pos.y + 0.5
        val cz = pos.z + 0.5
        w.createExplosion(null, cx, cy, cz, boomPower.coerceAtMost(20f), true, World.ExplosionSourceType.BLOCK)
    }

    /**
     * 根据堆温对周围环境施加影响：着火、水蒸发、生物受伤、方块变岩浆。
     */
    private fun applyHeatEffects(world: World, pos: BlockPos) {
        val heat = sync.temperature
        val rng = world.random

        // 堆温 > 4000：5×5×5 方块有几率着火
        if (heat > NuclearReactorSync.HEAT_FIRE_THRESHOLD) {
            for (dx in -2..2) for (dy in -2..2) for (dz in -2..2) {
                if (rng.nextFloat() > 0.02f) continue
                val p = pos.add(dx, dy, dz)
                if (!world.isInBuildLimit(p)) continue
                if (AbstractFireBlock.canPlaceAt(world, p, Direction.UP)) {
                    world.setBlockState(p, Blocks.FIRE.defaultState)
                }
            }
        }

        // 堆温 > 5000：5×5×5 水有几率蒸发
        if (heat > NuclearReactorSync.HEAT_EVAPORATE_THRESHOLD) {
            for (dx in -2..2) for (dy in -2..2) for (dz in -2..2) {
                if (rng.nextFloat() > 0.02f) continue
                val p = pos.add(dx, dy, dz)
                if (!world.isInBuildLimit(p)) continue
                if (world.getBlockState(p).isOf(Blocks.WATER)) {
                    world.setBlockState(p, Blocks.AIR.defaultState)
                }
            }
        }

        // 堆温 > 7000：7×7×7 生物有几率受伤（防化服可挡）
        if (heat > NuclearReactorSync.HEAT_DAMAGE_THRESHOLD) {
            val box = Box(pos).expand(3.5)
            val entities = world.getEntitiesByClass(LivingEntity::class.java, box) { true }
            for (entity in entities) {
                if (rng.nextFloat() > 0.05f) continue
                if (hasFullHazmat(entity)) continue
                val serverWorld = world as? net.minecraft.server.world.ServerWorld ?: continue
                val damageSource = createNuclearHeatDamageSource(serverWorld)
                entity.damage(damageSource, 2f)
            }
        }

        // 堆温 > 8500：5×5×5 方块有几率变岩浆
        if (heat > NuclearReactorSync.HEAT_LAVA_THRESHOLD) {
            for (dx in -2..2) for (dy in -2..2) for (dz in -2..2) {
                if (rng.nextFloat() > 0.01f) continue
                val p = pos.add(dx, dy, dz)
                if (!world.isInBuildLimit(p)) continue
                val state = world.getBlockState(p)
                val block = state.block
                if (block === Blocks.BEDROCK || block is NuclearReactorBlock || block is ReactorChamberBlock || block is MachineBlock || block is BaseCableBlock) continue
                if (state.isSolidBlock(world, p) && state.getHardness(world, p) >= 0f) {
                    world.setBlockState(p, Blocks.LAVA.defaultState)
                }
            }
        }
    }

    private fun hasFullHazmat(entity: LivingEntity): Boolean {
        val helmet = entity.getEquippedStack(EquipmentSlot.HEAD)
        val chest = entity.getEquippedStack(EquipmentSlot.CHEST)
        val legs = entity.getEquippedStack(EquipmentSlot.LEGS)
        fun isHazmat(stack: ItemStack): Boolean {
            if (stack.isEmpty || stack.item !is ArmorItem) return false
            val id = Registries.ITEM.getId(stack.item)
            return "hazmat" in id.path
        }
        return isHazmat(helmet) && isHazmat(chest) && isHazmat(legs)
    }

    private fun createNuclearHeatDamageSource(world: net.minecraft.server.world.ServerWorld): net.minecraft.entity.damage.DamageSource {
        val registry = world.registryManager.get(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE)
        val key = net.minecraft.registry.RegistryKey.of(
            net.minecraft.registry.RegistryKeys.DAMAGE_TYPE,
            Identifier(Ic2_120.MOD_ID, "nuclear_heat")
        )
        val entry = registry.getEntry(key).orElse(null)
            ?: registry.getEntry(
                net.minecraft.registry.RegistryKey.of(
                    net.minecraft.registry.RegistryKeys.DAMAGE_TYPE,
                    Identifier("minecraft", "lava")
                )
            ).orElseThrow()
        return net.minecraft.entity.damage.DamageSource(entry)
    }

    companion object {
        const val MAX_SLOTS = 81
    }
}


