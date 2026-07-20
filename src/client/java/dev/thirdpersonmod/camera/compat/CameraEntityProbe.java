package dev.thirdpersonmod.camera.compat;

import net.minecraft.client.Minecraft;

public final class CameraEntityProbe implements ExternalCameraProbe {
    @Override
    public boolean isExternalCameraActive(Minecraft minecraft) {
        return minecraft.player == null || minecraft.getCameraEntity() != minecraft.player;
    }
}
