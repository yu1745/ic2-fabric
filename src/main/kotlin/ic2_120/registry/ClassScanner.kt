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
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.annotation.ScreenFactory
import ic2_120.registry.annotation.RegisterEnergy
import ic2_120.registry.annotation.RegisterFluidStorage
import ic2_120.registry.annotation.RegisterItemStorage
import ic2_120.registry.type
import ic2_120.registry.annotation.ModMachineRecipe
import ic2_120.registry.annotation.ModMachineRecipeBinding
import ic2_120.registry.MachineRecipeScanEntry
import ic2_120.content.recipes.MaterialTagRegistry
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.util.math.Direction
import team.reborn.energy.api.EnergyStorage
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

import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.network.PacketByteBuf
import io.netty.buffer.ByteBuf
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.util.math.BlockPos
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ArrayPropertyDelegate
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
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
import java.nio.file.Files
import java.nio.file.Paths
import net.minecraft.data.server.recipe.RecipeExporter

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
    private var currentModId: String = "ic2_120"

    /** 扫描机器配方 [ModMachineRecipe] 的包 */
    private val machineRecipeScanPackages = listOf("ic2_120.content.recipes")

    /** 扫描 [ic2_120.registry.annotation.ModMachineRecipeBinding]（BlockEntity → 序列化器） */
    private val machineRecipeBindingScanPackages = listOf("ic2_120.content.block.machines")

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

    /** Block -> BlockEntityType 映射（由 @ModBlockEntity 注解建立，供 tooltip 等获取 tier） */
    private val blockToBlockEntityType = mutableMapOf<net.minecraft.block.Block, BlockEntityType<*>>()

    /** 存储 ScreenHandlerType（供扩展方法访问） */
    private val screenHandlerTypes = mutableMapOf<kotlin.reflect.KClass<*>, ScreenHandlerType<*>>()

    /** 配方生成器列表（带 modId 标记，支持附属 mod 按需过滤） */
    private data class RecipeGeneratorEntry(val modId: String, val generator: (RecipeExporter) -> Unit)
    private val recipeGenerators = mutableListOf<RecipeGeneratorEntry>()

    /**
     * `@ModBlock(generateBlockLootTable = false)` 的方块注册名（path），供 [ic2_120.content.recipes.ModBlockLootTableProvider] 跳过自动生成。
     */
    private val blockPathsSkippingGeneratedLootTable = mutableSetOf<String>()

    /** 已处理类名集合，避免同一类在多资源入口下被重复处理 */
    private val processedClassNames = mutableSetOf<String>()

    fun shouldSkipGeneratedBlockLootTable(path: String): Boolean =
        path in blockPathsSkippingGeneratedLootTable

    /**
     * 扫描并注册所有带注解的类。
     *
     * @param modId 模组命名空间
     * @param packageNames 要扫描的包名列表
     */
    fun scanAndRegister(modId: String, packageNames: List<String>) {
        logger.info("开始扫描包进行自动注册: {}", packageNames)
        currentModId = modId

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
        forEachClassInPackage(packageName) { className ->
            processClass(className, tabClasses, blockClasses, blockEntityClasses, screenHandlerClasses, itemClasses)
        }
    }

    /**
     * 遍历包内所有 .class 对应的 JVM 类名（支持 file / jar / union，与 [scanPackage] 一致）。
     */
    private fun forEachClassInPackage(packageName: String, onClass: (String) -> Unit) {
        try {
            logger.info("扫描包: {}", packageName)

            val classLoader = Thread.currentThread().contextClassLoader
            val packagePath = packageName.replace('.', '/')
            val resources = classLoader.getResources(packagePath)
            var foundSupportedResource = false

            while (resources.hasMoreElements()) {
                val url = resources.nextElement()

                when (url.protocol) {
                    "file" -> {
                        foundSupportedResource = true
                        scanDirectoryForClassFiles(url.path, packageName, onClass)
                    }
                    "jar" -> {
                        foundSupportedResource = true
                        scanJarFileForClassFiles(url, packageName, onClass)
                    }
                    "union" -> {
                        foundSupportedResource = true
                        // 信雅互联（Sinytra Connector）兼容代码：
                        // 直接扫描 union 虚拟文件系统，不走 codeSource fallback。
                        scanUnionResourceForClassFiles(url, packageName, onClass)
                    }
                    else -> {
                        logger.warn("未支持的协议: {}", url.protocol)
                    }
                }
            }

            if (!foundSupportedResource) {
                logger.warn("包 {} 未找到可扫描资源（支持协议: file/jar/union）", packageName)
            }
        } catch (e: Exception) {
            logger.error("扫描包 {} 失败: {}", packageName, e.message, e)
        }
    }

    /**
     * 信雅互联（Sinytra Connector）兼容代码：
     * 直接扫描 union 协议对应的虚拟文件系统资源。
     */
    private fun scanUnionResourceForClassFiles(
        url: java.net.URL,
        packageName: String,
        onClass: (String) -> Unit
    ) {
        try {
            val rootPath = Paths.get(url.toURI())
            if (!Files.exists(rootPath)) {
                logger.warn("union 资源不存在: {}", url)
                return
            }
            Files.walk(rootPath).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".class") }
                    .forEach { classFile ->
                        val relative = rootPath.relativize(classFile).toString().replace('\\', '/')
                        val relativeClassName = relative.removeSuffix(".class").replace('/', '.')
                        val className = "$packageName.$relativeClassName"
                        onClass(className)
                    }
            }
        } catch (e: Exception) {
            logger.warn("扫描 union 资源失败 {}: {}", url, e.message)
        }
    }

    private fun scanDirectoryForClassFiles(
        path: String,
        packageName: String,
        onClass: (String) -> Unit
    ) {
        val dir = java.io.File(path)
        if (!dir.exists() || !dir.isDirectory) return

        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> {
                    scanDirectoryForClassFiles(file.path, packageName + "." + file.name, onClass)
                }
                file.name.endsWith(".class") -> {
                    val className = packageName + "." + file.name.removeSuffix(".class")
                    onClass(className)
                }
            }
        }
    }

    private fun scanJarFileForClassFiles(
        url: java.net.URL,
        packageName: String,
        onClass: (String) -> Unit
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
        scanJarPathForClassFiles(decodedPath, packageName, onClass)
    }

    private fun scanJarPathForClassFiles(
        jarPath: String,
        packageName: String,
        onClass: (String) -> Unit
    ) {
        val jarFile = java.util.jar.JarFile(jarPath)
        val entries = jarFile.entries()
        val packagePath = packageName.replace('.', '/') + "/"

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name

            if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                val className = entryName
                    .removeSuffix(".class")
                    .replace('/', '.')
                onClass(className)
            }
        }

        jarFile.close()
    }

    /**
     * 收集 [ModMachineRecipe] 声明的注册条目，
     * 供 [ic2_120.content.recipes.ModMachineRecipes.register] 创建并注册 [net.minecraft.recipe.RecipeType] / [RecipeSerializer]。
     */
    fun collectMachineRecipeRegistrations(): List<MachineRecipeScanEntry> {
        val list = mutableListOf<MachineRecipeScanEntry>()
        val seenSerializers = mutableSetOf<KClass<*>>()
        for (pkg in machineRecipeScanPackages) {
            forEachClassInPackage(pkg) { className ->
                collectMachineRecipeRegistrationsFromClass(className, list, seenSerializers)
            }
        }
        logger.info("机器配方注解扫描: {} 个序列化器条目", list.size)
        return list
    }

    private fun collectMachineRecipeRegistrationsFromClass(
        className: String,
        into: MutableList<MachineRecipeScanEntry>,
        seenSerializers: MutableSet<KClass<*>>
    ) {
        try {
            val loader = Thread.currentThread().contextClassLoader ?: ClassScanner::class.java.classLoader
            /*
             * 第二参数 false = 不执行静态初始化（<clinit>）。
             *
             * runDatagen 曾在此崩溃的典型链路（已配合 [Ic2_120] 中「先 ClassScanner.scanAndRegister、再 ModMachineRecipes.register」修复）：
             * 1) 本方法会枚举 `ic2_120.content.recipes` 下几乎所有 .class（含 *RecipeDatagen、辅助类等），不仅有序列化器。
             * 2) 若使用 Class.forName(name) 默认行为（initialize=true），每加载一个类都会跑 <clinit>。
             * 3) 例如 BlockCutterRecipeDatagen 的静态初始化里会引用物品（如 IronPlate.instance）；而当时若在
             *    ClassScanner 完成方块/物品注册之前就扫描配方包，注册表里还没有该物品 → IllegalStateException，游戏在 Initializing 阶段直接崩。
             * 4) 传 false 时：可先读 @ModMachineRecipe；无该注解则立即 return，该类永不初始化，避免误触 Datagen 的 <clinit>。
             *    仅有注解的序列化器才会继续走到 objectInstance，此时初始化发生在物品已注册之后（见模组入口顺序）。
             */
            val jClass = Class.forName(className, false, loader)
            val kotlinClazz = jClass.kotlin
            val single = kotlinClazz.findAnnotation<ModMachineRecipe>() ?: return
            if (!kotlinClazz.isSubclassOf(RecipeSerializer::class)) {
                logger.warn("@ModMachineRecipe 仅适用于 RecipeSerializer: {}", className)
                return
            }
            if (kotlinClazz.objectInstance == null) {
                logger.warn("@ModMachineRecipe 要求 Kotlin object 序列化器: {}", className)
                return
            }
            if (!seenSerializers.add(kotlinClazz)) {
                return
            }
            @Suppress("UNCHECKED_CAST")
            val serClass = kotlinClazz as KClass<out RecipeSerializer<*>>
            into.add(MachineRecipeScanEntry(single.id, single.recipeClass, serClass))
        } catch (e: ClassNotFoundException) {
            logger.warn("无法加载类: {}", className)
        } catch (e: NoClassDefFoundError) {
            logger.warn("类依赖缺失，跳过机器配方注解 {}: {}", className, e.message)
        } catch (e: Exception) {
            logger.debug("处理机器配方注解 {} 时出错: {}", className, e.message)
        }
    }

    /**
     * 收集 [ModMachineRecipeBinding]，建立 BlockEntity 类 → 序列化器类（用于 [ic2_120.content.recipes.ModMachineRecipes.getRecipeType]）。
     */
    fun collectMachineRecipeBindings(): List<Pair<KClass<*>, KClass<out RecipeSerializer<*>>>> {
        val out = mutableListOf<Pair<KClass<*>, KClass<out RecipeSerializer<*>>>>()
        for (pkg in machineRecipeBindingScanPackages) {
            forEachClassInPackage(pkg) { className ->
                collectMachineRecipeBindingFromClass(className, out)
            }
        }
        logger.info("机器配方 BlockEntity 绑定: {} 条", out.size)
        return out
    }

    private fun collectMachineRecipeBindingFromClass(
        className: String,
        into: MutableList<Pair<KClass<*>, KClass<out RecipeSerializer<*>>>>
    ) {
        try {
            val clazz = Class.forName(className).kotlin
            val ann = clazz.findAnnotation<ModMachineRecipeBinding>() ?: return
            if (!clazz.isSubclassOf(BlockEntity::class)) {
                logger.warn("@ModMachineRecipeBinding 仅适用于 BlockEntity 子类: {}", className)
                return
            }
            into.add(clazz to ann.serializerClass)
        } catch (e: ClassNotFoundException) {
            logger.warn("无法加载类: {}", className)
        } catch (e: NoClassDefFoundError) {
            logger.warn("类依赖缺失，跳过机器配方绑定 {}: {}", className, e.message)
        } catch (e: Exception) {
            logger.debug("处理机器配方绑定 {} 时出错: {}", className, e.message)
        }
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
            if (!processedClassNames.add(className)) {
                return
            }
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
            logger.warn("类依赖缺失，跳过 {}: {}", className, e.message)
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
                val id = Identifier.of(modId, name)

                // 创建 RegistryKey
                val tabKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, id)

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

                // 解析图标：根据 iconResource（优先）或 iconItem 创建图标
                val iconStackSupplier: () -> ItemStack
                when {
                    annotation.iconResource.isNotEmpty() -> {
                        // 直接使用资源文件路径创建图标
                        val resourcePath = annotation.iconResource
                        iconStackSupplier = { CreativeTabIconProvider.getIconStack(resourcePath) }
                    }
                    annotation.iconItem.isNotEmpty() -> {
                        // 使用已注册的物品作为图标
                        val iconId = Identifier.of(modId, annotation.iconItem)
                        iconStackSupplier = { ItemStack(Registries.ITEM.get(iconId)) }
                    }
                    else -> {
                        // 默认使用第一个物品
                        val firstEntry = entries.firstOrNull()
                        iconStackSupplier = if (firstEntry != null) {
                            { ItemStack(Registries.ITEM.get(firstEntry.itemId)) }
                        } else {
                            { ItemStack(net.minecraft.item.Items.BARRIER) }
                        }
                    }
                }

                // 创建物品栏，使用 entries() 方法添加物品
                val itemGroup = FabricItemGroup.builder()
                    .icon(iconStackSupplier)
                    .displayName(Text.translatable("itemGroup.$modId.$name"))
                    .entries { _, collector ->
                        val addedStacks = mutableListOf<ItemStack>()
                        fun addUnique(stack: ItemStack) {
                            if (addedStacks.any { ItemStack.areEqual(it, stack) }) {
                                return
                            }
                            collector.add(stack)
                            addedStacks.add(stack.copy())
                        }

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
                                    addUnique(emptyStack)

                                    // 添加满电版本
                                    val fullStack = ItemStack(item)
                                    if (isBatteryItem) {
                                        val batteryItem = item as IBatteryItem
                                        batteryItem.setCurrentCharge(fullStack, batteryItem.maxCapacity)
                                    } else if (isElectricTool) {
                                        val electricTool = item as IElectricTool
                                        electricTool.setEnergy(fullStack, electricTool.maxCapacity)
                                    }
                                    addUnique(fullStack)
                                } else {
                                    // 普通物品，直接添加
                                    addUnique(ItemStack(item))
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
                val fromBufferFn = clazz.companionObject?.memberFunctions?.find {
                    it.findAnnotation<ScreenFactory>() != null
                } ?: clazz.companionObject?.memberFunctions?.find { it.name == "fromBuffer" }

                val createFromBuffer: (Int, PlayerInventory, PacketByteBuf) -> ScreenHandler =
                    if (fromBufferFn != null) {
                        val instance = companion
                            ?: error("@ModScreenHandler 类 ${clazz.simpleName} 的 companion 不存在，无法调用 ${fromBufferFn.name}")
                        validateFromBufferSignature(clazz, fromBufferFn)
                        val factory: (Int, PlayerInventory, PacketByteBuf) -> ScreenHandler =
                            { syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf ->
                            @Suppress("UNCHECKED_CAST")
                            fromBufferFn.call(instance, syncId, playerInventory, buf) as ScreenHandler
                        }
                        factory
                    } else {
                        createAutoScreenHandlerFactory(clazz, annotation)
                    }

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

                    val id = Identifier.of(modId, finalName)

                    val type = ExtendedScreenHandlerType<ScreenHandler, PacketByteBuf>(
                        { syncId, playerInventory, buf -> createFromBuffer(syncId, playerInventory, buf) },
                        object : PacketCodec<RegistryByteBuf, PacketByteBuf> {
                            override fun encode(buf: RegistryByteBuf, value: PacketByteBuf) {
                                buf.writeBytes(value, value.readableBytes())
                            }
                            override fun decode(buf: RegistryByteBuf): PacketByteBuf {
                                return PacketByteBuf(buf.readBytes(buf.readableBytes()) as ByteBuf)
                            }
                        }
                    )

                    Registry.register(Registries.SCREEN_HANDLER, id, type)
                    screenHandlerTypes[clazz] = type
                    logger.debug("已注册 ScreenHandler 类型: {} (来自类 {})", id, clazz.simpleName)
                }
            } catch (e: Exception) {
                logger.error("注册 ScreenHandler {} 失败: {}", clazz.simpleName, e.message, e)
            }
        }
    }

    private fun validateFromBufferSignature(
        clazz: KClass<*>,
        fromBufferFn: kotlin.reflect.KFunction<*>
    ) {
        val parameters = fromBufferFn.parameters
        if (parameters.size != 4 ||
            parameters[1].type.classifier != Int::class ||
            parameters[2].type.classifier != PlayerInventory::class ||
            parameters[3].type.classifier != PacketByteBuf::class
        ) {
            error(
                "@ModScreenHandler 类 ${clazz.simpleName} 的 ${fromBufferFn.name} 签名不正确，" +
                    "应为 (syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf)"
            )
        }
    }

    private fun createAutoScreenHandlerFactory(
        clazz: KClass<*>,
        annotation: ModScreenHandler
    ): (Int, PlayerInventory, PacketByteBuf) -> ScreenHandler {
        val inventorySize = annotation.clientInventorySize
        if (inventorySize <= 0) {
            error(
                "@ModScreenHandler 类 ${clazz.simpleName} 未提供 fromBuffer，" +
                    "请在 companion 增加 @ScreenFactory/fromBuffer，或在注解中配置 clientInventorySize 并使用标准构造签名"
            )
        }

        val ctor = clazz.constructors.find { constructor ->
            val parameters = constructor.parameters
            parameters.size == 5 &&
                parameters[0].type.classifier == Int::class &&
                parameters[1].type.classifier == PlayerInventory::class &&
                parameters[2].type.classifier == Inventory::class &&
                parameters[3].type.classifier == ScreenHandlerContext::class &&
                parameters[4].type.classifier == PropertyDelegate::class
        } ?: error(
            "@ModScreenHandler 类 ${clazz.simpleName} 未提供 fromBuffer 且构造函数不匹配，" +
                "需要构造签名 (syncId, playerInventory, blockInventory, context, propertyDelegate)"
        )

        return { syncId, playerInventory, buf ->
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInventory = SimpleInventory(inventorySize)
            val propertyDelegate = ArrayPropertyDelegate(propertyCount)
            @Suppress("UNCHECKED_CAST")
            ctor.call(syncId, playerInventory, blockInventory, context, propertyDelegate) as ScreenHandler
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
                val id = Identifier.of(modId, name)
                val targetBlocks = resolveBlockEntityTargets(modId, clazz, annotation, id)
                if (targetBlocks.isEmpty()) {
                    logger.error("注册方块实体类型 {} 失败: 未解析到任何目标方块", clazz.simpleName)
                    continue
                }
                val ctor = clazz.constructors.find { it.parameters.size == 2 }
                    ?: error("@ModBlockEntity 类 ${clazz.simpleName} 需提供 (BlockPos, BlockState) 构造函数")
                val factory = FabricBlockEntityTypeBuilder.Factory { pos: BlockPos, state: BlockState ->
                    @Suppress("UNCHECKED_CAST")
                    ctor.call(pos, state) as BlockEntity
                }
                @Suppress("UNCHECKED_CAST")
                val type = FabricBlockEntityTypeBuilder
                    .create(factory, *targetBlocks.toTypedArray())
                    .build() as BlockEntityType<BlockEntity>
                Registry.register(Registries.BLOCK_ENTITY_TYPE, id, type)
                blockEntityTypes[clazz] = type
                for (block in targetBlocks) {
                    if (block != net.minecraft.block.Blocks.AIR) {
                        blockToBlockEntityType[block] = type
                    }
                }
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

                // 若存在 @RegisterItemStorage 字段，则向 Fabric Transfer API 注册 SIDED 查找
                val itemProperty = findAllMemberProperties(clazz).firstOrNull { it.hasAnnotation<RegisterItemStorage>() }
                if (itemProperty != null) {
                    val iProp = itemProperty
                    @Suppress("UNCHECKED_CAST")
                    ItemStorage.SIDED.registerForBlockEntity({ be, _ ->
                        val storage = (iProp as KProperty1<Any, Any?>).get(be)
                        @Suppress("UNCHECKED_CAST")
                        if (storage is Storage<*>) storage as Storage<ItemVariant>
                        else {
                            logger.error("方块实体 {} 的物品存储 {} 不是 Storage<ItemVariant>", clazz.simpleName, iProp.name)
                            null
                        }
                    }, type)
                    logger.debug("已为方块实体 {} 注册 ItemStorage（属性 {}）", clazz.simpleName, iProp.name)
                }

                registerFluidStorageLookup(clazz)
            } catch (e: Exception) {
                logger.error("注册方块实体类型 {} 失败: {}", clazz.simpleName, e.message, e)
            }
        }
    }

    private fun registerFluidStorageLookup(clazz: KClass<*>) {
        try {
            val companion = clazz.companionObjectInstance ?: return
            val registerFns = clazz.companionObject?.memberFunctions
                ?.filter { it.findAnnotation<RegisterFluidStorage>() != null }
                .orEmpty()
            for (registerFn in registerFns) {
                if (registerFn.parameters.size != 1) {
                    logger.error(
                        "@RegisterFluidStorage 方法 {}.{} 签名不正确，应为无参 companion 方法",
                        clazz.simpleName,
                        registerFn.name
                    )
                    continue
                }
                registerFn.call(companion)
                logger.debug("已为方块实体 {} 自动注册 FluidStorage lookup: {}", clazz.simpleName, registerFn.name)
            }
        } catch (e: Exception) {
            logger.error("自动注册 FluidStorage lookup 失败 {}: {}", clazz.simpleName, e.message, e)
        }
    }

    private fun resolveBlockEntityTargets(
        modId: String,
        clazz: kotlin.reflect.KClass<*>,
        annotation: ModBlockEntity,
        id: Identifier
    ): List<Block> {
        if (annotation.blocks.isNotEmpty()) {
            val resolved = annotation.blocks.mapNotNull { blockClass ->
                if (!blockClass.isSubclassOf(Block::class)) {
                    logger.warn("@ModBlockEntity 类 {} 的 blocks 包含非 Block 子类: {}", clazz.simpleName, blockClass.simpleName)
                    return@mapNotNull null
                }
                val blockName = blockClassToName[blockClass]
                if (blockName == null) {
                    logger.warn(
                        "@ModBlockEntity 类 {} 的目标方块 {} 未注册，可能未标注 @ModBlock 或扫描包缺失",
                        clazz.simpleName,
                        blockClass.simpleName
                    )
                    return@mapNotNull null
                }
                val blockId = Identifier.of(modId, blockName)
                val block = Registries.BLOCK.get(blockId)
                if (block == net.minecraft.block.Blocks.AIR && blockId.path != "air") {
                    logger.warn("方块实体 {} 对应的方块未找到: {}，请确保方块先于方块实体注册", clazz.simpleName, blockId)
                    return@mapNotNull null
                }
                block
            }
            if (resolved.isEmpty()) {
                logger.error("@ModBlockEntity 类 {} 未解析到有效 blocks，注解 id: {}", clazz.simpleName, id)
            }
            return resolved
        }

        val block = Registries.BLOCK.get(id)
        if (block == net.minecraft.block.Blocks.AIR && id.path != "air") {
            logger.warn("方块实体 {} 对应的方块未找到: {}，请确保方块先于方块实体注册", clazz.simpleName, id)
        }
        return listOf(block)
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
                val id = Identifier.of(modId, name)

                // 注册方块
                Registry.register(Registries.BLOCK, id, instance)
                blockClassToName[clazz] = name
                blockInstances[clazz] = instance
                if (!annotation.generateBlockLootTable) {
                    blockPathsSkippingGeneratedLootTable.add(name)
                }
                if (annotation.materialTags.isNotEmpty()) {
                    MaterialTagRegistry.blockEntries.add(clazz to annotation.materialTags.toList())
                }
                BlockRenderLayerRegistry.put(id, annotation.renderLayer)
                logger.debug("已注册方块: {}", id)

                // 注册方块物品
                if (annotation.registerItem) {
                    val blockItem = createBlockItemForClass(clazz, instance, id)
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

    /**
     * 优先使用方块类内部定义的自定义 BlockItem（如 XxxBlockItem），
     * 若未找到则回退为默认 BlockItem。
     */
    private fun createBlockItemForClass(
        blockClass: KClass<*>,
        block: Block,
        id: Identifier
    ): BlockItem {
        val customItemClass = blockClass.nestedClasses.firstOrNull { nested ->
            nested.simpleName?.endsWith("BlockItem") == true && nested.isSubclassOf(BlockItem::class)
        } ?: return BlockItem(block, Item.Settings())

        val ctor = customItemClass.constructors.firstOrNull { constructor ->
            val params = constructor.parameters
            params.size == 2 &&
                params[0].type.classifier == Block::class &&
                params[1].type.classifier == Item.Settings::class
        } ?: run {
            logger.warn("方块 {} 存在自定义 BlockItem 类 {}，但未找到 (Block, Item.Settings) 构造函数，回退默认 BlockItem", id, customItemClass.simpleName)
            return BlockItem(block, Item.Settings())
        }

        return try {
            @Suppress("UNCHECKED_CAST")
            ctor.call(block, Item.Settings()) as BlockItem
        } catch (e: Exception) {
            logger.warn("创建方块 {} 的自定义 BlockItem 失败: {}，回退默认 BlockItem", id, e.message)
            BlockItem(block, Item.Settings())
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
                val id = Identifier.of(modId, name)

                // 注册物品
                Registry.register(Registries.ITEM, id, instance)
                itemInstances[clazz] = instance
                if (annotation.materialTags.isNotEmpty()) {
                    MaterialTagRegistry.itemEntries.add(clazz to annotation.materialTags.toList())
                }
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
        val id = Identifier.of(tab.getNamespacedId(modId))
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
                    it.findAnnotation<RecipeProvider>() != null
                } ?: clazz.companionObject?.memberFunctions?.find { it.name == "generateRecipes" }
                    ?: continue

                // 验证方法签名
                val parameters = generateRecipesMethod.parameters
                if (parameters.size != 2 ||
                    parameters[1].type.classifier != RecipeExporter::class) {
                    logger.warn(
                        "类 {} 的 {} 方法签名不正确，应为 (RecipeExporter) -> Unit",
                        clazz.simpleName,
                        generateRecipesMethod.name
                    )
                    continue
                }

                // 创建生成器闭包
                val generator: (RecipeExporter) -> Unit = { exporter ->
                    @Suppress("UNCHECKED_CAST")
                    generateRecipesMethod.call(companion, exporter as Any?)
                }

                recipeGenerators.add(RecipeGeneratorEntry(currentModId, generator))
                logger.debug("已收集配方生成器: {}.{}", clazz.simpleName, generateRecipesMethod.name)
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
    fun generateAllRecipes(recipeExporter: RecipeExporter) {
        logger.info("开始生成配方...")
        var successCount = 0
        var failCount = 0

        recipeGenerators.forEach { entry ->
            try {
                entry.generator(recipeExporter)
                successCount++
            } catch (e: Exception) {
                logger.error("配方生成失败: {}", e.message, e)
                failCount++
            }
        }
        logger.info("配方生成完成 - 成功: {}, 失败: {}, 总计: {}", successCount, failCount, recipeGenerators.size)
    }

    /**
     * 执行指定 modId 的配方生成（供附属 mod 使用）。
     */
    fun generateRecipesForMod(modId: String, recipeExporter: RecipeExporter) {
        logger.info("开始生成 {} 配方...", modId)
        var successCount = 0
        var failCount = 0
        val filtered = recipeGenerators.filter { it.modId == modId }

        filtered.forEach { entry ->
            try {
                entry.generator(recipeExporter)
                successCount++
            } catch (e: Exception) {
                logger.error("配方生成失败: {}", e.message, e)
                failCount++
            }
        }
        logger.info("{} 配方生成完成 - 成功: {}, 失败: {}, 总计: {}", modId, successCount, failCount, filtered.size)
    }

    /**
     * 获取 Block 实例
     * 供扩展方法调用
     */
    @JvmStatic fun getBlockInstancePublic(clazz: kotlin.reflect.KClass<out Block>): Block? =
        blockInstances[clazz]

    /**
     * 获取 Block 实例
     * 供扩展方法调用
     */
    internal fun getBlockInstance(clazz: kotlin.reflect.KClass<out Block>): Block =
        blockInstances[clazz] ?: error("Block instance not found: ${clazz.simpleName}")

    /**
     * 获取 Item 实例（可空，供附属 mod 查询）
     */
    @JvmStatic fun getItemInstancePublic(clazz: kotlin.reflect.KClass<out Item>): Item? =
        itemInstances[clazz]

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
        (blockEntityTypes[clazz] as? BlockEntityType<T>)
            ?: resolveBlockEntityTypeFromRegistry(clazz)
            ?: error("BlockEntityType not found: ${clazz.simpleName}")

    @Suppress("UNCHECKED_CAST")
    private fun <T : BlockEntity> resolveBlockEntityTypeFromRegistry(clazz: kotlin.reflect.KClass<T>): BlockEntityType<T>? {
        val annotation = clazz.findAnnotation<ModBlockEntity>() ?: return null
        val name = resolveNameFromBlockOrAnnotation(annotation.name, annotation.block, clazz.simpleName ?: "unknown")
        val id = Identifier.of(currentModId, name)
        val type = Registries.BLOCK_ENTITY_TYPE.getOrEmpty(id).orElse(null) ?: return null
        blockEntityTypes[clazz] = type
        return type as? BlockEntityType<T>
    }

    /**
     * 根据 @ModBlockEntity(block = XBlock::class) 建立的映射，获取方块对应的 BlockEntityType。
     * 用于 tooltip 等场景从 BlockEntity 获取 tier。
     */
    fun getBlockEntityTypeForBlock(block: net.minecraft.block.Block): BlockEntityType<*>? =
        blockToBlockEntityType[block]

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
