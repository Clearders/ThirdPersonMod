package dev.thirdpersonmod.mixin;

import dev.thirdpersonmod.ShoulderCameraClient;
import dev.thirdpersonmod.camera.CameraPresentationState;
import dev.thirdpersonmod.camera.ShoulderCameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(
        method = "update(Lnet/minecraft/client/DeltaTracker;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;calculateFov(F)F",
            shift = At.Shift.BEFORE
        )
    )
    private void thirdpersonmod$applyShoulderCamera(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        ShoulderCameraController controller = ShoulderCameraClient.controller();
        if (controller != null) {
            controller.onCameraAligned((Camera) (Object) this, deltaTracker);
        }
    }

    @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void thirdpersonmod$finishCameraUpdate(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        ShoulderCameraController controller = ShoulderCameraClient.controller();
        if (controller != null) {
            controller.onCameraUpdateFinished();
        }
    }

    @Inject(method = "calculateFov(F)F", at = @At("RETURN"), cancellable = true)
    private void thirdpersonmod$applyDynamicFov(float partialTicks, CallbackInfoReturnable<Float> callbackInfo) {
        ShoulderCameraController controller = ShoulderCameraClient.controller();
        if (controller == null) {
            return;
        }
        CameraPresentationState presentation = controller.presentationState((Camera) (Object) this);
        if (presentation.active() && presentation.fovOffsetDegrees() > 0.0F) {
            callbackInfo.setReturnValue(Math.min(169.0F, callbackInfo.getReturnValue() + presentation.fovOffsetDegrees()));
        }
    }
}
