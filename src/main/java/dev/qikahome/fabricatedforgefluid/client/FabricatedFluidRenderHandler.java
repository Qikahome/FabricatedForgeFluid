package dev.qikahome.fabricatedforgefluid.client;

import dev.qikahome.fabricatedforgefluid.FabricatedForgeFluid;
import dev.qikahome.fabricatedforgefluid.event.AtlasLoadHooks;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.Nullable;

/**
 * 可指定 still/flowing/overlay 贴图与染色的流体渲染 handler。
 * <p>
 * Fabric 的 {@code SimpleFluidRenderHandler#reloadTextures(TextureAtlas)} 在资源重载的
 * apply 阶段才填充 sprites，晚于模型烘焙；本类改为监听 {@link AtlasLoadHooks}，
 * 在 atlas 就绪（bake 开始前）时从 {@link AtlasSet.StitchResult} 取 sprite，
 * 使依赖烘焙期纹理的模型（如流体桶 loader）不会拿到 null。
 */
public class FabricatedFluidRenderHandler extends SimpleFluidRenderHandler {

    protected final ResourceLocation stillTexture;
    protected final ResourceLocation flowingTexture;
    @Nullable
    protected final ResourceLocation overlayTexture;

    public FabricatedFluidRenderHandler(ResourceLocation stillTexture, ResourceLocation flowingTexture,
            @Nullable ResourceLocation overlayTexture, int tint) {
        super(stillTexture, flowingTexture, overlayTexture, tint);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.overlayTexture = overlayTexture;
        AtlasLoadHooks.register(map -> this.reloadTextures(map.get(InventoryMenu.BLOCK_ATLAS)));
    }

    @Override
    public void reloadTextures(TextureAtlas textureAtlas) {
        // 纹理由 AtlasLoadHooks 在 atlas 就绪时填充，此处不做事（对应旧时序下 apply 阶段的行为）。
    }

    public void reloadTextures(AtlasSet.StitchResult textureAtlas) {
        if (textureAtlas == null) {
            FabricatedForgeFluid.LOGGER.warn("Failed to reload fluid textures as {} is null", InventoryMenu.BLOCK_ATLAS);
            return;
        }
        sprites[0] = textureAtlas.getSprite(stillTexture);
        sprites[1] = textureAtlas.getSprite(flowingTexture);
        if (overlayTexture != null) {
            sprites[2] = textureAtlas.getSprite(overlayTexture);
        }
    }
}
