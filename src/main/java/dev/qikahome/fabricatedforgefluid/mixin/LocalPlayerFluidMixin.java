package dev.qikahome.fabricatedforgefluid.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让 {@code LocalPlayer#aiStep} 里的游泳/疾跑判定感知自定义可游泳流体。
 * <p>
 * 玩家的输入/移动逻辑在 {@code aiStep}，其中多次用 {@code isInWater()/isUnderWater()}
 * 决定是否取消疾跑/是否进入游泳；若不扩展为自定义流体 true，会把游泳状态反复拉回，
 * 与 {@code EntityFluidMixin#updateSwimming} 叠加造成每 tick 振荡。
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerFluidMixin extends LivingEntityFluidMixin {

    @Unique
    private boolean fff$inCustomSwimableFluid() {
        FluidType type = this.level().getFluidState(this.blockPosition()).getFluidType();
        return !type.isAir() && this.canSwimInFluidType(type) && type != PortingLibFluids.LAVA_TYPE;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isInWater()Z"))
    private boolean fff$aiStepIsInWater(boolean original) {
        return original || this.fff$inCustomSwimableFluid();
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z"))
    private boolean fff$aiStepIsUnderWater(boolean original) {
        return original || this.fff$inCustomSwimableFluid();
    }
}