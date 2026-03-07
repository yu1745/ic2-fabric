package ic2_120.content

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
 * }
 * ```
 * 服务端：`MyMachineSync(syncedData)`
 * 客户端：`MyMachineSync(SyncedDataView(propertyDelegate))`
 */
interface SyncSchema {
    fun int(name: String, default: Int = 0): ReadWriteProperty<Any?, Int>
}

/**
 * 服务端数据拥有者——实现 [PropertyDelegate] 供 ScreenHandler 同步，
 * 同时实现 [SyncSchema] 供属性定义类注册字段。
 */
class SyncedData : PropertyDelegate, SyncSchema {
    private val entries = mutableListOf<Entry>()

    private class Entry(val name: String, var value: Int)

    override fun int(name: String, default: Int): ReadWriteProperty<Any?, Int> {
        val index = entries.size
        entries.add(Entry(name, default))
        return object : ReadWriteProperty<Any?, Int> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Int = entries[index].value
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                entries[index].value = value
            }
        }
    }

    override fun get(index: Int): Int = entries[index].value
    override fun set(index: Int, value: Int) { entries[index].value = value }
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
        val index = nextIndex++
        return object : ReadWriteProperty<Any?, Int> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Int = delegate.get(index)
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                delegate.set(index, value)
            }
        }
    }
}
