package dev.qikahome.fabricatedforgefluid.fluids;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Forge
 * {@code FluidType#initializeClient(Consumer<IClientFluidTypeExtensions>)} 的
 * Fabric 对应物。
 * <p>
 * 视觉数据（贴图/颜色）由子类在 {@link #initializeClient(Consumer)} 中通过
 * {@link dev.qikahome.fabricatedforgefluid.client.FabricatedFluidRenderHandler}
 * 交给客户端；
 * {@link dev.qikahome.fabricatedforgefluid.client.FluidRenderHandlerRegistrar}
 * 会在客户端启动时
 * 自动遍历流体注册表，对所有本类型实例调用该方法并注册世界流体渲染（对应 Forge 的自动发现）。
 */
public abstract class FabricatedFluidType extends FluidType {

    public static final Map<TagKey<Fluid>, FabricatedFluidType> tagToTypeMap = new HashMap<>();

    public final TagKey<Fluid> tag;

    public FabricatedFluidType(Properties properties, TagKey<Fluid> tag) {
        super(properties);
        if (tagToTypeMap.put(tag, this) != null)
            throw new IllegalStateException("Duplicate Fluid Tag " + tag + " for FabricatedFluidType.");
        this.tag = tag;
    }

    public void initializeClient(Consumer<FluidRenderHandler> consumer) {
    }
}
