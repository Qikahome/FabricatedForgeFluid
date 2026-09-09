package qikahome.fabricatedforgefluid.fluids;

import io.github.fabricators_of_create.porting_lib.fluids.BaseFlowingFluid;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Forge {@code ForgeFlowingFluid} 的 Fabric 等价物：一个绑定 {@link FabricatedFluidType} 的可流动流体。
 * <p>
 * 复用 Porting Lib {@link BaseFlowingFluid} 的核心实现（slopeFindDistance/levelDecrease/explosionResistance/
 * tickRate 等），仅将 FluidType 约束为 {@link FabricatedFluidType}，使客户端自动渲染注册（
 * {@link qikahome.fabricatedforgefluid.client.FluidRenderHandlerRegistrar}）能识别并调用
 * {@link FabricatedFluidType#initializeClient}。
 */
public abstract class FabricatedFlowingFluid extends BaseFlowingFluid {

    protected FabricatedFlowingFluid(Properties properties) {
        super(properties.toPortingLibProperties());
    }

    @Override
    public FabricatedFluidType getFluidType() {
        return (FabricatedFluidType) super.getFluidType();
    }

    public static class Flowing extends FabricatedFlowingFluid {
        public Flowing(Properties properties) {
            super(properties);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }
    }

    public static class Source extends FabricatedFlowingFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }
    }

    /**
     * 对齐 Forge {@code ForgeFlowingFluid.Properties} 字段与默认值的 builder。
     */
    public static class Properties {
        private final FabricatedFluidType fluidType;
        private final Supplier<? extends Fluid> still;
        private final Supplier<? extends Fluid> flowing;
        @Nullable
        private Supplier<? extends Item> bucket;
        @Nullable
        private Supplier<? extends LiquidBlock> block;
        private int slopeFindDistance = 4;
        private int levelDecreasePerBlock = 1;
        private float explosionResistance = 1;
        private int tickRate = 5;

        public Properties(FabricatedFluidType fluidType, Supplier<? extends Fluid> still,
                Supplier<? extends Fluid> flowing) {
            this.fluidType = fluidType;
            this.still = still;
            this.flowing = flowing;
        }

        public Properties bucket(@Nullable Supplier<? extends Item> bucket) {
            this.bucket = bucket;
            return this;
        }

        public Properties block(@Nullable Supplier<? extends LiquidBlock> block) {
            this.block = block;
            return this;
        }

        public Properties slopeFindDistance(int slopeFindDistance) {
            this.slopeFindDistance = slopeFindDistance;
            return this;
        }

        public Properties levelDecreasePerBlock(int levelDecreasePerBlock) {
            this.levelDecreasePerBlock = levelDecreasePerBlock;
            return this;
        }

        public Properties explosionResistance(float explosionResistance) {
            this.explosionResistance = explosionResistance;
            return this;
        }

        public Properties tickRate(int tickRate) {
            this.tickRate = tickRate;
            return this;
        }

        public BaseFlowingFluid.Properties toPortingLibProperties() {
            return new BaseFlowingFluid.Properties(() -> this.fluidType, this.still, this.flowing)
                    .bucket(this.bucket)
                    .block(this.block)
                    .slopeFindDistance(this.slopeFindDistance)
                    .levelDecreasePerBlock(this.levelDecreasePerBlock)
                    .explosionResistance(this.explosionResistance)
                    .tickRate(this.tickRate);
        }
    }
}