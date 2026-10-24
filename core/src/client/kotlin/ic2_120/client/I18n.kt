package ic2_120.client

import net.minecraft.client.resources.language.I18n as McI18n

/**
 * Shorthand for [McI18n.get] — returns the translated string for the given key.
 * Usage: `t("gui.ic2_120.label_energy")` or `t("gui.ic2_120.energy_input", "1.5K")`
 */
fun t(key: String, vararg args: Any): String = McI18n.get(key, *args)
