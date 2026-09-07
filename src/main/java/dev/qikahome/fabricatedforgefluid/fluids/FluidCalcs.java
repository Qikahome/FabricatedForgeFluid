package dev.qikahome.fabricatedforgefluid.fluids;

import net.minecraft.world.phys.Vec3;

/**
 * 单个 {@code FluidType} 的高度/方向/计数聚合（源自 Forge Entity 的 {@code FluidCalcs}）。
 * <p>
 * 独立成顶层类：若置于 mixin 包内，会被 Mixin 视为不可直接引用的 mixin 内部类而拒绝加载。
 */
public final class FluidCalcs {
    public double height = 0.0D;
    public Vec3 direction = Vec3.ZERO;
    public int count = 0;

    public double height(double height) {
        return (this.height = this.height >= height ? this.height : height);
    }
}
