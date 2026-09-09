package qikahome.fabricatedforgefluid.mixin;

import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelManager.ReloadState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import qikahome.fabricatedforgefluid.event.AtlasLoadHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * 在 {@link ModelManager#loadModels} 开头（= atlas 全部加载完成、bake 开始之前）触发
 * {@link AtlasLoadHooks#onAtlasesReady}，提供一个"atlas 就绪"的精确时机钩子。
 * <p>
 * 依据 {@code ModelManager.reload} 的调度：loadModels 在
 * {@code CompletableFuture.allOf(atlasFutures + modelBakery)} 门闩之后才执行，
 * 此时 {@code stitchResults} 已 join（atlas 已加载完），bake 尚未开始。
 */
@Mixin(ModelManager.class)
public abstract class MixinModelManager {
    @Inject(method = "loadModels", at = @At("HEAD"))
    private void fff$atlasesReady(ProfilerFiller profiler,
            Map<ResourceLocation, AtlasSet.StitchResult> stitchResults,
            ModelBakery modelBakery,
            CallbackInfoReturnable<ReloadState> ci) {
        AtlasLoadHooks.onAtlasesReady(stitchResults);
    }
}
