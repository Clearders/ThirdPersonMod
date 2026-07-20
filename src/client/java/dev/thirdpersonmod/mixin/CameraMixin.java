package dev.thirdpersonmod.mixin;

import dev.thirdpersonmod.ShoulderCameraClient;
import dev.thirdpersonmod.camera.ShoulderCameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(
        method = "update(Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("TAIL")
    )
    private void thirdpersonmod$applyShoulderCamera(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        ShoulderCameraController controller = ShoulderCameraClient.controller();
        if (controller != null) {
            controller.onCameraUpdated((Camera) (Object) this, deltaTracker);
        }
    }
}
