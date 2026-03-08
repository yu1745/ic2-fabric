package ic2_120.registry

import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.ModCreativeTab
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.annotation.RegisterEnergy
import net.minecraft.util.math.Direction
import team.reborn.energy.api.EnergyStorage
import team.reborn.energy.api.base.SimpleSidedEnergyContainer
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import org.slf4j.LoggerFactory
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberFunctions

/**
 * 基于类注解的注册扫描器。
 * 扫描指定包中的类，根据类上的注解自动创建实例并注册。
 *
 * 特性：
 * - 直接在 Block/Item 类上添加注解
 * - 支持类级别的创造模式物品栏
 * - 自动创建实例并注册
 * - 支持按包路径扫描
 */
object ClassScanner {

    private val logger = LoggerFactory.getLogger("ic2_120/ClassScanner")

    // 跟踪每个物品栏应该包含的物品ID及分组（group 用于排序，相同 group 排在一起；空字符串表示不分组）
    private val tabItems = mutableMapOf<CreativeTab, MutableList<Pair<Identifier, String>>>()

    /** 方块类 -> 注册名（path），供 ModBlockEntity/ModScreenHandler 等从 block 解析 name */
    private val blockClassToName = mutableMapOf<kotlin.reflect.KClass<*>, String>()

    /**
     * 扫描并注册所有带注解的类。
     *
     * @param modId 模组命名空间
     * @param packageNames 要扫描的包名列表
     */
    fun scanAndRegister(modId: String, packageNames: List<String>) {
        logger.info("开始扫描包进行自动注册: {}", packageNames)

        val tabClasses = mutableListOf<TabClassInfo>()
        val blockClasses = mutableListOf<BlockClassInfo>()
        val blockEntityClasses = mutableListOf<BlockEntityClassInfo>()
        val screenHandlerClasses = mutableListOf<ScreenHandlerClassInfo>()
        val itemClasses = mutableListOf<ItemClassInfo>()

        // 扫描所有包中的类
        for (packageName in packageNames) {
            scanPackage(packageName, tabClasses, blockClasses, blockEntityClasses, screenHandlerClasses, itemClasses)
        }

        logger.info("扫描完成: {} 个物品栏类, {} 个方块类, {} 个方块实体类, {} 个 ScreenHandler 类, {} 个物品类",
            tabClasses.size, blockClasses.size, blockEntityClasses.size, screenHandlerClasses.size, itemClasses.size)

        // 清空之前的映射
        tabItems.clear()
        blockClassToName.clear()
        TransparentBlockRegistry.clear()

        // 按顺序注册：方块 → 方块实体类型 → ScreenHandler → 物品 → 物品栏
        registerBlocks(modId, blockClasses)
        registerBlockEntities(modId, blockEntityClasses)
        registerScreenHandlers(modId, screenHandlerClasses)
        registerItems(modId, itemClasses)
        registerCreativeTabs(modId, tabClasses)

        logger.info("自动注册完成")
    }

    /**
     * 扫描指定包中的类。
     */
    private fun scanPackage(
        packageName: String,
        tabClasses: MutableList<TabClassInfo>,
        blockClasses: MutableList<BlockClassInfo>,
        blockEntityClasses: MutableList<BlockEntityClassInfo>,
        screenHandlerClasses: MutableList<ScreenHandlerClassInfo>,
        itemClasses: MutableList<ItemClassInfo>
    ) {
        try {
            logger.info("扫描包: {}", packageName)

            val classLoader = Thread.currentThread().contextClassLoader
            val packagePath = packageName.replace('.', '/')
            val resources = classLoader.getResources(packagePath)

            while (resources.hasMoreElements()) {
                val url = resources.nextElement()
                val protocol = url.protocol

                when (protocol) {
                    "file" -> scanDirectory(url.path, packageName, tabClasses, blockClasses, blockEntityClasses, screenHandlerClasses, itemClasses)
                    "jar" -> scanJarFile(url, packageName, tabClasses, blockClasses, blockEntityClasses, screenHandlerClasses, itemClasses)
                    else -> logger.warn("未支持的协议: {}", protocol)
                }
            }
        } catch (e: Exception) {
            logger.error("扫描包 {} 失败: {}", packageName, e.message, e)
        }
    }

