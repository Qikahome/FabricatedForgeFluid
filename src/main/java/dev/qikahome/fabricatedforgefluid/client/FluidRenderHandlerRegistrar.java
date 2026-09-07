package dev.qikahome.fabricatedforgefluid.client;

import dev.qikahome.fabricatedforgefluid.FabricatedForgeFluid;
import dev.qikahome.fabricatedforgefluid.fluids.FabricatedFluidType;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

/**
 * 客户端自动渲染注册（对应 Forge 的 FluidType 客户端扩展自动发现）：
 * 启动后遍历流体注册表，对所有关联 FluidType 为 {@link FabricatedFluidType} 的流体
 * 调用 {@code initializeClient} 并注册进 {@link FluidRenderHandlerRegistry}。
 * <p>
 * 服务端安全：本类仅被客户端入口调用。
 */
public final class FluidRenderHandlerRegistrar {
    private FluidRenderHandlerRegistrar() {
    }

    public static void init() {
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            FluidType type = fluid.getFluidType();
            if (type instanceof FabricatedFluidType fabricated) {
                try {
                    fabricated.initializeClient(handler -> FluidRenderHandlerRegistry.INSTANCE.register(fluid, handler));
                } catch (Throwable e) {
                    FabricatedForgeFluid.LOGGER.error("Failed registering fluid render handler for {}", fluid, e);
                }
            }
        }
    }
}
