package dev.qikahome.fabricatedforgefluid;

import dev.qikahome.fabricatedforgefluid.client.FluidRenderHandlerRegistrar;
import dev.qikahome.fabricatedforgefluid.test.TestFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * FabricatedForgeFluid：在 Fabric 上复刻 Forge 的流体体系（FluidType 驱动）。
 * <ul>
 *   <li>渲染：客户端自动遍历流体注册表，为 {@link dev.qikahome.fabricatedforgefluid.fluids.FabricatedFluidType}
 *       注册世界流体渲染（对应 Forge 的 FluidType#initializeClient + 自动发现）。</li>
 *   <li>实体交互：FluidType 属性驱动的推动/游泳/溺水等物理行为（对应 Forge 的 Entity/LivingEntity 补丁）。</li>
 * </ul>
 */
public class FabricatedForgeFluid implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "fabricatedforgefluid";
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {
        // 测试内容：仅 dev 环境注册（对应 AutoSizedGUI test 包模式）
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            TestFluids.register();
        }
    }

    @Override
    public void onInitializeClient() {
        FluidRenderHandlerRegistrar.init();
    }
}
