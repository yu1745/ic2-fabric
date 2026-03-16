package ic2_120.registry

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.type
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.type
import ic2_120.registry.annotation.ModCreativeTab
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import ic2_120.registry.annotation.ModScreenHandler
import ic2_120.registry.type
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.type
import net.minecraft.util.math.Direction
import team.reborn.energy.api.EnergyStorage
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
import net.minecraft.item.ItemStack
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
import kotlin.reflect.full.superclasses
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import java.util.function.Consumer
import net.minecraft.data.server.recipe.RecipeJsonProvider

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
    // 同时存储类类型，用于在创建物品栏时检查是否需要添加多个电量变体
    private val tabItems = mutableMapOf<CreativeTab, MutableList<TabEntry>>()

    /** 方块类 -> 注册名（path），供 ModBlockEntity/ModScreenHandler 等从 block 解析 name */
    private val blockClassToName = mutableMapOf<kotlin.reflect.KClass<*>, String>()

    /** 存储 Block 实例（供扩展方法访问） */
    private val blockInstances = mutableMapOf<kotlin.reflect.KClass<*>, Block>()

    /** 存储 Item 实例（供扩展方法访问） */
    private val itemInstances = mutableMapOf<kotlin.reflect.KClass<*>, Item>()

    /** 存储 BlockEntityType（供扩展方法访问） */
    private val blockEntityTypes = mutableMapOf<kotlin.reflect.KClass<*>, BlockEntityType<*>>()

    /** 存储 ScreenHandlerType（供扩展方法访问） */
    private val screenHandlerTypes = mutableMapOf<kotlin.reflect.KClass<*>, ScreenHandlerType<*>>()

    /** 配方生成器列表 */
    private val recipeGenerators = mutableListOf<(Consumer<RecipeJsonProvider>) -> Unit>()

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
        blockInstances.clear()
        itemInstances.clear()
        recipeGenerators.clear()

        // 按顺序注册：方块 → 方块实体类型 → ScreenHandler → 物品 → 物品栏
        registerBlocks(modId, blockClasses)
        registerBlockEntities(modId, blockEntityClasses)
        registerScreenHandlers(modId, screenHandlerClasses)
        registerItems(modId, itemClasses)
        registerCreativeTabs(modId, tabClasses)
        collectRecipeGenerators(blockClasses, itemClasses)

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
                val entries = rawEntries
                    .sortedWith(compareBy({ it.group }, { it.itemId.toString() }))

                logger.info("物品栏 {} 包含 {} 个物品: {}", name, entries.size, entries.map { it.itemId })

                // 创建物品栏，使用 entries() 方法添加物品
                val itemGroup = FabricItemGroup.builder()
                    .icon { net.minecraft.item.ItemStack(Registries.ITEM.get(iconId)) }
                    .displayName(Text.translatable("itemGroup.$modId.$name"))
                    .entries { _, collector ->
                        // 添加所有属于这个物品栏的物品（已按 group 排序）
                        for (entry in entries) {
                            val item = Registries.ITEM.get(entry.itemId)
                            if (item !== net.minecraft.item.Items.AIR) {
                                // 检查物品是否实现了 IBatteryItem 或 IElectricTool 接口
                                // 如果实现了，添加空电和满电两个版本
                                val isBatteryItem = entry.itemClass != null &&
                                    entry.itemClass.isSubclassOf(IBatteryItem::class)
                                val isElectricTool = entry.itemClass != null &&
                                    entry.itemClass.isSubclassOf(IElectricTool::class)

                                if (isBatteryItem || isElectricTool) {
                                    // 添加空电版本
                                    val emptyStack = ItemStack(item)
                                    if (isBatteryItem) {
                                        val batteryItem = item as IBatteryItem
                                        batteryItem.setCurrentCharge(emptyStack, 0)
                                    } else if (isElectricTool) {
                                        val electricTool = item as IElectricTool
                                        electricTool.setEnergy(emptyStack, 0)
                                    }
                                    collector.add(emptyStack)

                                    // 添加满电版本
                                    val fullStack = ItemStack(item)
                                    if (isBatteryItem) {
                                        val batteryItem = item as IBatteryItem
                                        batteryItem.setCurrentCharge(fullStack, batteryItem.maxCapacity)
                                    } else if (isElectricTool) {
                                        val electricTool = item as IElectricTool
                                        electricTool.setEnergy(fullStack, electricTool.maxCapacity)
                                    }
                                    collector.add(fullStack)
                                } else {
                                    // 普通物品，直接添加
                                    collector.add(item)
                                }
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
                val companion = clazz.companionObjectInstance
                    ?: error("@ModScreenHandler 类 ${clazz.simpleName} 需有 companion 对象并提供 fromBuffer")
                val fromBufferFn = clazz.companionObject?.memberFunctions?.find { it.name == "fromBuffer" }
                    ?: error("@ModScreenHandler 类 ${clazz.simpleName} 的 companion 需提供 fromBuffer(syncId, playerInventory, buf)")

                // 收集所有需要注册的名称
                val namesToRegister = when {
                    annotation.names.isNotEmpty() -> annotation.names.toList()
                    annotation.name.isNotEmpty() -> listOf(annotation.name)
                    else -> {
                        // 使用类名转换
                        listOf(camelToSnake(clazz.simpleName ?: "unknown"))
                    }
                }

                // 为每个名称注册一个 ScreenHandlerType（多个方块共用同一个 UI）
                for (name in namesToRegister) {
                    val finalName = if (annotation.block != Any::class && blockClassToName.containsKey(annotation.block)) {
                        blockClassToName[annotation.block]!!
                    } else {
                        name
                    }

                    val id = Identifier(modId, finalName)

                    val type = ExtendedScreenHandlerType { syncId, playerInventory, buf ->
                        @Suppress("UNCHECKED_CAST")
                        fromBufferFn.call(companion, syncId, playerInventory, buf) as ScreenHandler
                    }

                    Registry.register(Registries.SCREEN_HANDLER, id, type)
                    screenHandlerTypes[clazz] = type
                    logger.debug("已注册 ScreenHandler 类型: {} (来自类 {})", id, clazz.simpleName)
                }
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
                blockEntityTypes[clazz] = type
                logger.debug("已注册方块实体类型: {}", id)

                // 若存在 @RegisterEnergy 字段，则向 Energy API 注册 SIDED 查找
                // 递归扫描类层次结构（包括父类）以查找 @RegisterEnergy 字段
                val energyProperty = findAllMemberProperties(clazz).firstOrNull { it.hasAnnotation<RegisterEnergy>() }
                if (energyProperty != null) {
                    val prop = energyProperty
                    @Suppress("UNCHECKED_CAST")
                    EnergyStorage.SIDED.registerForBlockEntity({ be, direction ->
                        val energyContainer = (prop as KProperty1<Any, Any?>).get(be)
                        if (energyContainer is TickLimitedSidedEnergyContainer) energyContainer.getSideStorage(direction)
                        else {
                            logger.error("方块实体 {} 的能量容器 {} 不是 TickLimitedSidedEnergyContainer 的子类", clazz.simpleName, prop.name)
                            null
                        }
                    }, type)
                    logger.debug("已为方块实体 {} 注册 EnergyStorage（属性 {}）", clazz.simpleName, prop.name)
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
                blockInstances[clazz] = instance
                if (annotation.transparent) {
                    TransparentBlockRegistry.add(id)
                }
                logger.debug("已注册方块: {}", id)

                // 注册方块物品
                if (annotation.registerItem) {
                    val blockItem = BlockItem(instance, FabricItemSettings())
                    Registry.register(Registries.ITEM, id, blockItem)
                    logger.debug("已注册方块物品: {}", id)

                    // 记录物品应该添加到哪个物品栏（带 group 以便排序，不包含类类型因为方块物品不是电池/电动工具）
                    if (annotation.tab != CreativeTab.MINECRAFT_MISC) {
                        tabItems.getOrPut(annotation.tab) { mutableListOf() }.add(TabEntry(id, annotation.group))
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
                itemInstances[clazz] = instance
                logger.debug("已注册物品: {}", id)

                // 记录物品应该添加到哪个物品栏（带 group 以便排序，包含类类型用于检查电池/电动工具）
                if (annotation.tab != CreativeTab.MINECRAFT_MISC) {
                    tabItems.getOrPut(annotation.tab) { mutableListOf() }.add(TabEntry(id, annotation.group, clazz))
                }
            } catch (e: Exception) {
                logger.error("注册物品 {} 失败: {}", clazz.simpleName, e.message, e)
            }
        }
    }

    private fun camelToSnake(str: String): String {
        return str.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }

    /**
     * 递归查找类及其所有父类的成员属性（Kotlin 专用）
     */
    private fun findAllMemberProperties(clazz: kotlin.reflect.KClass<*>): Sequence<kotlin.reflect.KProperty1<*, *>> {
        return sequence {
            var current: kotlin.reflect.KClass<*>? = clazz
            while (current != null && current != Any::class) {
                yieldAll(current.declaredMemberProperties)
                current = current.superclasses.firstOrNull()
            }
        }
    }

    private fun parseItemGroupKey(modId: String, tab: CreativeTab): RegistryKey<ItemGroup> {
        val id = Identifier(tab.getNamespacedId(modId))
        return RegistryKey.of(RegistryKeys.ITEM_GROUP, id)
    }

    /**
     * 物品栏条目，包含物品ID、分组信息和类类型
     */
    private data class TabEntry(
        val itemId: Identifier,
        val group: String,
        val itemClass: kotlin.reflect.KClass<*>? = null
    )

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

    /**
     * 收集 Block/Item 类 companion 中的配方生成器
     */
    private fun collectRecipeGenerators(
        blockClasses: List<BlockClassInfo>,
        itemClasses: List<ItemClassInfo>
    ) {
        val allClasses = (blockClasses.map { it.clazz } + itemClasses.map { it.clazz })

        for (clazz in allClasses) {
            try {
                val companion = clazz.companionObjectInstance ?: continue
                val generateRecipesMethod = clazz.companionObject?.memberFunctions?.find {
                    it.name == "generateRecipes"
                } ?: continue

                // 验证方法签名
                val parameters = generateRecipesMethod.parameters
                if (parameters.size != 2 ||
                    parameters[1].type.classifier != Consumer::class) {
                    logger.warn("类 {} 的 generateRecipes 方法签名不正确，应为 (Consumer<RecipeJsonProvider>) -> Unit", clazz.simpleName)
                    continue
                }

                // 创建生成器闭包
                val generator: (Consumer<RecipeJsonProvider>) -> Unit = { exporter ->
                    @Suppress("UNCHECKED_CAST")
                    generateRecipesMethod.call(companion, exporter as Any?)
                }

                recipeGenerators.add(generator)
                logger.debug("已收集配方生成器: {}", clazz.simpleName)
            } catch (e: Exception) {
                logger.debug("收集配方生成器失败 {}: {}", clazz.simpleName, e.message)
            }
        }

        logger.info("共收集 {} 个配方生成器", recipeGenerators.size)
    }

    /**
     * 执行所有配方生成
     * 供 ModRecipeProvider 调用
     */
    fun generateAllRecipes(recipeExporter: Consumer<RecipeJsonProvider>) {
        logger.info("开始生成配方...")
        recipeGenerators.forEach { it(recipeExporter) }
        logger.info("配方生成完成，共生成 {} 个配方", recipeGenerators.size)
    }

    /**
     * 获取 Block 实例
     * 供扩展方法调用
     */
    internal fun getBlockInstance(clazz: kotlin.reflect.KClass<out Block>): Block =
        blockInstances[clazz] ?: error("Block instance not found: ${clazz.simpleName}")

    /**
     * 获取 Item 实例
     * 供扩展方法调用
     */
    internal fun getItemInstance(clazz: kotlin.reflect.KClass<out Item>): Item =
        itemInstances[clazz] ?: error("Item instance not found: ${clazz.simpleName}")

    /**
     * 手动注册 BlockEntityType（供未通过注解自动注册的类使用）
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : BlockEntity> registerBlockEntityType(clazz: kotlin.reflect.KClass<T>, type: BlockEntityType<T>) {
        blockEntityTypes[clazz] = type
    }

    /**
     * 获取 BlockEntityType
     * 供扩展方法调用
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T : BlockEntity> getBlockEntityType(clazz: kotlin.reflect.KClass<T>): BlockEntityType<T> =
        blockEntityTypes[clazz] as? BlockEntityType<T>
            ?: error("BlockEntityType not found: ${clazz.simpleName}")

    /**
     * 获取 ScreenHandlerType
     * 供扩展方法调用
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T : ScreenHandler> getScreenHandlerType(clazz: kotlin.reflect.KClass<T>): ScreenHandlerType<T> =
        screenHandlerTypes[clazz] as? ScreenHandlerType<T>
            ?: error("ScreenHandlerType not found: ${clazz.simpleName}")

    private data class ItemClassInfo(
        val clazz: kotlin.reflect.KClass<*>,
        val annotation: ModItem
    )
}
