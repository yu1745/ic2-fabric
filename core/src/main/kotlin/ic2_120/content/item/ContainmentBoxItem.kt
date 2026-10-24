package ic2_120.content.item

import net.minecraft.data.recipes.RecipeProvider.getHasName

import net.minecraft.data.recipes.RecipeProvider.has

import ic2_120.compat.isClient
import ic2_120.compat.world

import ic2_120.content.screen.ContainmentBoxScreenHandler
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.ContainerHelper
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag

import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.core.NonNullList
import net.minecraft.world.level.Level
import ic2_120.editCustomData
import ic2_120.getCustomData
import net.minecraft.network.FriendlyByteBuf
import io.netty.buffer.Unpooled

/**
 * 防辐射容纳盒：12 格物品栏，数据存于物品 NBT；禁止再放入容纳盒避免嵌套。
 */
@ModItem(name = "containment_box", tab = CreativeTab.IC2_MATERIALS, group = "containment")
class ContainmentBoxItem : Item(Item.Properties().maxStackSize(1)) {

    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (world.isClient) return InteractionResultHolder.success(stack)
        if (stack.isEmpty || stack.item !is ContainmentBoxItem) return InteractionResultHolder.pass(stack)

        player.openMenu(object : ExtendedScreenHandlerFactory<FriendlyByteBuf> {
            override fun getDisplayName(): Component = Component.translatable("item.ic2_120.containment_box")

            override fun getScreenOpeningData(serverPlayer: net.minecraft.server.level.ServerPlayer): FriendlyByteBuf {
                val buf = FriendlyByteBuf(Unpooled.buffer())
                buf.writeEnumConstant(hand)
                return buf
            }

            override fun createMenu(
                syncId: Int,
                playerInventory: Inventory,
                player: Player
            ): net.minecraft.world.inventory.AbstractContainerMenu {
                return ContainmentBoxScreenHandler(syncId, playerInventory, hand)
            }
        })

        return InteractionResultHolder.success(stack)
    }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: RecipeOutput) {
            val chest = Items.CHEST
            val leadCasing = LeadCasing::class.instance()
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ContainmentBoxItem::class.instance(), 1)
                .pattern("LLL")
                .pattern("LCL")
                .pattern("LLL")
                .define('L', leadCasing)
                .define('C', chest)
                .unlockedBy(getHasName(chest), has(chest))
                .unlockedBy(getHasName(leadCasing), has(leadCasing))
                .save(exporter, ContainmentBoxItem::class.id())
        }
    }
}

/** 容纳盒内部栏：与手持物品 NBT 同步 */
class ContainmentBoxInventory(
    private val player: Player,
    private val hand: InteractionHand
) : SimpleContainer(ContainmentBoxInventory.SIZE) {

    private var loading: Boolean = false

    init {
        loadFromStack()
    }

    override fun setChanged() {
        super.setChanged()
        if (!loading) saveToStack()
    }

    override fun onClose(player: Player) {
        super.onClose(player)
        saveToStack()
    }

    override fun stillValid(player: Player): Boolean {
        val s = player.getItemInHand(hand)
        return !s.isEmpty && s.item is ContainmentBoxItem
    }

    private fun loadFromStack() {
        loading = true
        try {
        for (i in 0 until SIZE) setStack(i, ItemStack.EMPTY)
        val stack = player.getItemInHand(hand)
        if (stack.isEmpty || stack.item !is ContainmentBoxItem) return
        val root = stack.getCustomData() ?: return
        if (!root.hasProperty(NBT_KEY)) return
        val tag = root.getCompound(NBT_KEY)
        val list = NonNullList.withSize(SIZE, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(tag, list, player.world.registryManager)
        for (i in 0 until SIZE) setStack(i, list[i])
        } finally {
            loading = false
        }
        saveToStack()
    }

    private fun saveToStack() {
        val stack = player.getItemInHand(hand)
        if (stack.isEmpty || stack.item !is ContainmentBoxItem) return
        val tag = CompoundTag()
        val list = NonNullList.withSize(SIZE, ItemStack.EMPTY)
        for (i in 0 until SIZE) list[i] = getStack(i).copy()
        ContainerHelper.saveAllItems(tag, list, player.world.registryManager)
        stack.editCustomData { it.put(NBT_KEY, tag) }
    }

    companion object {
        const val SIZE = 12
        const val NBT_KEY = "ContainmentItems"
    }
}
