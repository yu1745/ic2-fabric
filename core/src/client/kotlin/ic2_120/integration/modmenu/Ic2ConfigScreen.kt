package ic2_120.integration.modmenu

import ic2_120.config.*
import ic2_120.Ic2_120
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

object Ic2ConfigScreen {

    private val LOGGER = LoggerFactory.getLogger("${Ic2_120.MOD_ID}/modmenu")

    fun createScreen(parent: Screen?): Screen {
        LOGGER.info("Creating IC2 config screen")
        try {
            return buildScreen(parent)
        } catch (e: Exception) {
            LOGGER.error("Failed to build config screen", e)
            throw e
        }
    }

    private fun buildScreen(parent: Screen?): Screen {
        val config = Ic2Config.current
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("IC2 配置"))
        val eb = builder.entryBuilder()

        val generalReader = generalCategory(builder, eb, config.general)
        val recyclerReader = recyclerCategory(builder, eb, config.recycler)
        val nuclearReader = nuclearCategory(builder, eb, config.nuclear)
        val minerReader = minerCategory(builder, eb, config.miner)
        val laserReader = miningLaserCategory(builder, eb, config.miningLaser)
        val armorReader = armorCategory(builder, eb, config.armor)
        val worldgenReader = worldgenCategory(builder, eb, config.worldgen)

        // UU 复制：Map 结构不适合 GUI，引导至命令
        builder.getOrCreateCategory(Text.literal("UU 复制"))
            .addEntry(eb.startTextDescription(
                Text.literal("UU 复制白名单请使用命令 /uureplication 管理")
            ).build())

        builder.setSavingRunnable {
            Ic2Config.save(Ic2MainConfig(
                general = generalReader(),
                recycler = recyclerReader(),
                nuclear = nuclearReader(),
                uuReplication = config.uuReplication,
                miner = minerReader(),
                miningLaser = laserReader(),
                armor = armorReader(),
                worldgen = worldgenReader()
            ))
        }

