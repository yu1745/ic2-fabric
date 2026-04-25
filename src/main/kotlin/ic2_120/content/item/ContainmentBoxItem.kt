package ic2_120.content.item

import ic2_120.content.screen.ContainmentBoxScreenHandler
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.data.server.recipe.RecipeExporter
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound

import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World
import java.util.function.Consumer
import ic2_120.getCustomData
import ic2_120.getOrCreateCustomData

/**
 * 防辐射容纳盒：12 格物品栏，数据存于物品 NBT；禁止再放入容纳盒避免嵌套。
 */
@ModItem(name = "containment_box", tab = CreativeTab.IC2_MATERIALS, group = "containment")
class ContainmentBoxItem : Item(Item.Settings().maxCount(1)) {

    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = player.getStackInHand(hand)
        if (world.isClient) return TypedActionResult.success(stack)
        if (stack.isEmpty || stack.item !is ContainmentBoxItem) return TypedActionResult.pass(stack)

        player.openHandledScreen(object : ExtendedScreenHandlerFactory {
            override fun getDisplayName(): Text = Text.translatable("item.ic2_120.containment_box")

            override fun writeScreenOpeningData(
                serverPlayer: net.minecraft.server.network.ServerPlayerEntity,
                buf: PacketByteBuf
            ) {
                buf.writeEnumConstant(hand)
            }

            override fun createMenu(
                syncId: Int,
                playerInventory: PlayerInventory,
                player: PlayerEntity
            ): net.minecraft.screen.ScreenHandler {
                return ContainmentBoxScreenHandler(syncId, playerInventory, hand)
            }
        })

        return TypedActionResult.success(stack, true)
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeExporter>) {
            val chest = Items.CHEST
            val leadCasing = LeadCasing::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ContainmentBoxItem::class.instance(), 1)
                .pattern("LLL")
                .pattern("LCL")
                .pattern("LLL")
                .input('L', leadCasing)
                .input('C', chest)
                .criterion(hasItem(chest), conditionsFromItem(chest))
                .criterion(hasItem(leadCasing), conditionsFromItem(leadCasing))
                .offerTo(exporter, ContainmentBoxItem::class.id())
        }
    }
}

/** 容纳盒内部栏：与手持物品 NBT 同步 */
class ContainmentBoxInventory(
    private val player: PlayerEntity,
    private val hand: Hand
) : SimpleInventory(ContainmentBoxInventory.SIZE) {

    init {
        loadFromStack()
    }

    override fun markDirty() {
        super.markDirty()
        saveToStack()
    }

    override fun onClose(player: PlayerEntity) {
        super.onClose(player)
        saveToStack()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean {
        val s = player.getStackInHand(hand)
        return !s.isEmpty && s.item is ContainmentBoxItem
    }

    private fun loadFromStack() {
        for (i in 0 until SIZE) setStack(i, ItemStack.EMPTY)
        val stack = player.getStackInHand(hand)
        if (stack.isEmpty || stack.item !is ContainmentBoxItem) return
        val root = stack.getCustomData() ?: return
        if (!root.contains(NBT_KEY)) return
        val tag = root.getCompound(NBT_KEY)
        val list = DefaultedList.ofSize(SIZE, ItemStack.EMPTY)
        Inventories.readNbt(tag, list)
        for (i in 0 until SIZE) setStack(i, list[i])
    }

    private fun saveToStack() {
        val stack = player.getStackInHand(hand)
        if (stack.isEmpty || stack.item !is ContainmentBoxItem) return
        val nbt = stack.getOrCreateCustomData()
        val tag = NbtCompound()
        val list = DefaultedList.ofSize(SIZE, ItemStack.EMPTY)
        for (i in 0 until SIZE) list[i] = getStack(i).copy()
        Inventories.writeNbt(tag, list)
        nbt.put(NBT_KEY, tag)
    }

    companion object {
        const val SIZE = 12
        const val NBT_KEY = "ContainmentItems"
    }
}
