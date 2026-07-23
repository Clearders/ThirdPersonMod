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
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;

public final class ShoulderCameraController {
    private static final Set<ItemUseAnimation> FOCUS_ANIMATIONS = EnumSet.of(
        ItemUseAnimation.BOW,
        ItemUseAnimation.CROSSBOW,
        ItemUseAnimation.TRIDENT,
        ItemUseAnimation.SPEAR,
        ItemUseAnimation.BLOCK,
        ItemUseAnimation.SPYGLASS
    );

    private final ConfigManager configManager;
    private final ShoulderCameraState state;
    private final CameraOwnership ownership = new CameraOwnership();
    private final CameraCollision collision = new CameraCollision();

    private ClientLevel lastLevel;
    private LocalPlayer lastPlayer;
    private CameraConfig previewConfig;

    public ShoulderCameraController(ConfigManager configManager) {
        this.configManager = configManager;
        this.state = new ShoulderCameraState(configManager.get().defaultShoulder);
    }

    public boolean toggleEnabled() {
        boolean enabled = !this.configManager.get().enabled;
        this.configManager.setEnabled(enabled);
        if (!enabled) {
            this.state.leavePlayer(CameraOwnerState.INACTIVE);
        }
        ShoulderCameraClient.LOGGER.info("Cinematic shoulder camera {}", enabled ? "enabled" : "disabled");
        return enabled;
    }

    public ShoulderSide toggleShoulder() {
        this.state.toggleShoulder();
        this.configManager.setDefaultShoulder(this.state.shoulder());
        ShoulderCameraClient.LOGGER.info("Shoulder camera side: {}", this.state.shoulder());
        return this.state.shoulder();
    }

    public void applyConfiguration() {
        this.previewConfig = null;
        CameraConfig config = this.configManager.get();
        this.state.shoulder(config.defaultShoulder);
        if (!config.enabled) {
            this.state.leavePlayer(CameraOwnerState.INACTIVE);
        }
    }

    public void previewConfiguration(CameraConfig draft) {
        CameraConfig validated = draft.copy();
        validated.validate();
        this.previewConfig = validated;
        this.state.shoulder(validated.defaultShoulder);
        if (!validated.enabled) {
            this.state.leavePlayer(CameraOwnerState.INACTIVE);
        }
    }

    public void cancelConfigurationPreview() {
        this.previewConfig = null;
        CameraConfig persisted = this.configManager.get();
        this.state.shoulder(persisted.defaultShoulder);
        if (!persisted.enabled) {
            this.state.leavePlayer(CameraOwnerState.INACTIVE);
        }
    }

    public void onCameraAligned(Camera camera, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        CameraConfig config = currentConfig();
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
        updateMotion(playerSpeed(minecraft.player), isFocusActive(minecraft.player, config), config, deltaSeconds);
        CameraMotionModel.MotionTargets motion = CameraMotionModel.targets(
            config.distance,
            config.shoulderOffset,
            config.minimumDistance,
            config.motionStrength,
            this.state.movementBlend(),
            this.state.focusBlend(),
            config.cinematicMotionEnabled,
            config.dynamicFovEnabled
        );
        this.state.fovOffsetDegrees(motion.fovOffsetDegrees());

        double targetShoulder = motion.shoulderOffset() * this.state.shoulder().sign();
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
                config.verticalSmoothingSpeed,
                deltaSeconds
            )
        );

        Vec3 fullComposition = CameraMath.desiredPosition(
            eyeAnchor,
            forward,
            right,
            up,
            motion.distance(),
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
            motion.distance(),
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

        Vec3 finalPosition = adaptiveComposition(eyeAnchor, forward, right, up, config, motion.distance());

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
            finalPosition = adaptiveComposition(eyeAnchor, forward, right, up, config, motion.distance());
        }

        ((CameraAccessor) camera).thirdpersonmod$setPosition(finalPosition);
        this.ownership.logIfChanged(config.debugCameraOwnership, minecraft, focusedEntity, this.state.owner(), false);
    }

    public void onCameraUpdateFinished() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            transitionAway(CameraOwnerState.INACTIVE);
            this.lastLevel = minecraft.level;
            this.lastPlayer = minecraft.player;
        }
    }

    public CameraPresentationState presentationState(Camera camera) {
        Minecraft minecraft = Minecraft.getInstance();
        CameraConfig config = currentConfig();
        Entity focusedEntity = camera.entity();
        boolean active = minecraft.level != null
            && minecraft.player != null
            && this.state.owner() == CameraOwnerState.PLAYER
            && !this.ownership.isExternalCamera(minecraft, focusedEntity)
            && shouldApply(minecraft, minecraft.player, config);
        if (!active) {
            return CameraPresentationState.INACTIVE;
        }
        return new CameraPresentationState(
            true,
            (float) this.state.fovOffsetDegrees(),
            config.correctedCrosshairEnabled
        );
    }

    private Vec3 adaptiveComposition(
        Vec3 eyeAnchor,
        Vec3 forward,
        Vec3 right,
        Vec3 up,
        CameraConfig config,
        double effectiveDistance
    ) {
        double distanceRatio = CameraMath.clamp(this.state.currentDistance() / effectiveDistance, 0.0, 1.0);
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

    private void updateMotion(double playerSpeed, boolean focusActive, CameraConfig config, double deltaSeconds) {
        if (!config.cinematicMotionEnabled) {
            this.state.movementBlend(0.0);
            this.state.focusBlend(0.0);
            this.state.fovOffsetDegrees(0.0);
            return;
        }

        this.state.movementBlend(CameraMath.smooth(
            this.state.movementBlend(),
            CameraMotionModel.movementTarget(playerSpeed),
            CameraMotionModel.MOVEMENT_SMOOTHING_SPEED,
            deltaSeconds
        ));
        double focusSpeed = focusActive
            ? CameraMotionModel.FOCUS_IN_SMOOTHING_SPEED
            : CameraMotionModel.FOCUS_OUT_SMOOTHING_SPEED;
        this.state.focusBlend(CameraMath.smooth(
            this.state.focusBlend(),
            focusActive ? 1.0 : 0.0,
            focusSpeed,
            deltaSeconds
        ));
    }

    private CameraConfig currentConfig() {
        return this.previewConfig != null ? this.previewConfig : this.configManager.get();
    }

    private static double playerSpeed(LocalPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        if (player.isSwimming() || player.isFallFlying()) {
            return movement.length();
        }
        return Math.sqrt(movement.x * movement.x + movement.z * movement.z);
    }

    private static boolean isFocusActive(LocalPlayer player, CameraConfig config) {
        return config.focusWhileAiming
            && player.isUsingItem()
            && FOCUS_ANIMATIONS.contains(player.getUseItem().getUseAnimation());
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
