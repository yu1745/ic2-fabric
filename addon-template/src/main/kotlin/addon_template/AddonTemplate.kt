package addon_template

import addon_template.content.tab.ModTabs
import ic2_120.registry.ClassScanner
import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object AddonTemplate : ModInitializer {
    const val MOD_ID = "addon_template"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    fun id(path: String): Identifier = Identifier(MOD_ID, path)

    override fun onInitialize() {
        ClassScanner.scanAndRegister(
            MOD_ID,
            listOf(
                "addon_template.content.tab",
                "addon_template.content.block",
                "addon_template.content.item"
            )
        )

        LOGGER.info("${MOD_ID} initialized")
    }
}
