package ic2_120.content.fluid

import ic2_120.content.recipes.ModTags
import net.minecraft.fluid.Fluid

/**
 * Shared fluid-fuel definitions. Machines and fuel-powered items use this
 * registry so their tag matching and fuel equivalence stay consistent.
 */
object FluidFuelRegistry {
    data class FuelProfile(
        val euPerBucket: Long,
        val euPerTick: Long,
        /** Jetpack consumption relative to biofuel-equivalent fuel. */
        val jetpackConsumptionMultiplier: Double
    )

    private val BIOFUEL_PROFILE = FuelProfile(32_000L, 16L, 1.0)
    private val CREOSOTE_PROFILE = FuelProfile(3_200L, 8L, 2.0)

    fun isSupported(fluid: Fluid): Boolean = getProfile(fluid) != null

    fun getProfile(fluid: Fluid): FuelProfile? {
        val state = fluid.defaultState
        return when {
            state.isIn(ModTags.Compat.Fluids.SEMIFLUID_BIOFUEL_EQUIVALENT) -> BIOFUEL_PROFILE
            state.isIn(ModTags.Compat.Fluids.SEMIFLUID_CREOSOTE_EQUIVALENT) -> CREOSOTE_PROFILE
            else -> null
        }
    }
}
