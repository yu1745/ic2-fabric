package ic2_120.content.syncs

import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.PropertyDelegate
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 同步属性的工厂接口。
 * [SyncedData]（服务端）和 [SyncedDataView]（客户端）均实现此接口，
 * 使同一个属性定义类可在两端复用，从构造上保证 index 顺序一致。
 *
 * 用法——定义一次，两端共享：
 * ```
 * class MyMachineSync(schema: SyncSchema) {
 *     var counter  by schema.int("Counter")
 *     var energy   by schema.int("Energy", default = 1000)
 *     var progress by schema.int("Progress")
 *     var avgRate  by schema.intAveraged("AvgRate", windowSize = 20) // 滑动窗口平均
 * }
 * ```
 * 服务端：`MyMachineSync(syncedData)`
 * 客户端：`MyMachineSync(SyncedDataView(propertyDelegate))`
 */
interface SyncSchema {
    fun int(name: String, default: Int = 0): ReadWriteProperty<Any?, Int>
    /**
     * 创建一个带滑动窗口平均滤波的整型属性。
     * 每次赋值会将当前值加入滑动窗口，返回值为窗口内所有值的平均值。
     *
     * @param name 属性名称
     * @param default 默认值
     * @param windowSize 滑动窗口大小（tick 数），默认 20（1 秒）
     */
    fun intAveraged(name: String, default: Int = 0, windowSize: Int = 20): ReadWriteProperty<Any?, Int>
}

/**
 * 服务端数据拥有者——实现 [PropertyDelegate] 供 ScreenHandler 同步（网络包下发客户端），
 * 同时实现 [SyncSchema] 供属性定义类注册字段。
 *
 * 属性写入**不会**触发 [net.minecraft.block.entity.BlockEntity.markDirty]：
 * 同步字段大多是每 tick 变化的运行时/显示数据，不该让区块持续变脏。
 * NBT 落盘由区块自然保存（卸载 / 关服 / 业务代码显式 markDirty）时调用 [writeNbt] 完成；
 * 需要即时保存的关键事件（模式切换、配置修改等）由业务代码显式调用 `markDirty()`。
 */
class SyncedData : PropertyDelegate, SyncSchema {
    private val entries = mutableListOf<Entry>()

    private class Entry(val name: String, var value: Int)

    override fun int(name: String, default: Int): ReadWriteProperty<Any?, Int> {
        val indexHigh = entries.size
        entries.add(Entry("${name}_High", default ushr 16))
        val indexLow = entries.size
        entries.add(Entry("${name}_Low", default and 0xFFFF))
        return object : ReadWriteProperty<Any?, Int> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
                val high = entries[indexHigh].value
                val low = entries[indexLow].value
                return (high shl 16) or (low and 0xFFFF)
            }
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                if (getValue(thisRef, property) == value) return
                entries[indexHigh].value = value ushr 16
                entries[indexLow].value = value and 0xFFFF
            }
        }
    }

    override fun intAveraged(name: String, default: Int, windowSize: Int): ReadWriteProperty<Any?, Int> {
        val indexHigh = entries.size
        entries.add(Entry("${name}_High", default ushr 16))
        val indexLow = entries.size
        entries.add(Entry("${name}_Low", default and 0xFFFF))
        // 为每个属性维护独立的滑动窗口；windowSum 增量维护，避免每次读写 O(n) 求和。
        // 环形 int 数组替代 ArrayDeque<Int>：无装箱、零分配（每 tick × 每机器 × 每字段都会写入）。
        // 注意：不能对等值写入做短路——窗口按“每次写入”滑动，等值写入同样会推出
        // 旧样本（如稳态 32 EU/t 会把停机期的 0 推出窗口）；若跳过，平均值会卡在
        // 历史混合值永不收敛到稳态（EnergyFlowSync/HeatFlowSync 每 tick 恒定写入）。
        val buf = IntArray(windowSize)
        var pos = 0          // 下一个写入位置（环形）
        var count = 0        // 窗口内有效样本数
        var windowSum = 0
        return object : ReadWriteProperty<Any?, Int> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
                // 返回滑动窗口的平均值
                return if (count == 0) {
                    val high = entries[indexHigh].value
                    val low = entries[indexLow].value
                    (high shl 16) or (low and 0xFFFF)
                } else windowSum / count
            }
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                // 环形窗口：写入当前位置，替换最旧样本（窗口满时）
                windowSum += value - buf[pos]
                buf[pos] = value
                pos++
                if (pos == windowSize) pos = 0
                if (count < windowSize) count++
                // 同步平均值到 entry（用于 NBT 序列化）
                val avg = windowSum / count
                entries[indexHigh].value = avg ushr 16
                entries[indexLow].value = avg and 0xFFFF
            }
        }
    }

    override fun get(index: Int): Int = entries[index].value
    override fun set(index: Int, value: Int) {
        if (entries[index].value == value) return
        entries[index].value = value
    }
    override fun size(): Int = entries.size

    fun readNbt(nbt: NbtCompound) {
        for (entry in entries) entry.value = nbt.getInt(entry.name)
    }

    fun writeNbt(nbt: NbtCompound) {
        for (entry in entries) nbt.putInt(entry.name, entry.value)
    }
}

/**
 * 客户端视图——包装 ScreenHandler 传入的 [PropertyDelegate]，
 * 实现 [SyncSchema] 使属性定义类可在客户端复用。
 * index 按 [int] 调用顺序自动递增，与 [SyncedData] 对齐。
 */
class SyncedDataView(private val delegate: PropertyDelegate) : SyncSchema {
    private var nextIndex = 0

    override fun int(name: String, default: Int): ReadWriteProperty<Any?, Int> {
        val indexHigh = nextIndex++
        val indexLow = nextIndex++
        return object : ReadWriteProperty<Any?, Int> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
                val high = delegate.get(indexHigh)
                val low = delegate.get(indexLow)
                return (high shl 16) or (low and 0xFFFF)
            }
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                delegate.set(indexHigh, value ushr 16)
                delegate.set(indexLow, value and 0xFFFF)
            }
        }
    }

    override fun intAveraged(name: String, default: Int, windowSize: Int): ReadWriteProperty<Any?, Int> {
        // 客户端不需要滤波，直接返回服务端计算好的平均值
        return int(name, default)
    }
}
