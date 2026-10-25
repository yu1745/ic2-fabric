package ic2_120.compat

import net.minecraft.core.Registry
import net.minecraft.data.tags.TagAppender
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey

/** Bridges old datagen call sites to the 26.2 tag appender, which accepts resource keys. */
class LegacyTagAppender26<T : Any>(
    private val registry: Registry<T>,
    private val makeAppender: (Boolean) -> TagAppender<T>
) {
    private var appender = makeAppender(false)

    fun setReplace(replace: Boolean): LegacyTagAppender26<T> {
        appender = makeAppender(replace)
        return this
    }

    fun add(value: T): LegacyTagAppender26<T> {
        appender.add(registry.getResourceKey(value).orElseThrow())
        return this
    }

    fun add(key: ResourceKey<T>): LegacyTagAppender26<T> {
        appender.add(key)
        return this
    }

    fun addTag(tag: TagKey<T>): LegacyTagAppender26<T> {
        appender.addTag(tag)
        return this
    }

    fun addOptionalTag(id: Identifier): LegacyTagAppender26<T> {
        appender.addOptionalTag(TagKey.create(registry.key(), id))
        return this
    }
}
