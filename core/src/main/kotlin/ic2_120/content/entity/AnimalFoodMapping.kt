package ic2_120.content.entity

import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import kotlin.jvm.JvmStatic

/**
 * 动物食物映射系统
 *
 * 仅包含产生肉和皮革的动物。
 * 每种动物映射到其对应的繁殖食物列表。
 */
object AnimalFoodMapping {
    // 仅包含产生肉和皮革的动物
    private val FOOD_MAP = mapOf<Identifier, List<Item>>(
        // 猪（猪肉）
        Identifier.fromNamespaceAndPath("minecraft", "pig") to listOf(Items.CARROT),
        // 牛/哞菇（牛肉、皮革、牛奶）
        Identifier.fromNamespaceAndPath("minecraft", "cow") to listOf(Items.WHEAT),
        Identifier.fromNamespaceAndPath("minecraft", "mooshroom") to listOf(Items.WHEAT),
        // 羊（羊肉、羊毛）
        Identifier.fromNamespaceAndPath("minecraft", "sheep") to listOf(Items.WHEAT),
        // 鸡（鸡肉、羽毛、鸡蛋）
        Identifier.fromNamespaceAndPath("minecraft", "chicken") to listOf(Items.WHEAT_SEEDS),
        // 兔子（兔子肉、兔子皮）
        Identifier.fromNamespaceAndPath("minecraft", "rabbit") to listOf(Items.CARROT, Items.DANDELION),
        // 马/驴/骡（皮革）
        Identifier.fromNamespaceAndPath("minecraft", "horse") to listOf(Items.GOLDEN_APPLE, Items.GOLDEN_CARROT),
        Identifier.fromNamespaceAndPath("minecraft", "donkey") to listOf(Items.GOLDEN_APPLE, Items.GOLDEN_CARROT),
        Identifier.fromNamespaceAndPath("minecraft", "mule") to listOf(Items.GOLDEN_APPLE, Items.GOLDEN_CARROT),
        // 羊驼（皮革）
        Identifier.fromNamespaceAndPath("minecraft", "llama") to listOf(Items.HAY_BLOCK)
    )

    /**
     * 获取指定动物的食物列表
     */
    @JvmStatic
    fun getFoodForAnimal(entity: AgeableMob): List<Item> {
        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
        return FOOD_MAP[entityId] ?: emptyList()
    }

    /**
     * 检查指定物品是否是该动物的食物
     */
    @JvmStatic
    fun isFoodForAnimal(entity: AgeableMob, item: Item): Boolean {
        return getFoodForAnimal(entity).contains(item)
    }

    /**
     * 检查动物是否在监管白名单中（即是否产生肉或皮革）
     */
    @JvmStatic
    fun isManagedAnimal(entity: AgeableMob): Boolean {
        val entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
        return FOOD_MAP.containsKey(entityId)
    }

    /**
     * 获取食物映射表
     */
    @JvmStatic
    fun getFoodMap(): Map<Identifier, List<Item>> = FOOD_MAP
}
