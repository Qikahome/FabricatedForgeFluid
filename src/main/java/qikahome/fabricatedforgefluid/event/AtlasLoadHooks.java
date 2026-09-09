package qikahome.fabricatedforgefluid.event;

import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * "atlas 就绪"钩子：由 {@code MixinModelManager} 在 atlas 全部加载完成、
 * bake 开始之前调用（见 mixin 的 javadoc 时序说明）。
 * <p>
 * 注意 StitchResult 只在当前资源重载周期内有效，勿长期持有。
 */
public final class AtlasLoadHooks {
    private static final Set<Consumer<Map<ResourceLocation, AtlasSet.StitchResult>>> LISTENERS = new HashSet<>();

    private AtlasLoadHooks() {
    }

    public static void onAtlasesReady(Map<ResourceLocation, AtlasSet.StitchResult> atlases) {
        LISTENERS.forEach(l -> l.accept(atlases));
    }

    public static void register(Consumer<Map<ResourceLocation, AtlasSet.StitchResult>> listener) {
        LISTENERS.add(listener);
    }
}
