package ic2_120_advanced_solar_addon.content.block

import ic2_120_advanced_solar_addon.content.item.QuantumCore
import ic2_120_advanced_solar_addon.content.screen.SolarPanelScreenHandler
import ic2_120.content.block.MachineBlock
import ic2_120.content.energy.charge.BatteryChargerComponent
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer

// i18n: block.ic2_120_advanced_solar_addon.quantum_solar_panel
// zh_cn: 量子太阳能发电机
// en_us: Quantum Solar Panel
@ModBlock(name = "quantum_solar_panel", registerItem = true, tab = CreativeTab.IC2_SOLAR, group = "solar_panel")
class QuantumSolarPanelBlock : MachineBlock() {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        QuantumSolarPanelBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, QuantumSolarPanelBlockEntity::class.type()) { w, p, s, be -> (be as QuantumSolarPanelBlockEntity).tick(w, p, s) }

    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): NamedScreenHandlerFactory? {
        val be = world.getBlockEntity(pos)
        return be as? NamedScreenHandlerFactory
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (!world.isClient) {
            createScreenHandlerFactory(state, world, pos)?.let { factory ->
                player.openHandledScreen(factory)
            }
        }
        return ActionResult.SUCCESS
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val ultimateSolar = UltimateSolarPanelBlock::class.item()
            val quantumCore = QuantumCore::class.instance()

            // 8个终极混合太阳能 + 1个量子核心
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, QuantumSolarPanelBlock::class.item(), 1)
                .pattern("UUU")
                .pattern("UQU")
                .pattern("UUU")
                .input('U', ultimateSolar)
                .input('Q', quantumCore)
                .criterion(hasItem(ultimateSolar), conditionsFromItem(ultimateSolar))
                .offerTo(exporter, ic2_120_advanced_solar_addon.IC2AdvancedSolarAddon.id("quantum_solar_panel"))
        }
    }
}

@ModBlockEntity(block = QuantumSolarPanelBlock::class)
class QuantumSolarPanelBlockEntity(pos: BlockPos, state: BlockState) :
    SolarPanelBlockEntity(QuantumSolarPanelBlockEntity::class.type(), pos, state, dayPower = 4096, nightPower = 2048, maxStorage = 10000000, tier = 5, activeProperty = QuantumSolarPanelBlock.ACTIVE),
    Inventory {

    companion object {
        private const val CHARGE_SLOTS = 4
        private const val INVENTORY_NBT_KEY = "charge_inventory"
    }

    private val inventory = DefaultedList.ofSize(CHARGE_SLOTS, ItemStack.EMPTY)

    private val chargerComponents = (0 until CHARGE_SLOTS).map { slot ->
        BatteryChargerComponent(
            inventory = this,
            batterySlot = slot,
            machineTierProvider = { tier },
            machineEnergyProvider = { sync.amount },
            extractEnergy = { requested -> sync.extractEnergy(requested) },
            canChargeNow = { true }
        )
    }

    override fun getChargeSlotCount(): Int = CHARGE_SLOTS

    override fun getInventory(): Inventory? = if (getChargeSlotCount() > 0) this else null

    override fun getBlockName(): String = "quantum_solar_panel"

    // ====== Tick ======

    override fun tick(world: World, pos: BlockPos, state: BlockState) {
        super.tick(world, pos, state)

        for (charger in chargerComponents) {
            charger.tick()
        }
    }

    // ====== Inventory ======

    override fun size(): Int = CHARGE_SLOTS
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    // ====== NBT ======

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt.getCompound(INVENTORY_NBT_KEY), inventory)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.put(INVENTORY_NBT_KEY, Inventories.writeNbt(NbtCompound(), inventory))
    }

    // ====== Screen ======

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
        buf.writeVarInt(CHARGE_SLOTS)
        buf.writeVarInt(tier)
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler {
        return SolarPanelScreenHandler(
            syncId, playerInventory,
            ScreenHandlerContext.create(world!!, pos),
            syncedData,
            this, CHARGE_SLOTS, tier
        )
    }
}
