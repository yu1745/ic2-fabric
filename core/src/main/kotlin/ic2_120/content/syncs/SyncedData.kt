package ic2_120.content.syncs

import net.minecraft.block.entity.BlockEntity
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
 * 服务端数据拥有者——实现 [PropertyDelegate] 供 ScreenHandler 同步，
 * 同时实现 [SyncSchema] 供属性定义类注册字段。
 * 若传入 [blockEntity]，任意属性写入时会自动调用 [BlockEntity.markDirty]。
 */
class SyncedData(private val blockEntity: BlockEntity? = null) : PropertyDelegate, SyncSchema {
    private val entries = mutableListOf<Entry>()

    private class Entry(val name: String, var value: Int)

    private fun markDirtyIfNeeded() {
        blockEntity?.markDirty()
    }

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
                entries[indexHigh].value = value ushr 16
                entries[indexLow].value = value and 0xFFFF
                markDirtyIfNeeded()
            }
        }
    }

    override fun intAveraged(name: String, default: Int, windowSize: Int): ReadWriteProperty<Any?, Int> {
        val indexHigh = entries.size
        entries.add(Entry("${name}_High", default ushr 16))
        val indexLow = entries.size
        entries.add(Entry("${name}_Low", default and 0xFFFF))
        // 为每个属性维护独立的滑动窗口
        val window = ArrayDeque<Int>()
        return object : ReadWriteProperty<Any?, Int> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
                // 返回滑动窗口的平均值
                return if (window.isEmpty()) {
                    val high = entries[indexHigh].value
                    val low = entries[indexLow].value
                    (high shl 16) or (low and 0xFFFF)
                } else window.sum() / window.size
            }
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                // 更新滑动窗口
                window.addLast(value)
                if (window.size > windowSize) {
                    window.removeFirst()
                }
                // 同步平均值到 entry（用于 NBT 序列化）
                val avg = window.sum() / window.size
                entries[indexHigh].value = avg ushr 16
                entries[indexLow].value = avg and 0xFFFF
                markDirtyIfNeeded()
            }
        }
    }

    override fun get(index: Int): Int = entries[index].value
    override fun set(index: Int, value: Int) {
        entries[index].value = value
        markDirtyIfNeeded()
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
