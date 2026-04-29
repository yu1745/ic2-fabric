package ic2_120.client

import net.minecraft.client.resource.language.I18n as McI18n

/**
 * Shorthand for [McI18n.translate] — returns the translated string for the given key.
 * Usage: `t("gui.ic2_120.label_energy")` or `t("gui.ic2_120.energy_input", "1.5K")`
 */
fun t(key: String, vararg args: Any): String = McI18n.translate(key, *args)