        return builder.build()
    }

    // ==================== 通用 ====================

    private fun generalCategory(
        builder: ConfigBuilder, eb: ConfigEntryBuilder, cfg: GeneralConfig
    ): () -> GeneralConfig {
        var logConfigOnLoad = cfg.logConfigOnLoad
        var checkForUpdates = cfg.checkForUpdates
        var shockWhenNoEnergyFlow = cfg.shockWhenNoEnergyFlow
        var explodeWhenNoEnergyFlow = cfg.explodeWhenNoEnergyFlow
        var enableOvervoltageExplosion = cfg.enableOvervoltageExplosion

        val cat = builder.getOrCreateCategory(Text.literal("通用"))
        cat.addEntry(eb.startBooleanToggle(Text.literal("启用超压爆炸"), enableOvervoltageExplosion)
            .setDefaultValue(true)
            .setTooltip(Text.literal("是否启用超压爆炸。设为 false 则完全关闭超压机器爆炸。"))
            .setSaveConsumer { enableOvervoltageExplosion = it }.build())
        cat.addEntry(eb.startBooleanToggle(Text.literal("启动时打印配置到日志"), logConfigOnLoad)
            .setDefaultValue(true)
            .setTooltip(Text.literal("启动/重载时是否把当前配置打印到日志"))
            .setSaveConsumer { logConfigOnLoad = it }.build())
        cat.addEntry(eb.startBooleanToggle(Text.literal("启用更新检查"), checkForUpdates)
            .setDefaultValue(true)
            .setSaveConsumer { checkForUpdates = it }.build())
        cat.addEntry(eb.startBooleanToggle(Text.literal("无能量时电人"), shockWhenNoEnergyFlow)
            .setDefaultValue(false)
            .setTooltip(Text.literal("电网不发生能量流动时是否仍会电人。设为 false 则无能量时不漏电。"))
            .setSaveConsumer { shockWhenNoEnergyFlow = it }.build())
        cat.addEntry(eb.startBooleanToggle(Text.literal("无能量时超压爆炸"), explodeWhenNoEnergyFlow)
            .setDefaultValue(false)
            .setTooltip(Text.literal("电网不发生能量流动时是否仍会超压爆炸。设为 false 则无能量时不爆炸。"))
            .setSaveConsumer { explodeWhenNoEnergyFlow = it }.build())

        return {
            GeneralConfig(
                logConfigOnLoad = logConfigOnLoad,
                checkForUpdates = checkForUpdates,
                shockWhenNoEnergyFlow = shockWhenNoEnergyFlow,
                explodeWhenNoEnergyFlow = explodeWhenNoEnergyFlow,
                enableOvervoltageExplosion = enableOvervoltageExplosion
            )
        }
    }

    // ==================== 回收机 ====================

    private fun recyclerCategory(
        builder: ConfigBuilder, eb: ConfigEntryBuilder, cfg: RecyclerConfig
    ): () -> RecyclerConfig {
        var blacklist = cfg.blacklist.toMutableList()

        val cat = builder.getOrCreateCategory(Text.literal("回收机"))
        cat.addEntry(eb.startStrList(Text.literal("回收机黑名单"), blacklist)
            .setDefaultValue(listOf("minecraft:stick"))
            .setTooltip(Text.literal("填写物品 id 列表，例如 minecraft:stick"))
            .setSaveConsumer { blacklist = it.toMutableList() }.build())

        return { RecyclerConfig(blacklist = blacklist.toList()) }
    }

    // ==================== 核能 ====================

    private fun nuclearCategory(
        builder: ConfigBuilder, eb: ConfigEntryBuilder, cfg: NuclearConfig
    ): () -> NuclearConfig {
        var enableReactorExplosion = cfg.enableReactorExplosion
        var reactorExplosionPowerLimit = cfg.reactorExplosionPowerLimit
        var explosionBlocksPerTick = cfg.explosionBlocksPerTick

        val cat = builder.getOrCreateCategory(Text.literal("核能"))
        cat.addEntry(eb.startBooleanToggle(Text.literal("反应堆过热爆炸"), enableReactorExplosion)
            .setDefaultValue(true)
            .setTooltip(Text.literal("是否允许核反应堆在过热时爆炸"))
            .setSaveConsumer { enableReactorExplosion = it }.build())
        cat.addEntry(eb.startFloatField(Text.literal("爆炸威力上限"), reactorExplosionPowerLimit)
            .setDefaultValue(0f)
            .setTooltip(Text.literal("反应堆爆炸威力上限。0 = 不限制（实际最大 100）。"))
            .setSaveConsumer { reactorExplosionPowerLimit = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("每 tick 摧毁方块数"), explosionBlocksPerTick)
            .setDefaultValue(2000)
            .setTooltip(Text.literal("核爆炸每 tick 摧毁方块数。越大炸得越快，但可能掉 tps。"))
            .setSaveConsumer { explosionBlocksPerTick = it }.build())

        return {
            NuclearConfig(
                enableReactorExplosion = enableReactorExplosion,
                reactorExplosionPowerLimit = reactorExplosionPowerLimit,
                explosionBlocksPerTick = explosionBlocksPerTick
            )
        }
    }

    // ==================== 采矿机 ====================

    private fun minerCategory(
        builder: ConfigBuilder, eb: ConfigEntryBuilder, cfg: MinerConfig
    ): () -> MinerConfig {
        var additionalBlocks = cfg.additionalMineableBlocks.toMutableList()

        val cat = builder.getOrCreateCategory(Text.literal("采矿机"))
        cat.addEntry(eb.startStrList(Text.literal("额外可挖方块"), additionalBlocks)
            .setDefaultValue(emptyList())
            .setTooltip(Text.literal("采矿机额外可挖方块 id 列表。默认矿石通过名称自动匹配（含 ore），此列表用于添加特殊方块或者别的 mod 的方块。"))
            .setSaveConsumer { additionalBlocks = it.toMutableList() }.build())

        return { MinerConfig(additionalMineableBlocks = additionalBlocks.toList()) }
    }

    // ==================== 采矿镭射枪 ====================

    private fun miningLaserCategory(
        builder: ConfigBuilder, eb: ConfigEntryBuilder, cfg: MiningLaserConfig
    ): () -> MiningLaserConfig {
        var maxEnergy = cfg.maxEnergy
        var visualLength = cfg.visualLength
        var renderDistance = cfg.renderDistance
        var maxLife = cfg.maxLife
        var bulletRadius = cfg.bulletRadius

        val cat = builder.getOrCreateCategory(Text.literal("采矿镭射枪"))

        cat.addEntry(eb.startLongField(Text.literal("最大能量（EU）"), maxEnergy)
            .setDefaultValue(200_000L)
            .setSaveConsumer { maxEnergy = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("弹体视觉长度（blocks）"), visualLength)
            .setDefaultValue(1.2)
            .setSaveConsumer { visualLength = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("最大渲染距离（blocks）"), renderDistance)
            .setDefaultValue(256.0)
            .setSaveConsumer { renderDistance = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("弹体最大存活 tick"), maxLife)
            .setDefaultValue(200)
            .setSaveConsumer { maxLife = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("弹体截面半径（blocks）"), bulletRadius)
            .setDefaultValue(0.04)
            .setSaveConsumer { bulletRadius = it }.build())

        // 各模式配置
        val miningReader = laserMode(eb, cat, "采矿模式", cfg.mining, defaultEnergyCost = 2_000L, defaultRange = 64.0,
            defaultSpeed = 1.5, defaultEntityDamage = 4f)
        val lowFocusReader = laserMode(eb, cat, "低聚焦模式", cfg.lowFocus, defaultEnergyCost = 500L, defaultRange = 4.0,
            defaultSpeed = 1.0, defaultEntityDamage = 2f)
        val longRangeReader = laserMode(eb, cat, "远距模式", cfg.longRange, defaultEnergyCost = 5_000L, defaultRange = 64.0,
            defaultSpeed = 3.0, defaultEntityDamage = 6f)
        val superHeatReader = laserMode(eb, cat, "超级热线模式", cfg.superHeat, defaultEnergyCost = 5_000L, defaultRange = 8.0,
            defaultSpeed = 1.5, defaultEntityDamage = 8f)
        val scatterReader = laserMode(eb, cat, "散射模式", cfg.scatter, defaultEnergyCost = 12_500L, defaultRange = 10.0,
            defaultSpeed = 1.5, defaultEntityDamage = 2f, defaultScatterCount = 25, defaultSpread = 2.5)
        val explosiveReader = laserMode(eb, cat, "爆破模式", cfg.explosive, defaultEnergyCost = 10_000L, defaultRange = 10.0,
            defaultSpeed = 1.5, defaultEntityDamage = 100f, defaultExplosionPower = 4.0f)
        val trenchReader = laserMode(eb, cat, "3x3 模式", cfg.trench3x3, defaultEnergyCost = 7_200L, defaultRange = 20.0,
            defaultSpeed = 1.5, defaultEntityDamage = 2f, defaultScatterCount = 9, defaultSpread = 3.0)

        return {
            MiningLaserConfig(
                maxEnergy = maxEnergy,
                visualLength = visualLength,
                renderDistance = renderDistance,
                maxLife = maxLife,
                bulletRadius = bulletRadius,
                mining = miningReader(),
                lowFocus = lowFocusReader(),
                longRange = longRangeReader(),
                superHeat = superHeatReader(),
                scatter = scatterReader(),
                explosive = explosiveReader(),
                trench3x3 = trenchReader()
            )
        }
    }

    private fun laserMode(
        eb: ConfigEntryBuilder, cat: me.shedaniel.clothconfig2.api.ConfigCategory,
        label: String, cfg: LaserModeConfig,
        defaultEnergyCost: Long, defaultRange: Double, defaultSpeed: Double,
        defaultEntityDamage: Float, defaultColor: String = "0xFF00BFFF",
        defaultExplosionPower: Float = 0f, defaultScatterCount: Int = 1, defaultSpread: Double = 0.0
    ): () -> LaserModeConfig {
        cat.addEntry(eb.startTextDescription(Text.literal("§6§l--- $label ---")).build())

        var energyCost = cfg.energyCost
        var range = cfg.range
        var speed = cfg.speed
        var explosionPower = cfg.explosionPower
        var color = cfg.color
        var scatterCount = cfg.scatterCount
        var scatterSpread = cfg.scatterSpread
        var entityDamage = cfg.entityDamage

        cat.addEntry(eb.startLongField(Text.literal("  §7每发消耗能量（EU）"), energyCost)
            .setDefaultValue(defaultEnergyCost).setSaveConsumer { energyCost = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("  §7最大射程（blocks）"), range)
            .setDefaultValue(defaultRange).setSaveConsumer { range = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("  §7弹体飞行速度（blocks/tick）"), speed)
            .setDefaultValue(defaultSpeed).setSaveConsumer { speed = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("  §7爆炸威力（0=不爆炸, 4=TNT）"), explosionPower.toDouble())
            .setDefaultValue(defaultExplosionPower.toDouble()).setSaveConsumer { explosionPower = it.toFloat() }.build())
        cat.addEntry(eb.startIntField(Text.literal("  §7视觉颜色（ARGB 整数）"), color)
            .setDefaultValue(0xFF00BFFF.toInt()).setSaveConsumer { color = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("  §7散射弹体数量"), scatterCount)
            .setDefaultValue(defaultScatterCount).setSaveConsumer { scatterCount = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("  §7散射张角（度）"), scatterSpread)
            .setDefaultValue(defaultSpread).setSaveConsumer { scatterSpread = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("  §7对实体伤害"), entityDamage.toDouble())
            .setDefaultValue(defaultEntityDamage.toDouble()).setSaveConsumer { entityDamage = it.toFloat() }.build())

        return {
            LaserModeConfig(
                energyCost = energyCost,
                range = range,
                speed = speed,
                explosionPower = explosionPower,
                color = color,
                scatterCount = scatterCount,
                scatterSpread = scatterSpread,
                entityDamage = entityDamage
            )
        }
    }

    // ==================== 护甲 ====================

    private fun armorCategory(
        builder: ConfigBuilder, eb: ConfigEntryBuilder, cfg: ArmorConfig
    ): () -> ArmorConfig {
        val jetpackReader = jetpackConfig(eb, builder, cfg.jetpack)
        val electricJetpackReader = electricJetpackConfig(eb, builder, cfg.electricJetpack)
        val chestplateReader = quantumChestplateConfig(eb, builder, cfg.quantumChestplate)
        val helmetReader = quantumHelmetConfig(eb, builder, cfg.quantumHelmet)
        val leggingsReader = quantumLeggingsConfig(eb, builder, cfg.quantumLeggings)
        val bootsReader = quantumBootsConfig(eb, builder, cfg.quantumBoots)
        val nightVisionReader = nightVisionConfig(eb, builder, cfg.nightVision)
        val rubberBootsReader = rubberBootsConfig(eb, builder, cfg.rubberBoots)

        return {
            ArmorConfig(
                jetpack = jetpackReader(),
                electricJetpack = electricJetpackReader(),
                quantumChestplate = chestplateReader(),
                quantumHelmet = helmetReader(),
                quantumLeggings = leggingsReader(),
                quantumBoots = bootsReader(),
                nightVision = nightVisionReader(),
                rubberBoots = rubberBootsReader()
            )
        }
    }

    private fun jetpackConfig(
        eb: ConfigEntryBuilder, builder: ConfigBuilder, cfg: JetpackConfig
    ): () -> JetpackConfig {
        val cat = builder.getOrCreateCategory(Text.literal("护甲 • 喷气背包"))
        var maxFuel = cfg.maxFuel
        var flightDuration = cfg.flightDurationSeconds

        cat.addEntry(eb.startLongField(Text.literal("燃料容量（mB）"), maxFuel)
            .setDefaultValue(30_000L).setSaveConsumer { maxFuel = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("飞行时长（秒）"), flightDuration)
            .setDefaultValue(750).setSaveConsumer { flightDuration = it }.build())

        return { JetpackConfig(maxFuel = maxFuel, flightDurationSeconds = flightDuration) }
    }

    private fun electricJetpackConfig(
        eb: ConfigEntryBuilder, builder: ConfigBuilder, cfg: ElectricJetpackConfig
    ): () -> ElectricJetpackConfig {
        val cat = builder.getOrCreateCategory(Text.literal("护甲 • 电力喷气背包"))
        var maxEnergy = cfg.maxEnergy
        var flightDuration = cfg.flightDurationSeconds

        cat.addEntry(eb.startLongField(Text.literal("最大能量（EU）"), maxEnergy)
            .setDefaultValue(30_000L).setSaveConsumer { maxEnergy = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("飞行时长（秒）"), flightDuration)
            .setDefaultValue(750).setSaveConsumer { flightDuration = it }.build())

        return { ElectricJetpackConfig(maxEnergy = maxEnergy, flightDurationSeconds = flightDuration) }
    }

    private fun quantumChestplateConfig(
        eb: ConfigEntryBuilder, builder: ConfigBuilder, cfg: QuantumChestplateConfig
    ): () -> QuantumChestplateConfig {
        val cat = builder.getOrCreateCategory(Text.literal("护甲 • 量子胸甲"))
        var maxEnergy = cfg.maxEnergy
        var flightDuration = cfg.flightDurationSeconds

        cat.addEntry(eb.startLongField(Text.literal("最大能量（EU）"), maxEnergy)
            .setDefaultValue(10_000_000L).setSaveConsumer { maxEnergy = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("飞行时长（秒）"), flightDuration)
            .setDefaultValue(1200).setSaveConsumer { flightDuration = it }.build())

        return { QuantumChestplateConfig(maxEnergy = maxEnergy, flightDurationSeconds = flightDuration) }
    }

    private fun quantumHelmetConfig(
        eb: ConfigEntryBuilder, builder: ConfigBuilder, cfg: QuantumHelmetConfig
    ): () -> QuantumHelmetConfig {
        val cat = builder.getOrCreateCategory(Text.literal("护甲 • 量子头盔"))
        var nightVisionDuration = cfg.nightVisionDurationSeconds

        cat.addEntry(eb.startIntField(Text.literal("夜视时长（秒）"), nightVisionDuration)
            .setDefaultValue(28800).setSaveConsumer { nightVisionDuration = it }.build())

        return { QuantumHelmetConfig(nightVisionDurationSeconds = nightVisionDuration) }
    }

    private fun quantumLeggingsConfig(
        eb: ConfigEntryBuilder, builder: ConfigBuilder, cfg: QuantumLeggingsConfig
    ): () -> QuantumLeggingsConfig {
        val cat = builder.getOrCreateCategory(Text.literal("护甲 • 量子护腿"))
        var maxEnergy = cfg.maxEnergy
        var speedDuration = cfg.speedBoostDurationSeconds
        var speed1 = cfg.speedMultiplierTier1
        var speed2 = cfg.speedMultiplierTier2

        cat.addEntry(eb.startLongField(Text.literal("参考容量（EU）"), maxEnergy)
            .setDefaultValue(10_000_000L).setSaveConsumer { maxEnergy = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("神行时长（秒，2档满电）"), speedDuration)
            .setDefaultValue(1800).setSaveConsumer { speedDuration = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("1 档速度倍率"), speed1)
            .setDefaultValue(0.2).setSaveConsumer { speed1 = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("2 档速度倍率"), speed2)
            .setDefaultValue(0.4).setSaveConsumer { speed2 = it }.build())

        return {
            QuantumLeggingsConfig(
                maxEnergy = maxEnergy,
                speedBoostDurationSeconds = speedDuration,
                speedMultiplierTier1 = speed1,
                speedMultiplierTier2 = speed2
            )
        }
    }

    private fun quantumBootsConfig(
        eb: ConfigEntryBuilder, builder: ConfigBuilder, cfg: QuantumBootsConfig
    ): () -> QuantumBootsConfig {
        val cat = builder.getOrCreateCategory(Text.literal("护甲 • 量子靴子"))
        var maxEnergy = cfg.maxEnergy
        var jumpCost = cfg.jumpEnergyCost
        var jumpHeight = cfg.jumpHeightMultiplier

        cat.addEntry(eb.startLongField(Text.literal("参考容量（EU）"), maxEnergy)
            .setDefaultValue(10_000_000L).setSaveConsumer { maxEnergy = it }.build())
        cat.addEntry(eb.startLongField(Text.literal("每次大跳消耗能量（EU）"), jumpCost)
            .setDefaultValue(10_000L).setSaveConsumer { jumpCost = it }.build())
        cat.addEntry(eb.startDoubleField(Text.literal("跳跃高度倍率"), jumpHeight)
            .setDefaultValue(3.0).setSaveConsumer { jumpHeight = it }.build())

        return {
            QuantumBootsConfig(
                maxEnergy = maxEnergy,
                jumpEnergyCost = jumpCost,
                jumpHeightMultiplier = jumpHeight
            )
        }
    }

    private fun nightVisionConfig(
        eb: ConfigEntryBuilder, builder: ConfigBuilder, cfg: NightVisionConfig
    ): () -> NightVisionConfig {
        val cat = builder.getOrCreateCategory(Text.literal("护甲 • 夜视"))
        var gogglesMaxEnergy = cfg.nightVisionGogglesMaxEnergy
        var gogglesDuration = cfg.nightVisionGogglesDurationSeconds
        var nanoMaxEnergy = cfg.nanoHelmetMaxEnergy
        var nanoDuration = cfg.nanoHelmetDurationSeconds

        cat.addEntry(eb.startLongField(Text.literal("夜视镜最大能量（EU）"), gogglesMaxEnergy)
            .setDefaultValue(100_000L).setSaveConsumer { gogglesMaxEnergy = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("夜视镜夜视时长（秒）"), gogglesDuration)
            .setDefaultValue(5000).setSaveConsumer { gogglesDuration = it }.build())
        cat.addEntry(eb.startLongField(Text.literal("纳米头盔最大能量（EU）"), nanoMaxEnergy)
            .setDefaultValue(1_000_000L).setSaveConsumer { nanoMaxEnergy = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("纳米头盔夜视时长（秒）"), nanoDuration)
            .setDefaultValue(3571).setSaveConsumer { nanoDuration = it }.build())

        return {
            NightVisionConfig(
                nightVisionGogglesMaxEnergy = gogglesMaxEnergy,
                nightVisionGogglesDurationSeconds = gogglesDuration,
                nanoHelmetMaxEnergy = nanoMaxEnergy,
                nanoHelmetDurationSeconds = nanoDuration
            )
        }
    }

    private fun rubberBootsConfig(
        eb: ConfigEntryBuilder, builder: ConfigBuilder, cfg: RubberBootsConfig
    ): () -> RubberBootsConfig {
        val cat = builder.getOrCreateCategory(Text.literal("护甲 • 橡胶靴"))
        var distance = cfg.distance
        var eu = cfg.eu

        cat.addEntry(eb.startDoubleField(Text.literal("行走格数触发一次充电"), distance)
            .setDefaultValue(1.0).setSaveConsumer { distance = it }.build())
        cat.addEntry(eb.startLongField(Text.literal("每次充电 EU"), eu)
            .setDefaultValue(20L).setSaveConsumer { eu = it }.build())

        return { RubberBootsConfig(distance = distance, eu = eu) }
    }

    // ==================== 世界生成 ====================

    private fun worldgenCategory(
        builder: ConfigBuilder, eb: ConfigEntryBuilder, cfg: WorldgenConfig
    ): () -> WorldgenConfig {
        val rubberTreeReader = rubberTreeConfig(eb, builder, cfg.rubberTree)
        val peatOreReader = peatOreConfig(eb, builder, cfg.peatOre)

        return { WorldgenConfig(rubberTree = rubberTreeReader(), peatOre = peatOreReader()) }
    }

    private fun rubberTreeConfig(
        eb: ConfigEntryBuilder, builder: ConfigBuilder, cfg: RubberTreeWorldgenConfig
    ): () -> RubberTreeWorldgenConfig {
        val cat = builder.getOrCreateCategory(Text.literal("世界生成 • 橡胶树"))
        var enabled = cfg.enabled
        var biomes = cfg.biomes.toMutableList()
        var countPerChunk = cfg.countPerChunk
        var rarityChance = cfg.rarityChance
        var maxWaterDepth = cfg.maxWaterDepth
        var baseHeight = cfg.baseHeight
        var heightRandA = cfg.heightRandA
        var heightRandB = cfg.heightRandB
        var foliageRadius = cfg.foliageRadius
        var foliageOffset = cfg.foliageOffset
        var foliageHeight = cfg.foliageHeight
        var zeroHole = cfg.zeroHoleWeight
        var singleHole = cfg.singleHoleWeight
        var doubleHole = cfg.doubleHoleWeight
        var ignoreVines = cfg.ignoreVines
        var forceDirt = cfg.forceDirt

        cat.addEntry(eb.startBooleanToggle(Text.literal("启用自然生成"), enabled)
            .setDefaultValue(true)
            .setTooltip(Text.literal("是否允许自然生成橡胶树。变更后需要重启。"))
            .setSaveConsumer { enabled = it }.build())
        cat.addEntry(eb.startStrList(Text.literal("允许生成的生物群系"), biomes)
            .setDefaultValue(DEFAULT_RUBBER_BIOMES)
            .setTooltip(Text.literal("生物群系列表，填写 biome id。变更后需要重启。"))
            .setSaveConsumer { biomes = it.toMutableList() }.build())
        cat.addEntry(eb.startIntField(Text.literal("每区块放置尝试次数"), countPerChunk)
            .setDefaultValue(1).setSaveConsumer { countPerChunk = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("稀有度"), rarityChance)
            .setDefaultValue(64).setTooltip(Text.literal("64 表示每次尝试平均 1/64 概率通过"))
            .setSaveConsumer { rarityChance = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("最大地表水深"), maxWaterDepth)
            .setDefaultValue(0).setSaveConsumer { maxWaterDepth = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("树干基础高度"), baseHeight)
            .setDefaultValue(6).setSaveConsumer { baseHeight = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("第一段随机高度"), heightRandA)
            .setDefaultValue(2).setSaveConsumer { heightRandA = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("第二段随机高度"), heightRandB)
            .setDefaultValue(0).setSaveConsumer { heightRandB = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("树冠半径"), foliageRadius)
            .setDefaultValue(2).setSaveConsumer { foliageRadius = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("树冠垂直偏移"), foliageOffset)
            .setDefaultValue(0).setSaveConsumer { foliageOffset = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("树冠高度"), foliageHeight)
            .setDefaultValue(4).setSaveConsumer { foliageHeight = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("0 孔权重"), zeroHole)
            .setDefaultValue(11).setSaveConsumer { zeroHole = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("1 孔权重"), singleHole)
            .setDefaultValue(2).setSaveConsumer { singleHole = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("2 孔权重"), doubleHole)
            .setDefaultValue(1).setSaveConsumer { doubleHole = it }.build())
        cat.addEntry(eb.startBooleanToggle(Text.literal("忽略藤蔓阻挡"), ignoreVines)
            .setDefaultValue(true).setSaveConsumer { ignoreVines = it }.build())
        cat.addEntry(eb.startBooleanToggle(Text.literal("强制替换树底为泥土"), forceDirt)
            .setDefaultValue(false).setSaveConsumer { forceDirt = it }.build())

        return {
            RubberTreeWorldgenConfig(
                enabled = enabled,
                biomes = biomes.toList(),
                countPerChunk = countPerChunk,
                rarityChance = rarityChance,
                maxWaterDepth = maxWaterDepth,
                baseHeight = baseHeight,
                heightRandA = heightRandA,
                heightRandB = heightRandB,
                foliageRadius = foliageRadius,
                foliageOffset = foliageOffset,
                foliageHeight = foliageHeight,
                zeroHoleWeight = zeroHole,
                singleHoleWeight = singleHole,
                doubleHoleWeight = doubleHole,
                ignoreVines = ignoreVines,
                forceDirt = forceDirt
            )
        }
    }

    private fun peatOreConfig(
        eb: ConfigEntryBuilder, builder: ConfigBuilder, cfg: PeatOreWorldgenConfig
    ): () -> PeatOreWorldgenConfig {
        val cat = builder.getOrCreateCategory(Text.literal("世界生成 • 泥炭矿"))
        var enabled = cfg.enabled
        var biomes = cfg.biomes.toMutableList()
        var veinsPerChunk = cfg.veinsPerChunk
        var veinSize = cfg.veinSize
        var minY = cfg.minY
        var maxY = cfg.maxY

        cat.addEntry(eb.startBooleanToggle(Text.literal("启用自然生成"), enabled)
            .setDefaultValue(true)
            .setTooltip(Text.literal("是否允许自然生成泥炭矿。变更后需要重启。"))
            .setSaveConsumer { enabled = it }.build())
        cat.addEntry(eb.startStrList(Text.literal("允许生成的生物群系"), biomes)
            .setDefaultValue(DEFAULT_PEAT_BIOMES)
            .setTooltip(Text.literal("生物群系列表，填写 biome id。变更后需要重启。"))
            .setSaveConsumer { biomes = it.toMutableList() }.build())
        cat.addEntry(eb.startIntField(Text.literal("每区块矿脉数量"), veinsPerChunk)
            .setDefaultValue(24)
            .setTooltip(Text.literal("每个区块内生成多少条矿脉。"))
            .setSaveConsumer { veinsPerChunk = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("矿脉大小"), veinSize)
            .setDefaultValue(24)
            .setTooltip(Text.literal("每条矿脉最大包含的方块数。"))
            .setSaveConsumer { veinSize = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("最低生成高度（Y）"), minY)
            .setDefaultValue(-16)
            .setSaveConsumer { minY = it }.build())
        cat.addEntry(eb.startIntField(Text.literal("最高生成高度（Y）"), maxY)
            .setDefaultValue(64)
            .setSaveConsumer { maxY = it }.build())

        return {
            PeatOreWorldgenConfig(
                enabled = enabled,
                biomes = biomes.toList(),
                veinsPerChunk = veinsPerChunk,
                veinSize = veinSize,
                minY = minY,
                maxY = maxY
            )
        }
    }

    private val DEFAULT_RUBBER_BIOMES = listOf(
        "minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest",
        "minecraft:dark_forest", "minecraft:taiga", "minecraft:old_growth_pine_taiga",
        "minecraft:old_growth_spruce_taiga", "minecraft:jungle", "minecraft:sparse_jungle",
        "minecraft:bamboo_jungle", "minecraft:swamp"
    )

    private val DEFAULT_PEAT_BIOMES = listOf(
        "minecraft:jungle", "minecraft:sparse_jungle",
        "minecraft:bamboo_jungle", "minecraft:mushroom_fields"
    )
}
