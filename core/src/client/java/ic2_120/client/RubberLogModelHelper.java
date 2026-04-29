package ic2_120.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.util.Identifier;

public class RubberLogModelHelper {
    public static Identifier getModelId(ModelModifier.OnLoad.Context context) {
        return context.resourceId();
    }
}
