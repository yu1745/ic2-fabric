package addon_template.client

import ic2_120.client.ClientScreenRegistrar
import net.fabricmc.api.ClientModInitializer

object AddonTemplateClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientScreenRegistrar.registerScreens(
            "addon_template",
            listOf("addon_template.client.screen")
        )
    }
}
