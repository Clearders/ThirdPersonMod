package dev.thirdpersonmod.camera;

import dev.thirdpersonmod.ShoulderCameraClient;
import dev.thirdpersonmod.camera.compat.ExternalCameraDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class CameraOwnership {
    private final ExternalCameraDetector detector = new ExternalCameraDetector();
    private CameraOwnerState lastLoggedState;
    private String lastCameraEntityClass = "";
    private String lastFocusedEntityClass = "";

    public boolean isExternalCamera(Minecraft minecraft, Entity focusedEntity) {
        return this.detector.isExternalCameraActive(minecraft) || focusedEntity != minecraft.player;
    }

    public void logIfChanged(
        boolean debugEnabled,
        Minecraft minecraft,
        Entity focusedEntity,
        CameraOwnerState owner,
        boolean externalDetected
    ) {
        if (!debugEnabled) {
            this.lastLoggedState = null;
            this.lastCameraEntityClass = "";
            this.lastFocusedEntityClass = "";
            return;
        }

        Entity cameraEntity = minecraft.getCameraEntity();
        String cameraClass = className(cameraEntity);
        String focusedClass = className(focusedEntity);
        if (owner == this.lastLoggedState
            && cameraClass.equals(this.lastCameraEntityClass)
            && focusedClass.equals(this.lastFocusedEntityClass)) {
            return;
        }

        ShoulderCameraClient.LOGGER.info(
            "Camera owner changed: {} -> {}; camera entity: {}; camera entity is local player: {}; focused entity: {}; "
                + "owner: {}; Tweakeroo loaded: {}; external camera detected: {}",
            this.lastLoggedState == null ? "<none>" : this.lastLoggedState,
            owner,
            cameraClass,
            cameraEntity != null && cameraEntity == minecraft.player,
            focusedClass,
            owner,
            this.detector.isTweakerooLoaded(),
            externalDetected
        );
        this.lastLoggedState = owner;
        this.lastCameraEntityClass = cameraClass;
        this.lastFocusedEntityClass = focusedClass;
    }

    private static String className(Entity entity) {
        return entity == null ? "<null>" : entity.getClass().getName();
    }
}
