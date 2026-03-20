package ic2_120.content.block.machines

import ic2_120.content.block.WindKineticGeneratorBlock
import ic2_120.content.screen.WindKineticGeneratorScreenHandler
import ic2_120.content.sync.WindKineticGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.content.network.NetworkManager
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.slf4j.LoggerFactory
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import net.minecraft.block.Blocks
@ModBlockEntity(block = WindKineticGeneratorBlock::class)
class WindKineticGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), Inventory,
    net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    companion object {
        const val ROTOR_SLOT = 0
        private val ALLOWED_ROTORS = setOf("wooden_rotor", "iron_rotor", "steel_rotor", "carbon_rotor")
        private val LOGGER = LoggerFactory.getLogger("ic2_120/WindKineticGenerator")
        private const val PIXEL = 1.0f / 16.0f
    }

    override val tier: Int = 1
    private val inventory = DefaultedList.ofSize(1, ItemStack.EMPTY)

    /** 同步数据（服务端） */
    private val syncedData = SyncedData(this)
    private val kineticSync = WindKineticGeneratorSync(syncedData)

    /** 转子是否被阻挡（true = 卡住停转） */
    var isStuck: Boolean
        get() = kineticSync.isStuck != 0
        private set(value) {
            kineticSync.isStuck = if (value) 1 else 0
        }

    /** 卡住时的转子角度（度，0-359） */
    var stuckAngle: Float
        get() = kineticSync.stuckAngle.toFloat()
        private set(value) {
            kineticSync.stuckAngle = value.toInt()
        }

    /**
     * 客户端接收网络包更新转子状态
     */
    fun receiveRotorState(isStuck: Boolean, stuckAngle: Float) {
        kineticSync.isStuck = if (isStuck) 1 else 0
        kineticSync.stuckAngle = stuckAngle.toInt()
        markDirty() // 标记为 dirty 以触发更新
        LOGGER.info("[WindKineticGenerator Client] Received: isStuck=$isStuck angle=${stuckAngle.toInt()}")
    }

    constructor(pos: BlockPos, state: BlockState) : this(
        WindKineticGeneratorBlockEntity::class.type(),
        pos,
        state
    )

    override fun getInventory(): Inventory = this

    fun getRotorStack(): ItemStack = inventory[ROTOR_SLOT]

    fun onUse(player: PlayerEntity, hand: Hand): ActionResult {
        val held = player.getStackInHand(hand)
        val rotor = inventory[ROTOR_SLOT]

        // 空手右键取出转子
        if (held.isEmpty) {
            if (rotor.isEmpty) return ActionResult.PASS
            val extracted = rotor.copy()
            inventory[ROTOR_SLOT] = ItemStack.EMPTY
            markDirtyAndSync()
            if (!player.giveItemStack(extracted)) {
                player.dropItem(extracted, false)
            }
            return ActionResult.SUCCESS
        }

        // 手持非转子物品：不处理
        if (!isAllowedRotor(held)) return ActionResult.PASS

        // 已有转子时不覆盖
        if (!rotor.isEmpty) return ActionResult.PASS

        val inserted = held.copy()
        inserted.count = 1
        inventory[ROTOR_SLOT] = inserted
        if (!player.abilities.creativeMode) {
            held.decrement(1)
        }
        markDirtyAndSync()
        return ActionResult.SUCCESS
    }

    private fun isAllowedRotor(stack: ItemStack): Boolean {
        val id = Registries.ITEM.getId(stack.item)
        return ALLOWED_ROTORS.contains(id.path)
    }

    private fun getRotorRadius(stack: ItemStack): Float {
        return when (Registries.ITEM.getId(stack.item).path) {
            "wooden_rotor" -> 2.0f
            "iron_rotor" -> 3.0f
            "steel_rotor" -> 4.0f
            "carbon_rotor" -> 5.0f
            else -> 0.0f
        }
    }

    /**
     * 服务端 tick：检测转子是否被前方的方块阻挡。
     *
     * 检查逻辑：
     * - 以转子中心（在方块前方 0.5 格）为原点，在垂直于朝向的平面上，
     *   检查半径范围内的所有固体方块。
     * - 若某方块位于叶片旋转路径上，则认为叶片被卡住，
     *   计算并记录当前转子的实际旋转角度。
     * - 卡住后转子停止旋转，渲染器固定显示在 stuckAngle。
     */
    fun tick(world: World, pos: BlockPos, state: BlockState) {
        val rotor = inventory[ROTOR_SLOT]
        if (rotor.isEmpty) {
            if (isStuck) {
                isStuck = false
                kineticSync.stuckAngle = 0
                markDirtyAndSync()
                NetworkManager.sendWindRotorStateToNearby(world, pos, false, 0f)
                LOGGER.info("[WindKineticGenerator] RELEASED (no rotor)")
            }
            return
        }

        val rotorRadius = getRotorRadius(rotor)
        if (rotorRadius <= 0f) return

        val facing = state.getOrEmpty(Properties.HORIZONTAL_FACING).orElse(Direction.NORTH)

        // 计算当前转子的实际旋转角度（仅服务端）
        val currentAngle = if (!world.isClient) {
            (world.time * 1.8f) % 360.0f
        } else {
            0f // 客户端不计算碰撞
        }

        val collision = findBladeCollision(world, pos, facing, rotorRadius, currentAngle)

        if (collision != null) {
            if (!isStuck) {
                // 刚卡住：记录当前角度
                isStuck = true
                stuckAngle = currentAngle
                markDirtyAndSync()
                NetworkManager.sendWindRotorStateToNearby(world, pos, true, currentAngle)
                LOGGER.info("[WindKineticGenerator] STUCK at angle ${currentAngle.toInt()}")
            }
        } else {
            if (isStuck) {
                // 恢复正常
                isStuck = false
                stuckAngle = 0f
                markDirtyAndSync()
                NetworkManager.sendWindRotorStateToNearby(world, pos, false, 0f)
                LOGGER.info("[WindKineticGenerator] RELEASED")
            }
        }
    }

    /**
     * 检测叶片是否被前方的固体方块阻挡。
     *
     * 检查转子旋转平面内的所有固体方块，通过采样叶片路径上的多个点来判断碰撞。
     *
     * @param currentAngle 当前转子旋转角度（度）
     * @return 是否有碰撞
     */
    private fun findBladeCollision(
        world: World,
        pos: BlockPos,
        facing: Direction,
        rotorRadius: Float,
        currentAngle: Float
    ): Boolean {
        // 转子中心在方块中心的偏移
        val cx = pos.x + 0.5f
        val cy = pos.y + 0.5f
        val cz = pos.z + 0.5f

        // 转子平面前方的方块坐标偏移（转子在前方 1 格）
        val frontBlockOffset = when (facing) {
            Direction.NORTH -> -1
            Direction.SOUTH -> 1
            Direction.EAST -> 1
            Direction.WEST -> -1
            else -> return false
        }

        // 四个叶片在模型空间的角度
        val bladeAngles = listOf(0f, 90f, 180f, 270f)

        for (bladeAngle in bladeAngles) {
            // 叶片旋转角度（加上当前旋转角度）
            val bladeRad = Math.toRadians((bladeAngle + currentAngle).toDouble()).toFloat()

            // 沿着叶片长度采样多个点（从内到外）
            val bladeInnerOffset = 1.0f * PIXEL // BLADE_INNER_OFFSET
            val sampleCount = 5 // 采样点数
            val stepSize = (rotorRadius - bladeInnerOffset) / sampleCount

            for (i in 0..sampleCount) {
                val distance = bladeInnerOffset + stepSize * i

                // 根据朝向计算叶片点在旋转平面上的坐标
                val checkX: Int
                val checkY: Int
                val checkZ: Int

                when (facing) {
                    Direction.NORTH -> {
                        // 朝北：转子在 z-1 平面，叶片在 x-y 平面旋转
                        val pointX = cx + cos(bladeRad.toDouble()).toFloat() * distance
                        val pointY = cy + sin(bladeRad.toDouble()).toFloat() * distance
                        checkX = floor(pointX).toInt()
                        checkY = floor(pointY).toInt()
                        checkZ = pos.z - 1
                    }
                    Direction.SOUTH -> {
                        // 朝南：转子在 z+1 平面，叶片在 x-y 平面旋转（但旋转了 180°）
                        val pointX = cx - cos(bladeRad.toDouble()).toFloat() * distance
                        val pointY = cy + sin(bladeRad.toDouble()).toFloat() * distance
                        checkX = floor(pointX).toInt()
                        checkY = floor(pointY).toInt()
                        checkZ = pos.z + 1
                    }
                    Direction.EAST -> {
                        // 朝东：转子在 x+1 平面，叶片在 y-z 平面旋转
                        val pointY = cy + cos(bladeRad.toDouble()).toFloat() * distance
                        val pointZ = cz - sin(bladeRad.toDouble()).toFloat() * distance
                        checkX = pos.x + 1
                        checkY = floor(pointY).toInt()
                        checkZ = floor(pointZ).toInt()
                    }
                    Direction.WEST -> {
                        // 朝西：转子在 x-1 平面，叶片在 y-z 平面旋转
                        val pointY = cy + cos(bladeRad.toDouble()).toFloat() * distance
                        val pointZ = cz + sin(bladeRad.toDouble()).toFloat() * distance
                        checkX = pos.x - 1
                        checkY = floor(pointY).toInt()
                        checkZ = floor(pointZ).toInt()
                    }
                    else -> continue
                }

                // 检查该方块是否为实体方块（会阻挡转子旋转）
                val checkPos = BlockPos(checkX, checkY, checkZ)
                val blockState = world.getBlockState(checkPos)

                if (blockState.isOpaqueFullCube(world, checkPos)) {
                    LOGGER.info("[WindKineticGenerator] COLLISION at {} facing={} bladeAngle={} distance={} block={}",
                        checkPos, facing, (bladeAngle + currentAngle).toInt(), distance, blockState.block)
                    return true
                }
            }
        }

        return false
    }

    /** 复制渲染器的 yawFromFacing 逻辑，保证碰撞检测与渲染方向一致 */
    private fun yawFromFacing(facing: Direction): Float =
        when (facing) {
            Direction.NORTH -> 0.0f
            Direction.SOUTH -> 180.0f
            Direction.WEST -> 90.0f
            Direction.EAST -> -90.0f
            else -> 0.0f
        }

    private fun markDirtyAndSync() {
        markDirty()
        val world = world ?: return
        if (!world.isClient) {
            val state = world.getBlockState(pos)
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS)
            // 立即将 BlockEntity NBT 同步到客户端，否则取出转子后螺旋桨渲染不会消失
            (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
        }
    }

    override fun size(): Int = 1

    override fun isEmpty(): Boolean = inventory[ROTOR_SLOT].isEmpty

    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val result = Inventories.splitStack(inventory, slot, amount)
        if (!result.isEmpty) {
            markDirtyAndSync()
        }
        return result
    }

    override fun removeStack(slot: Int): ItemStack {
        val result = Inventories.removeStack(inventory, slot)
        if (!result.isEmpty) {
            markDirtyAndSync()
        }
        return result
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot != ROTOR_SLOT) return
        if (stack.isEmpty) {
            inventory[ROTOR_SLOT] = ItemStack.EMPTY
        } else if (isAllowedRotor(stack)) {
            val copy = stack.copy()
            copy.count = 1
            inventory[ROTOR_SLOT] = copy
        }
        markDirtyAndSync()
    }

    override fun clear() {
        inventory[ROTOR_SLOT] = ItemStack.EMPTY
        markDirtyAndSync()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        // Inventories.readNbt 不会在 nbt 中缺失该槽位时自动清空旧值，先清槽避免客户端残留渲染状态
        inventory[ROTOR_SLOT] = ItemStack.EMPTY
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
    }

    override fun toInitialChunkDataNbt(): NbtCompound = createNbt()

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> = BlockEntityUpdateS2CPacket.create(this)

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(0)
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.wind_kinetic_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        WindKineticGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            ScreenHandlerContext.create(world!!, pos),
            ArrayPropertyDelegate(0)
        )
}
