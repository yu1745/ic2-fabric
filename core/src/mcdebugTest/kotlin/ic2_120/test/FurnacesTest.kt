package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.assertSlotEmpty
import com.mcdebug.runner.assertSlotHas
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.invItemEquals
import com.mcdebug.runner.place
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.waitTicks
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Test

/**
 * 电炉 + 铁炉 (Furnaces) 测试。
 * 电炉槽位：0=输入, 1=输出, 2=放电槽。
 * 铁炉槽位：0=输入, 1=燃料, 2=输出。
 */
@McDebugTest
class FurnacesTest {

    // ---- electric furnace ----

    @Test
    fun electricFurnacePlace(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:electric_furnace")
        assertBlockId(ctx, ctx.origin, "ic2_120:electric_furnace")
    }

    @Test
    fun electricFurnaceSmeltWithAdjacentBatbox(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:electric_furnace")
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        waitUntil(ctx, invItemEquals(ctx.origin, 1, "minecraft:iron_ingot"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:iron_ingot")
    }

    @Test
    fun electricFurnaceNoPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:electric_furnace")
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:iron_ore")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    @Test
    fun electricFurnaceInvalidInputDirt(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:electric_furnace")
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:dirt")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    @Test
    fun electricFurnaceOutputFullBlocksNext(ctx: TestContext) {
        setupAdjacentBatbox(ctx, "ic2_120:electric_furnace")
        setSlot(ctx, ctx.origin, 1, "minecraft:iron_ingot", 64)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:iron_ore")
    }

    // ---- iron furnace ----

    @Test
    fun ironFurnacePlaceAndState(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:iron_furnace")
        assertBlockId(ctx, ctx.origin, "ic2_120:iron_furnace")
    }

    @Test
    fun ironFurnaceSmeltIronOre(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:iron_furnace")
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "minecraft:coal", 1, 1)
        waitUntil(ctx, invItemEquals(ctx.origin, 2, "minecraft:iron_ingot"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 2, "minecraft:iron_ingot")
    }

    @Test
    fun ironFurnaceNoFuelIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:iron_furnace")
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:iron_ore")
        assertSlotEmpty(ctx, ctx.origin, 2)
    }

    @Test
    fun ironFurnaceInvalidInputDirt(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:iron_furnace")
        moveItemStackTo(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "minecraft:coal", 1, 1)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:dirt")
        assertSlotEmpty(ctx, ctx.origin, 2)
    }

    @Test
    fun ironFurnaceOutputFullBlocksNext(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:iron_furnace")
        setSlot(ctx, ctx.origin, 2, "minecraft:iron_ingot", 64)
        moveItemStackTo(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        moveItemStackTo(ctx, ctx.origin, "minecraft:coal", 1, 1)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:iron_ore")
    }
}
