package qikahome.fabricatedforgefluid.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import qikahome.fabricatedforgefluid.fluids.FabricatedFluidType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge {@code LivingEntity.java} 流体补丁的完整移植（LGPL-2.1，源自 MinecraftForge 1.20.1）。
 * <p>
 * 依赖 {@link EntityFluidMixin} 提供的 FluidType 高度表/判定：
 * <ul>
 * <li>跳跃段（aiStep）无需注入——Entity 的 isInLava/getFluidHeight/isInWater 已重定向到
 * FluidType。</li>
 * <li>travel 游泳条件与游泳速度（SWIM_SPEED）在此注入。</li>
 * <li>呼吸判定（baseTick）扩展为 canDrown 的流体。</li>
 * </ul>
 * 注：重力 1.21.1 起为原版属性 {@code Attributes.GRAVITY}（1.20.1 用 Porting Lib attributes 的 ENTITY_GRAVITY）。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFluidMixin extends EntityFluidMixin {

    @Shadow
    protected abstract boolean isAffectedByFluids();

    @Shadow
    public abstract boolean canStandOnFluid(FluidState fluidState);

    @Shadow
    public abstract AttributeInstance getAttribute(Holder<Attribute> attribute);

    @Unique
    private LivingEntity self() {
        return (LivingEntity) (Object) this;
    }

    /*
     * ===================== FluidType API（Forge IForgeLivingEntity）
     * =====================
     */

    @Unique
    public boolean canDrownInFluidType(FluidType type) {
        final LivingEntity self = (LivingEntity) (Object) this;
        return type != null && !self.isDeadOrDying() && type.canDrownIn(self);
    }

    @Unique
    public boolean moveInFluid(FluidState state, Vec3 movementVector, double gravity) {
        return state.getFluidType().move(state, (LivingEntity) (Object) this, movementVector, gravity);
    }

    /* ===================== travel / baseTick 精细注入 ===================== */
    @Unique
    private Vec3 travelVector;

    @Inject(method = "travel", at = @At(value = "HEAD"))
    private void fff$storeTravelVector(Vec3 travelVector, CallbackInfo cb) {
        this.travelVector = travelVector;
    }

    /**
     * Forge: 把 travel 内 isInWater() 判定扩展为"原版水 或 实体所在格流体可游泳"，
     * 使自定义流体在（move 返回 false 时）也能走原版水游泳段。
     */
    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z"))
    private boolean fff$travelIsInWater(boolean original) {
        return original || this.canSwimInFluidType(this.level().getFluidState(this.blockPosition()).getFluidType());
    }

    /**
     * Forge: 游泳分支条件从 isInWater() 扩展为"水或自定义水类流体（非熔岩）"。
     */
    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isControlledByLocalInstance()Z"))
    private boolean fff$travelWaterCheck(boolean original) {
        if (!original)
            return false;
        FluidState state = this.level().getFluidState(this.blockPosition());
        if (!this.isAffectedByFluids() || this.canStandOnFluid(state))
            return original;
        FluidType type = state.getFluidType();
        // 1.21.1 起实体重力为原版属性 Attributes.GRAVITY（取代 1.20.1 的 Porting Lib ENTITY_GRAVITY）
        AttributeInstance gravity = this.getAttribute(Attributes.GRAVITY);
        double g = gravity != null ? gravity.getValue() : 0.08D;
        return !type.move(state, self(), travelVector, g);
    }

    @Unique
    private final FabricatedFluidType[] cachedType = new FabricatedFluidType[1];

    @Inject(method = "aiStep", at = @At(value = "HEAD"))
    private void fff$cleanCachedType(CallbackInfo cb) {
        cachedType[0] = null;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInLava()Z"))
    private boolean fff$isInCustomFluid$lava(boolean original) {
        if (cachedType[0] == null) {
            this.fluidHeight.object2DoubleEntrySet().stream().max((a, b) -> {
                var d = a.getDoubleValue() - b.getDoubleValue();
                return d > 0 ? 1 : d < 0 ? -1 : 0;
            }).map(e -> FabricatedFluidType.tagToTypeMap.get(e.getKey())).ifPresent(t -> cachedType[0] = t);
        }
        return original && cachedType[0] == null;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFluidHeight(Lnet/minecraft/tags/TagKey;)D"))
    private double fff$getCustomFluidHeight(double original) {
        double out = original;
        if (cachedType[0] != null)
            out = this.fluidHeight.getDouble(cachedType[0].tag);
        return out;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z"))
    private boolean fff$isInCustomFluid$water(boolean original) {
        return original || cachedType[0] != null;
    }

    @ModifyArg(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;jumpInLiquid(Lnet/minecraft/tags/TagKey;)V"))
    private TagKey<Fluid> fff$jumpInCustomFluid(TagKey<Fluid> origin) {
        return cachedType[0] != null ? cachedType[0].tag : origin;
    }

    /**
     * Forge: 呼吸判定扩展为"眼睛在水中或 canDrown 的流体中"（baseTick）。
     */
    @ModifyExpressionValue(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean fff$baseTickBreath(boolean original) {
        return original || this.fluidOnEyes.stream()
                .anyMatch(tag -> this.canDrownInFluidType(FabricatedFluidType.tagToTypeMap.get(tag)));
    }
}
