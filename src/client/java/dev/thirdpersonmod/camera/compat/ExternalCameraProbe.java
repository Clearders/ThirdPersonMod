package dev.thirdpersonmod.camera.compat;

import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface ExternalCameraProbe {
    boolean isExternalCameraActive(Minecraft minecraft);
}
