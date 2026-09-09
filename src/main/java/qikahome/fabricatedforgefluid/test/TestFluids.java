package qikahome.fabricatedforgefluid.test;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import qikahome.fabricatedforgefluid.client.FabricatedFluidRenderHandler;
import qikahome.fabricatedforgefluid.fluids.FabricatedFlowingFluid;
import qikahome.fabricatedforgefluid.fluids.FabricatedFluidType;

import java.util.function.Consumer;

/**
 * dev 环境测试流体（对应 AutoSizedGUI 的 test 包模式：main sourceSet 内 + 条件注册）。
 * <p>
 * 注册一个青色"水类"测试流体（可游泳/可推动，紫色染色 blend 出青色），验证：
 * <ol>
 *   <li>{@link FabricatedFlowingFluid} 可流动流体 + {@link FabricatedFluidType} 的渲染自动注册</li>
 *   <li>实体流体交互（游泳/推动）——由 {@code EntityFluidMixin}/{@code LivingEntityFluidMixin} 驱动</li>
 * </ol>
 */
public final class TestFluids {
    private TestFluids() {
    }

    public static void register() {
        // 0. 坐标与标签：tag 供 tagToTypeMap 反查（需让该流体真实挂上此 tag）
        ResourceLocation stillId = new ResourceLocation("fff", "test");
        ResourceLocation flowingId = new ResourceLocation("fff", "flowing_test");
        TagKey<Fluid> testTag = TagKey.create(Registries.FLUID, stillId);

        // 1. FluidType：匿名子类，物理可游泳/可推动，挂熔岩贴图 + 紫色染色（blend 呈青色）
        FabricatedFluidType type = new FabricatedFluidType(FluidType.Properties.create()
                .canSwim(true)
                .canPushEntity(true)
                .motionScale(0.014D), testTag) {
            @Override
            public void initializeClient(Consumer<FluidRenderHandler> consumer) {
                consumer.accept(new FabricatedFluidRenderHandler(
                        new ResourceLocation("minecraft", "block/lava_still"),
                        new ResourceLocation("minecraft", "block/lava_flow"),
                        null,
                        0xFF00FFFF)); // 青：蓝+绿
            }
        };
        Registry.register(PortingLibFluids.FLUID_TYPES, stillId, type);

        // 2. still / flowing / block：全部用 holder 惰性引用（流动、方块互指）

        FabricatedFlowingFluid[] fluidHolder = new FabricatedFlowingFluid[2];
        LiquidBlock[] blockHolder = new LiquidBlock[1];
        Item[] bucketHolder = new Item[1];

        // 流动参数对齐水的原版行为（slope=4、dropOff=1、tickRate=10）
        FabricatedFlowingFluid.Properties props = new FabricatedFlowingFluid.Properties(type,
                () -> fluidHolder[1], () -> fluidHolder[0])
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(10)
                .block(() -> blockHolder[0])
                .bucket(() -> bucketHolder[0]);
        fluidHolder[0] = new FabricatedFlowingFluid.Flowing(props);
        fluidHolder[1] = new FabricatedFlowingFluid.Source(props);

        FabricatedFlowingFluid flowing = Registry.register(BuiltInRegistries.FLUID, flowingId, fluidHolder[0]);
        FabricatedFlowingFluid still = Registry.register(BuiltInRegistries.FLUID, stillId, fluidHolder[1]);

        // 3. LiquidBlock + 随机 tick（流动必需）。先建后挂进 blockHolder 供 createLegacyBlock 懒解析。
        LiquidBlock block = new LiquidBlock(still, BlockBehaviour.Properties.copy(Blocks.WATER));
        blockHolder[0] = block;
        Registry.register(BuiltInRegistries.BLOCK, stillId, block);

        // 4. bucket item（进 ITEM 注册表，便于放置测试）。先建后挂进 bucketHolder 供 getBucket() 懒解析。
        BucketItem bucketItem = new BucketItem(still, new Item.Properties().stacksTo(1));
        bucketHolder[0] = bucketItem;
        Registry.register(BuiltInRegistries.ITEM, stillId, bucketItem);
    }
}