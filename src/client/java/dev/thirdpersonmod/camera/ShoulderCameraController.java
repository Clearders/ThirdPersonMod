package dev.thirdpersonmod.camera;

import dev.thirdpersonmod.ShoulderCameraClient;
import dev.thirdpersonmod.config.CameraConfig;
import dev.thirdpersonmod.config.ConfigManager;
import dev.thirdpersonmod.mixin.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public final class ShoulderCameraController {
    private final ConfigManager configManager;
    private final ShoulderCameraState state;
    private final CameraOwnership ownership = new CameraOwnership();
    private final CameraCollision collision = new CameraCollision();

    private ClientLevel lastLevel;
    private LocalPlayer lastPlayer;

    public ShoulderCameraController(ConfigManager configManager) {
        this.configManager = configManager;
        this.state = new ShoulderCameraState(configManager.get().defaultShoulder);
    }

    public void toggleEnabled() {
        boolean enabled = !this.configManager.get().enabled;
        this.configManager.setEnabled(enabled);
        if (!enabled) {
            this.state.leavePlayer(CameraOwnerState.INACTIVE);
        }
        ShoulderCameraClient.LOGGER.info("Cinematic shoulder camera {}", enabled ? "enabled" : "disabled");
    }

    public void toggleShoulder() {
        this.state.toggleShoulder();
        this.configManager.setDefaultShoulder(this.state.shoulder());
        ShoulderCameraClient.LOGGER.info("Shoulder camera side: {}", this.state.shoulder());
    }

    public void applyConfiguration() {
        CameraConfig config = this.configManager.get();
        this.state.shoulder(config.defaultShoulder);
        if (!config.enabled) {
            this.state.leavePlayer(CameraOwnerState.INACTIVE);
        }
    }

    public void onCameraUpdated(Camera camera, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        CameraConfig config = this.configManager.get();
        Entity focusedEntity = camera.entity();

        boolean lifecycleChanged = minecraft.level != this.lastLevel || minecraft.player != this.lastPlayer;
        if (lifecycleChanged) {
            this.state.resetForLifecycleChange();
            this.lastLevel = minecraft.level;
            this.lastPlayer = minecraft.player;
        }

        if (minecraft.level == null || minecraft.player == null) {
            transitionAway(CameraOwnerState.INACTIVE);
            this.ownership.logIfChanged(config.debugCameraOwnership, minecraft, focusedEntity, this.state.owner(), false);
            return;
        }

        // Camera identity is checked before calculating a player anchor or touching collision state.
        boolean externalCamera = this.ownership.isExternalCamera(minecraft, focusedEntity);
        if (externalCamera) {
            transitionAway(CameraOwnerState.EXTERNAL);
            this.ownership.logIfChanged(config.debugCameraOwnership, minecraft, focusedEntity, this.state.owner(), true);
            return;
        }

        if (!shouldApply(minecraft, minecraft.player, config)) {
            transitionAway(CameraOwnerState.INACTIVE);
            this.ownership.logIfChanged(config.debugCameraOwnership, minecraft, focusedEntity, this.state.owner(), false);
            return;
        }

        float partialTicks = camera.getCameraEntityPartialTicks(deltaTracker);
        Vec3 eyeAnchor = minecraft.player.getEyePosition(partialTicks);
        Vec3 forward = new Vec3(camera.forwardVector());
        Vec3 right = new Vec3(camera.leftVector()).scale(-1.0);
        Vec3 up = new Vec3(camera.upVector());

        if (!this.state.initialized() || this.state.owner() != CameraOwnerState.PLAYER || lifecycleChanged) {
            double initialDistance = initialDistance(camera.position(), eyeAnchor, forward, config.distance);
            this.state.enterPlayer(initialDistance);
        }

        double deltaSeconds = CameraMath.deltaSeconds(deltaTracker);
        double targetShoulder = config.shoulderOffset * this.state.shoulder().sign();
        this.state.currentShoulderOffset(
            CameraMath.smooth(
                this.state.currentShoulderOffset(),
                targetShoulder,
                config.shoulderTransitionSpeed,
                deltaSeconds
            )
        );
        this.state.currentVerticalOffset(
            CameraMath.smooth(
                this.state.currentVerticalOffset(),
                config.verticalOffset,
                config.positionSmoothingSpeed,
                deltaSeconds
            )
        );

        Vec3 fullComposition = CameraMath.desiredPosition(
            eyeAnchor,
            forward,
            right,
            up,
            config.distance,
            this.state.currentShoulderOffset(),
            this.state.currentVerticalOffset()
        );
        double collisionLimit = this.collision.findAvailableDistance(
            minecraft.level,
            focusedEntity,
            eyeAnchor,
            fullComposition,
            right,
            up,
            config.distance,
            config.collisionRadius,
            config.collisionSafetyMargin
        );

        double targetDistance = collisionTarget(collisionLimit, config.minimumDistance);
        double distanceSpeed = targetDistance < this.state.currentDistance()
            ? config.collisionInSpeed
            : config.collisionOutSpeed;
        double smoothedDistance = CameraMath.smooth(
            this.state.currentDistance(),
            targetDistance,
            distanceSpeed,
            deltaSeconds
        );
        // The collision limit is a hard cap: smoothing must never carry the camera through a wall.
        this.state.currentDistance(Math.min(smoothedDistance, collisionLimit));

        Vec3 finalPosition = adaptiveComposition(eyeAnchor, forward, right, up, config);

        // Recheck the degraded composition because reducing the lateral offset changes the swept path.
        double finalCollisionLimit = this.collision.findAvailableDistance(
            minecraft.level,
            focusedEntity,
            eyeAnchor,
            finalPosition,
            right,
            up,
            this.state.currentDistance(),
            config.collisionRadius,
            config.collisionSafetyMargin
        );
        if (finalCollisionLimit < this.state.currentDistance()) {
            this.state.currentDistance(finalCollisionLimit);
            finalPosition = adaptiveComposition(eyeAnchor, forward, right, up, config);
        }

        ((CameraAccessor) camera).thirdpersonmod$setPosition(finalPosition);
        this.ownership.logIfChanged(config.debugCameraOwnership, minecraft, focusedEntity, this.state.owner(), false);
    }

    private Vec3 adaptiveComposition(
        Vec3 eyeAnchor,
        Vec3 forward,
        Vec3 right,
        Vec3 up,
        CameraConfig config
    ) {
        double distanceRatio = CameraMath.clamp(this.state.currentDistance() / config.distance, 0.0, 1.0);
        double adaptiveShoulder = this.state.currentShoulderOffset() * distanceRatio;
        return CameraMath.desiredPosition(
            eyeAnchor,
            forward,
            right,
            up,
            this.state.currentDistance(),
            adaptiveShoulder,
            this.state.currentVerticalOffset()
        );
    }

    private void transitionAway(CameraOwnerState owner) {
        if (this.state.owner() != owner || this.state.initialized()) {
            this.state.leavePlayer(owner);
        }
    }

    private static double initialDistance(Vec3 vanillaPosition, Vec3 eyeAnchor, Vec3 forward, double configuredDistance) {
        double backwardsProjection = -vanillaPosition.subtract(eyeAnchor).dot(forward);
        return CameraMath.clamp(backwardsProjection, 0.0, configuredDistance);
    }

    private static double collisionTarget(double collisionLimit, double minimumDistance) {
        // If a wall is closer than the configured minimum, wall safety wins over composition.
        return collisionLimit < minimumDistance ? collisionLimit : Math.max(minimumDistance, collisionLimit);
    }

    private static boolean shouldApply(Minecraft minecraft, LocalPlayer player, CameraConfig config) {
        if (!config.enabled
            || minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK
            || player.isDeadOrDying()) {
            return false;
        }
        if (config.disableWhileSleeping && player.isSleeping()) {
            return false;
        }
        if (config.disableWhileRiding && player.isPassenger()) {
            return false;
        }
        if (config.disableWhileSwimming && player.isSwimming()) {
            return false;
        }
        if (config.disableWhileCrawling && player.getPose() == Pose.SWIMMING && !player.isSwimming()) {
            return false;
        }
        return !config.disableWhileFallFlying || !player.isFallFlying();
    }
}
