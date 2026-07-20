package dev.thirdpersonmod.camera.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class ExternalCameraDetector {
    private final ExternalCameraProbe[] probes = {new CameraEntityProbe()};
    private final boolean tweakerooLoaded = FabricLoader.getInstance().isModLoaded("tweakeroo");

    public boolean isExternalCameraActive(Minecraft minecraft) {
        for (ExternalCameraProbe probe : this.probes) {
            if (probe.isExternalCameraActive(minecraft)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTweakerooLoaded() {
        return this.tweakerooLoaded;
    }
}