    private fun scanDirectory(
        path: String,
        packageName: String,
        tabClasses: MutableList<TabClassInfo>,
        blockClasses: MutableList<BlockClassInfo>,
        blockEntityClasses: MutableList<BlockEntityClassInfo>,
        screenHandlerClasses: MutableList<ScreenHandlerClassInfo>,
        itemClasses: MutableList<ItemClassInfo>
    ) {
        val dir = java.io.File(path)
        if (!dir.exists() || !dir.isDirectory) return

        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> {
                    scanDirectory(file.path, packageName + "." + file.name, tabClasses, blockClasses, blockEntityClasses, screenHandlerClasses, itemClasses)
                }
                file.name.endsWith(".class") -> {
                    val className = packageName + "." + file.name.removeSuffix(".class")
                    processClass(className, tabClasses, blockClasses, blockEntityClasses, screenHandlerClasses, itemClasses)
                }
            }
        }
    }

    private fun scanJarFile(
        url: java.net.URL,
        packageName: String,
        tabClasses: MutableList<TabClassInfo>,
        blockClasses: MutableList<BlockClassInfo>,
        blockEntityClasses: MutableList<BlockEntityClassInfo>,
        screenHandlerClasses: MutableList<ScreenHandlerClassInfo>,
        itemClasses: MutableList<ItemClassInfo>
    ) {
        // jar: URL 格式: jar:file:/path.jar!/package/path
        // 需要提取文件路径并正确解码 URL 编码字符（如 %20 -> 空格）
        val jarPathPart = url.path.substringBefore("!")
        // 移除 "file:" 或 "file:///" 前缀（如果存在）
        val cleanPath = when {
            jarPathPart.startsWith("file:") -> jarPathPart.substring(5)
            jarPathPart.startsWith("file:///") -> jarPathPart.substring(8)
            else -> jarPathPart
        }
        // URL 解码路径中的特殊字符
        val decodedPath = java.net.URLDecoder.decode(cleanPath, "UTF-8")
        val jarFile = java.util.jar.JarFile(decodedPath)

        val entries = jarFile.entries()
        val packagePath = packageName.replace('.', '/') + "/"

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name

            if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                val className = entryName
                    .removeSuffix(".class")
                    .replace('/', '.')
                processClass(className, tabClasses, blockClasses, blockEntityClasses, screenHandlerClasses, itemClasses)
            }
        }

        jarFile.close()
    }

    private fun processClass(
        className: String,
        tabClasses: MutableList<TabClassInfo>,
        blockClasses: MutableList<BlockClassInfo>,
        blockEntityClasses: MutableList<BlockEntityClassInfo>,
        screenHandlerClasses: MutableList<ScreenHandlerClassInfo>,
        itemClasses: MutableList<ItemClassInfo>
    ) {
        try {
            val clazz = Class.forName(className).kotlin
            val modCreativeTab = clazz.findAnnotation<ModCreativeTab>()
            val modBlock = clazz.findAnnotation<ModBlock>()
            val modBlockEntity = clazz.findAnnotation<ModBlockEntity>()
            val modScreenHandler = clazz.findAnnotation<ModScreenHandler>()
            val modItem = clazz.findAnnotation<ModItem>()

            when {
                modCreativeTab != null -> {
                    tabClasses.add(TabClassInfo(clazz, modCreativeTab))
                    logger.debug("发现 @ModCreativeTab: {}", className)
                }
                modBlock != null -> {
                    if (!clazz.isSubclassOf(Block::class)) {
                        logger.warn("@ModBlock 类 {} 不是 Block 的子类", className)
                        return
                    }
                    blockClasses.add(BlockClassInfo(clazz, modBlock))
                    logger.debug("发现 @ModBlock: {}", className)
                }
                modBlockEntity != null -> {
                    if (!clazz.isSubclassOf(BlockEntity::class)) {
                        logger.warn("@ModBlockEntity 类 {} 不是 BlockEntity 的子类", className)
                        return
                    }
                    blockEntityClasses.add(BlockEntityClassInfo(clazz, modBlockEntity))
                    logger.debug("发现 @ModBlockEntity: {}", className)
                }
                modScreenHandler != null -> {
                    if (!clazz.isSubclassOf(ScreenHandler::class)) {
                        logger.warn("@ModScreenHandler 类 {} 不是 ScreenHandler 的子类", className)
                        return
                    }
                    screenHandlerClasses.add(ScreenHandlerClassInfo(clazz, modScreenHandler))
                    logger.debug("发现 @ModScreenHandler: {}", className)
                }
                modItem != null -> {
                    if (!clazz.isSubclassOf(Item::class)) {
                        logger.warn("@ModItem 类 {} 不是 Item 的子类", className)
                        return
                    }
                    itemClasses.add(ItemClassInfo(clazz, modItem))
                    logger.debug("发现 @ModItem: {}", className)
                }
            }
        } catch (e: ClassNotFoundException) {
            logger.warn("无法加载类: {}", className)
        } catch (e: NoClassDefFoundError) {
            // 忽略依赖缺失的类
        } catch (e: Exception) {
            logger.debug("处理类 {} 时出错: {}", className, e.message)
        }
    }

    private fun registerCreativeTabs(modId: String, tabClasses: List<TabClassInfo>) {
        for ((clazz, annotation) in tabClasses) {
            try {
                val name = if (annotation.name.isNotEmpty()) {
                    annotation.name
                } else {
                    camelToSnake(clazz.simpleName ?: "unknown")
                }
                val id = Identifier(modId, name)

                // 创建 RegistryKey
                val tabKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, id)

                // 解析图标物品ID
                val iconId = Identifier(modId, annotation.iconItem)

                // 获取这个物品栏对应的枚举值
                val tabEnum = CreativeTab.values().find { it.id == name }
                    ?: run {
                        logger.error("找不到对应的物品栏枚举: {}", name)
                        continue
                    }

                // 获取应该添加到这个物品栏的物品ID列表，按 group 排序（相同 group 放一起，组内按 id 稳定排序）
                val rawEntries = tabItems[tabEnum] ?: emptyList()
                val itemIds = rawEntries
                    .sortedWith(compareBy({ it.second }, { it.first.toString() }))
                    .map { it.first }

                logger.info("物品栏 {} 包含 {} 个物品: {}", name, itemIds.size, itemIds)

                // 创建物品栏，使用 entries() 方法添加物品
                val itemGroup = FabricItemGroup.builder()
                    .icon { net.minecraft.item.ItemStack(Registries.ITEM.get(iconId)) }
                    .displayName(Text.translatable("itemGroup.$modId.$name"))
                    .entries { _, entries ->
                        // 添加所有属于这个物品栏的物品（已按 group 排序）
                        for (itemId in itemIds) {
                            val item = Registries.ITEM.get(itemId)
                            if (item != net.minecraft.item.Items.AIR) {
                                entries.add(item)
                            }
                        }
                    }
                    .build()

                // 注册到注册表
                Registry.register(Registries.ITEM_GROUP, tabKey, itemGroup)
                logger.info("已注册创造模式物品栏: {}", id)
            } catch (e: Exception) {
                logger.error("注册创造模式物品栏 {} 失败: {}", clazz.simpleName, e.message, e)
            }
        }
    }

    private fun registerScreenHandlers(modId: String, screenHandlerClasses: List<ScreenHandlerClassInfo>) {
        for ((clazz, annotation) in screenHandlerClasses) {
            try {
                val name = resolveNameFromBlockOrAnnotation(annotation.name, annotation.block, clazz.simpleName ?: "unknown")
                val id = Identifier(modId, name)
                val companion = clazz.companionObjectInstance
                    ?: error("@ModScreenHandler 类 ${clazz.simpleName} 需有 companion 对象并提供 fromBuffer")
                val fromBufferFn = clazz.companionObject?.memberFunctions?.find { it.name == "fromBuffer" }
                    ?: error("@ModScreenHandler 类 ${clazz.simpleName} 的 companion 需提供 fromBuffer(syncId, playerInventory, buf)")
                val type = ExtendedScreenHandlerType { syncId, playerInventory, buf ->
                    @Suppress("UNCHECKED_CAST")
                    fromBufferFn.call(companion, syncId, playerInventory, buf) as ScreenHandler
                }
                Registry.register(Registries.SCREEN_HANDLER, id, type)
                ScreenHandlerTypeStore.registerType(clazz, type)
                logger.debug("已注册 ScreenHandler 类型: {}", id)
            } catch (e: Exception) {
                logger.error("注册 ScreenHandler {} 失败: {}", clazz.simpleName, e.message, e)
            }
        }
    }

    private fun resolveNameFromBlockOrAnnotation(
        annotationName: String,
        blockClass: kotlin.reflect.KClass<*>,
        fallbackSimpleName: String
    ): String {
        if (blockClass != Any::class && blockClass.isSubclassOf(Block::class) && blockClassToName.containsKey(blockClass)) {
            return blockClassToName[blockClass]!!
        }
        return if (annotationName.isNotEmpty()) annotationName else camelToSnake(fallbackSimpleName)
    }

    private fun registerBlockEntities(modId: String, blockEntityClasses: List<BlockEntityClassInfo>) {
        for ((clazz, annotation) in blockEntityClasses) {
            try {
                val name = resolveNameFromBlockOrAnnotation(annotation.name, annotation.block, clazz.simpleName ?: "unknown")
                val id = Identifier(modId, name)
                val block = Registries.BLOCK.get(id)
                if (block == net.minecraft.block.Blocks.AIR && id.path != "air") {
                    logger.warn("方块实体 {} 对应的方块未找到: {}，请确保方块先于方块实体注册", clazz.simpleName, id)
                }
                val ctor = clazz.constructors.find { it.parameters.size == 2 }
                    ?: error("@ModBlockEntity 类 ${clazz.simpleName} 需提供 (BlockPos, BlockState) 构造函数")
                val factory = FabricBlockEntityTypeBuilder.Factory { pos: BlockPos, state: BlockState ->
                    @Suppress("UNCHECKED_CAST")
                    ctor.call(pos, state) as BlockEntity
                }
                @Suppress("UNCHECKED_CAST")
                val type = FabricBlockEntityTypeBuilder.create(factory, block).build() as BlockEntityType<BlockEntity>
                Registry.register(Registries.BLOCK_ENTITY_TYPE, id, type)
                BlockEntityTypeStore.registerType(clazz, type)
                logger.debug("已注册方块实体类型: {}", id)

                // 若存在 @RegisterEnergy 字段，则向 Energy API 注册 SIDED 查找
                val energyProperty = clazz.declaredMemberProperties.firstOrNull { it.hasAnnotation<RegisterEnergy>() }
                if (energyProperty != null) {
                    val prop = energyProperty
                    @Suppress("UNCHECKED_CAST")
                    EnergyStorage.SIDED.registerForBlockEntity({ be, direction ->
                        val storage = (prop as kotlin.reflect.KProperty1<Any, Any?>).get(be)
                        if (storage is SimpleSidedEnergyContainer) storage.getSideStorage(direction)
                        else storage as EnergyStorage
                    }, type)
                    logger.debug("已为方块实体 {} 注册 EnergyStorage（属性 {}）", clazz.simpleName, prop.name)
                } else {
                    val energyField = clazz.java.declaredFields.firstOrNull { it.isAnnotationPresent(RegisterEnergy::class.java) }
                    if (energyField != null) {
                        energyField.trySetAccessible()
                        EnergyStorage.SIDED.registerForBlockEntity({ be, direction ->
                            val storage = energyField.get(be)
                            if (storage is SimpleSidedEnergyContainer) storage.getSideStorage(direction)
                            else storage as EnergyStorage
                        }, type)
                        logger.debug("已为方块实体 {} 注册 EnergyStorage（字段 {}）", clazz.simpleName, energyField.name)
                    }
                }
            } catch (e: Exception) {
                logger.error("注册方块实体类型 {} 失败: {}", clazz.simpleName, e.message, e)
            }
        }
    }

    private fun registerBlocks(modId: String, blockClasses: List<BlockClassInfo>) {
        for ((clazz, annotation) in blockClasses) {
            try {
                val instance = clazz.createInstance() as Block

                val name = if (annotation.name.isNotEmpty()) {
                    annotation.name
                } else {
                    camelToSnake(clazz.simpleName ?: "unknown")
                }
                val id = Identifier(modId, name)

                // 注册方块
                Registry.register(Registries.BLOCK, id, instance)
                blockClassToName[clazz] = name
                if (annotation.transparent) {
                    TransparentBlockRegistry.add(id)
                }
                logger.debug("已注册方块: {}", id)

                // 注册方块物品
                if (annotation.registerItem) {
                    val blockItem = BlockItem(instance, FabricItemSettings())
                    Registry.register(Registries.ITEM, id, blockItem)
                    logger.debug("已注册方块物品: {}", id)

                    // 记录物品应该添加到哪个物品栏（带 group 以便排序）
                    if (annotation.tab != CreativeTab.MINECRAFT_MISC) {
                        tabItems.getOrPut(annotation.tab) { mutableListOf() }.add(id to annotation.group)
                    }
                }
            } catch (e: Exception) {
                logger.error("注册方块 {} 失败: {}", clazz.simpleName, e.message, e)
            }
        }
    }

    private fun registerItems(modId: String, itemClasses: List<ItemClassInfo>) {
        for ((clazz, annotation) in itemClasses) {
            try {
                val instance = clazz.createInstance() as Item

                val name = if (annotation.name.isNotEmpty()) {
                    annotation.name
                } else {
                    camelToSnake(clazz.simpleName ?: "unknown")
                }
                val id = Identifier(modId, name)

                // 注册物品
                Registry.register(Registries.ITEM, id, instance)
                logger.debug("已注册物品: {}", id)

                // 记录物品应该添加到哪个物品栏（带 group 以便排序）
                if (annotation.tab != CreativeTab.MINECRAFT_MISC) {
                    tabItems.getOrPut(annotation.tab) { mutableListOf() }.add(id to annotation.group)
                }
            } catch (e: Exception) {
                logger.error("注册物品 {} 失败: {}", clazz.simpleName, e.message, e)
            }
        }
    }

    private fun camelToSnake(str: String): String {
        return str.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }

    private fun parseItemGroupKey(modId: String, tab: CreativeTab): RegistryKey<ItemGroup> {
        val id = Identifier(tab.getNamespacedId(modId))
        return RegistryKey.of(RegistryKeys.ITEM_GROUP, id)
    }

    private data class TabClassInfo(
        val clazz: kotlin.reflect.KClass<*>,
        val annotation: ModCreativeTab
    )

    private data class BlockClassInfo(
        val clazz: kotlin.reflect.KClass<*>,
        val annotation: ModBlock
    )

    private data class BlockEntityClassInfo(
        val clazz: kotlin.reflect.KClass<*>,
        val annotation: ModBlockEntity
    )

    private data class ScreenHandlerClassInfo(
        val clazz: kotlin.reflect.KClass<*>,
        val annotation: ModScreenHandler
    )

    private data class ItemClassInfo(
        val clazz: kotlin.reflect.KClass<*>,
        val annotation: ModItem
    )
}
