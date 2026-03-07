package ic2_120

import ic2_120.content.screen.ModScreenHandlers
import ic2_120.registry.ClassScanner
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object Ic2_120 : ModInitializer {

    const val MOD_ID = "ic2_120"

    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        // 使用类级别注解的自动注册系统
        ClassScanner.scanAndRegister(
            MOD_ID,
            listOf(
                "ic2_120.content.tab",    // 扫描物品栏类（必须先注册）
                "ic2_120.content.block", // 扫描方块类
                "ic2_120.content.screen",// 扫描 ScreenHandler 类
                "ic2_120.content.item"   // 扫描物品类
            )
        )
        
        logger.info("IC2 1.20 模组已加载（类注解驱动自动注册）")
    }
}
