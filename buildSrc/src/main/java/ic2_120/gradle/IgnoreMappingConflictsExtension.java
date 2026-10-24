package ic2_120.gradle;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Queue;
import java.util.Set;

import javax.inject.Inject;

import org.objectweb.asm.ClassVisitor;

import net.fabricmc.loom.api.remapping.RemapperContext;
import net.fabricmc.loom.api.remapping.RemapperExtension;
import net.fabricmc.loom.api.remapping.RemapperParameters;
import net.fabricmc.loom.api.remapping.TinyRemapperExtension;
import net.fabricmc.tinyremapper.TinyRemapper;

/**
 * TinyRemapper 扩展：启用 ignoreConflicts，解决 MC 1.21.1 Yarn 映射中
 * Inventory.isEmpty() 与 RecipeInput.isEmpty() 的映射冲突。
 */
public class IgnoreMappingConflictsExtension implements RemapperExtension<RemapperParameters.None>, TinyRemapperExtension {

    private static final Set<Integer> CONFIGURED_REMAPPER_IDS = Collections.synchronizedSet(new HashSet<>());

    @Inject
    public IgnoreMappingConflictsExtension() {
    }

    @Override
    public ClassVisitor insertVisitor(String className, RemapperContext remapperContext, ClassVisitor classVisitor) {
        try {
            Object remapper = remapperContext.remapper();
            Field trField = remapper.getClass().getDeclaredField("tr");
            trField.setAccessible(true);
            TinyRemapper tinyRemapper = (TinyRemapper) trField.get(remapper);
            setIgnoreConflictsIfNeeded(tinyRemapper, "insertVisitor");
        } catch (Exception e) {
            System.err.println("[IC2 Loom] Failed to set ignoreConflicts: " + e);
        }

        return classVisitor;
    }

    @Override
    public TinyRemapper.AnalyzeVisitorProvider getAnalyzeVisitorProvider(Context context) {
        return (version, className, classVisitor) -> {
            try {
                // Analyze phase happens before conflict handling; set as early as possible.
                trySetIgnoreConflictsFromObjectGraph(classVisitor);
            } catch (Exception e) {
                System.err.println("[IC2 Loom] Analyze-phase ignoreConflicts hook failed: " + e);
            }
            return classVisitor;
        };
    }

    private static boolean trySetIgnoreConflictsFromObjectGraph(Object root) {
        if (root == null) {
            return false;
        }

        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Queue<Object> queue = new ArrayDeque<>();
        queue.add(root);

        int maxNodes = 512;
        while (!queue.isEmpty() && maxNodes-- > 0) {
            Object current = queue.poll();
            if (current == null || !visited.add(current)) {
                continue;
            }

            if (setIgnoreConflictsIfTinyRemapper(current)) {
                return true;
            }

            Class<?> type = current.getClass();
            int depth = 0;
            while (type != null && type != Object.class && depth++ < 6) {
                for (Field field : type.getDeclaredFields()) {
                    if (field.getType().isPrimitive()) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object child;
                    try {
                        child = field.get(current);
                    } catch (Throwable ignored) {
                        continue;
                    }
                    if (child != null) {
                        queue.add(child);
                    }
                }
                type = type.getSuperclass();
            }
        }
        return false;
    }

    private static boolean setIgnoreConflictsIfTinyRemapper(Object candidate) {
        if (!(candidate instanceof TinyRemapper tinyRemapper)) {
            return false;
        }

        try {
            setIgnoreConflictsIfNeeded(tinyRemapper, "analyze phase");
            return true;
        } catch (Exception e) {
            System.err.println("[IC2 Loom] Failed to set ignoreConflicts on TinyRemapper: " + e);
            return false;
        }
    }

    private static void setIgnoreConflictsIfNeeded(TinyRemapper tinyRemapper, String source) throws Exception {
        int id = System.identityHashCode(tinyRemapper);
        if (!CONFIGURED_REMAPPER_IDS.add(id)) {
            return;
        }

        Field ignoreField = TinyRemapper.class.getDeclaredField("ignoreConflicts");
        ignoreField.setAccessible(true);
        ignoreField.setBoolean(tinyRemapper, true);
        System.out.println("[IC2 Loom] Set TinyRemapper.ignoreConflicts = true (" + source + ")");
    }
}
