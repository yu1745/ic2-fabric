package ic2_120.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.resources.ResourceLocation;

public class RubberLogModelHelper {
    public static ResourceLocation getModelId(ModelModifier.OnLoad.Context context) {
        return context.resourceId();
    }
}
