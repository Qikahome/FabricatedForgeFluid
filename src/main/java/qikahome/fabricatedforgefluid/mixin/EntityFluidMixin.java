package qikahome.fabricatedforgefluid.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import qikahome.fabricatedforgefluid.fluids.FabricatedFluidType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Forge {@code Entity.java} 流体补丁的移植（LGPL-2.1，源自 MinecraftForge 1.20.1）。
 * <p>
 * 以 {@code FabricatedFluidType.tagToTypeMap}（TagKey → FabricatedFluidType）为桥，
 * 基于原版 {@code fluidHeight[tag]} 高度表与 {@code fluidOnEyes} 集合，按每个流体自身的
 * FluidType 属性（motionScale/canSwim/canDrown/canExtinguish/fallDistanceModifier）
 * 驱动推动/游泳/溺水/熄灭/落距等行为，与 Forge 端行为一致。
 */
@Mixin(Entity.class)
public abstract class EntityFluidMixin {
    @Shadow
    public Set<TagKey<Fluid>> fluidOnEyes;

    @Shadow
    protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;

    @Shadow
    public float fallDistance;

    @Shadow
    public abstract boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> fluidTag, double motionScale);

    @Shadow
    public abstract Entity getVehicle();

    @Shadow
    public abstract void clearFire();

    @Shadow
    public abstract Level level();

    @Shadow
    public abstract BlockPos blockPosition();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract boolean isSprinting();

    @Unique
    private Entity self() {
        return (Entity) (Object) this;
    }

    /*
     * ===================== FluidType 高度 API（Forge IForgeEntity / Entity）
     * =====================
     */

    @Unique
    public boolean canSwimInFluidType(FluidType type) {
        // 只对 FabricatedFluidType 生效，避免误影响其他 mod 用 Porting Lib 注册的 FluidType
        return type instanceof FabricatedFluidType && type.canSwim(self());
    }

    /*
     * ===================== 核心：按 FluidType 分组的高度/推动计算（Forge 重写）
     * =====================
     */

    /* ===================== 原版方法重写（Forge 补丁对应） ===================== */

    @Unique
    private float cachedFallDistance = 0;

    @Inject(method = "updateInWaterStateAndDoFluidPushing", at = @At("HEAD"))
    public void fff$cacheFallDistance(CallbackInfoReturnable<Boolean> ci) {
        cachedFallDistance = fallDistance;
    }

    @Inject(method = "updateInWaterStateAndDoFluidPushing", at = @At("RETURN"), cancellable = true)
    public void fff$updateAllFluidTypes(CallbackInfoReturnable<Boolean> ci) {
        boolean any = false;
        Boat boat = this.getVehicle() instanceof Boat b && !b.isUnderWater() ? b : null;
        float fallDistanceMutiplier = 1f;
        for (TagKey<Fluid> tag : this.fff$contactedTags()) {
            FabricatedFluidType type = FabricatedFluidType.tagToTypeMap.get(tag);
            if (type == null)
                continue;
            if (boat != null && type.supportsBoating(boat))
                continue;
            if (this.updateFluidHeightAndDoFluidPushing(tag, type.motionScale(this.self()))) {
                any = true;
                // 落距/灭火仅对不在船上的实体生效（对齐 Forge：整段仅在非船时执行）
                if (boat == null) {
                    if (type.canExtinguish(this.self()))
                        this.clearFire();
                    fallDistanceMutiplier = Math.min(fallDistanceMutiplier, type.getFallDistanceModifier(this.self()));
                }
            }
        }
        if (any) {
            // 用 HEAD 缓存的原始值，避免与其他 mod 对 fallDistance 的修改重复累乘
            if (boat == null)
                this.fallDistance = Math.min(this.fallDistance, fallDistanceMutiplier * cachedFallDistance);
            ci.setReturnValue(true);
        }
    }

    /**
     * 单次包围盒扫描，收集本实体实际接触、且能在 {@code tagToTypeMap} 反查到
     * {@link FabricatedFluidType} 的所有流体 tag。只收录真实存在的流体，不会遍历全局注册表。
     */
    @Unique
    private Set<TagKey<Fluid>> fff$contactedTags() {
        final Entity self = this.self();
        AABB aabb = self.getBoundingBox().deflate(0.001D);
        Set<TagKey<Fluid>> tags = new java.util.HashSet<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(aabb.minX); x < Mth.ceil(aabb.maxX); ++x)
            for (int y = Mth.floor(aabb.minY); y < Mth.ceil(aabb.maxY); ++y)
                for (int z = Mth.floor(aabb.minZ); z < Mth.ceil(aabb.maxZ); ++z) {
                    FluidState state = self.level().getFluidState(pos.set(x, y, z));
                    if (state.getFluidType().isAir())
                        continue;
                    for (TagKey<Fluid> tag : state.getTags().toList())
                        if (FabricatedFluidType.tagToTypeMap.containsKey(tag))
                            tags.add(tag);
                }
        return tags;
    }

    @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInWater()Z"))
    private boolean fff$updateSwiming$isInWater(boolean original) {
        return original || this.fluidHeight.object2DoubleEntrySet().stream()
                .anyMatch(entry -> entry.getDoubleValue() > 0
                        && this.canSwimInFluidType(FabricatedFluidType.tagToTypeMap.get(entry.getKey())));
    }

    @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isUnderWater()Z"))
    private boolean fff$updateSwiming$isUnderWater(boolean original) {
        return original || this.fluidOnEyes.stream()
                .anyMatch(tag -> this.canSwimInFluidType(FabricatedFluidType.tagToTypeMap.get(tag)));
    }

    @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean fff$updateSwiming$isSwimableFluid(boolean original) {
        return original || this.canSwimInFluidType(this.level().getFluidState(this.blockPosition()).getFluidType());
    }

    /**
     * 防止游泳时被误判为"爬行"（isVisuallyCrawling = 游泳 && !isInWater），
     * 自定义流体中 isInWater() 为 false 会触发 isMovingSlowly → 输入乘 0.3 变慢。
     */
    @ModifyExpressionValue(method = "isVisuallyCrawling", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInWater()Z"))
    private boolean fff$visuallyCrawlingIsInWater(boolean original) {
        return original || this.canSwimInFluidType(this.level().getFluidState(this.blockPosition()).getFluidType());
    }
}