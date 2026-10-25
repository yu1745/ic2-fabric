package ic2_120.compat

import java.util.function.Consumer
import net.minecraft.network.chat.Component

/** Adapts the mod's existing list based tooltip helpers to the 26.2 consumer API. */
class TooltipSinkList(private val sink: Consumer<Component>) : ArrayList<Component>() {
    override fun add(element: Component): Boolean {
        sink.accept(element)
        return super.add(element)
    }

    override fun add(index: Int, element: Component) {
        sink.accept(element)
        super.add(index, element)
    }

    override fun addAll(elements: Collection<Component>): Boolean {
        elements.forEach(sink::accept)
        return super.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<Component>): Boolean {
        elements.forEach(sink::accept)
        return super.addAll(index, elements)
    }
}
