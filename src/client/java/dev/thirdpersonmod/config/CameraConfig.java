package dev.thirdpersonmod.config;

import com.google.gson.annotations.SerializedName;
import dev.thirdpersonmod.camera.CameraMath;
import dev.thirdpersonmod.camera.CompositionPreset;
import dev.thirdpersonmod.camera.ShoulderSide;

public final class CameraConfig {
    public boolean enabled = true;
    public double distance = 3.6;
    public double shoulderOffset = 0.75;
    public double verticalOffset = 0.35;
    public double minimumDistance = 0.35;
    public double collisionRadius = 0.14;
    public double collisionSafetyMargin = 0.1;
    @SerializedName(value = "verticalSmoothingSpeed", alternate = "positionSmoothingSpeed")
    public double verticalSmoothingSpeed = 14.0;
    public double collisionInSpeed = 32.0;
    public double collisionOutSpeed = 9.0;
    public double shoulderTransitionSpeed = 11.0;
    public CompositionPreset compositionPreset = CompositionPreset.CINEMATIC_RIGHT_SHOULDER;
    public ShoulderSide defaultShoulder = ShoulderSide.RIGHT;
    public boolean disableWhileRiding = true;
    public boolean disableWhileSleeping = true;
    public boolean disableWhileSwimming = false;
    public boolean disableWhileCrawling = true;
    public boolean disableWhileFallFlying = true;
    public boolean debugCameraOwnership = false;
    public boolean cinematicMotionEnabled = true;
    public double motionStrength = 0.35;
    public boolean dynamicFovEnabled = true;
    public boolean focusWhileAiming = true;
    public boolean correctedCrosshairEnabled = true;

    public CameraConfig copy() {
        CameraConfig copy = new CameraConfig();
        copy.copyFrom(this);
        return copy;
    }

    public void copyFrom(CameraConfig other) {
        this.enabled = other.enabled;
        this.distance = other.distance;
        this.shoulderOffset = other.shoulderOffset;
        this.verticalOffset = other.verticalOffset;
        this.minimumDistance = other.minimumDistance;
        this.collisionRadius = other.collisionRadius;
        this.collisionSafetyMargin = other.collisionSafetyMargin;
        this.verticalSmoothingSpeed = other.verticalSmoothingSpeed;
        this.collisionInSpeed = other.collisionInSpeed;
        this.collisionOutSpeed = other.collisionOutSpeed;
        this.shoulderTransitionSpeed = other.shoulderTransitionSpeed;
        this.compositionPreset = other.compositionPreset;
        this.defaultShoulder = other.defaultShoulder;
        this.disableWhileRiding = other.disableWhileRiding;
        this.disableWhileSleeping = other.disableWhileSleeping;
        this.disableWhileSwimming = other.disableWhileSwimming;
        this.disableWhileCrawling = other.disableWhileCrawling;
        this.disableWhileFallFlying = other.disableWhileFallFlying;
        this.debugCameraOwnership = other.debugCameraOwnership;
        this.cinematicMotionEnabled = other.cinematicMotionEnabled;
        this.motionStrength = other.motionStrength;
        this.dynamicFovEnabled = other.dynamicFovEnabled;
        this.focusWhileAiming = other.focusWhileAiming;
        this.correctedCrosshairEnabled = other.correctedCrosshairEnabled;
    }

    public void validate() {
        this.distance = finiteClamp(this.distance, 1.0, 12.0, 3.6);
        this.shoulderOffset = finiteClamp(this.shoulderOffset, 0.0, 2.0, 0.75);
        this.verticalOffset = finiteClamp(this.verticalOffset, -1.0, 3.0, 0.35);
        this.minimumDistance = finiteClamp(this.minimumDistance, 0.1, 2.0, 0.35);
        this.minimumDistance = Math.min(this.minimumDistance, this.distance);
        this.collisionRadius = finiteClamp(this.collisionRadius, 0.01, 0.5, 0.14);
        this.collisionSafetyMargin = finiteClamp(this.collisionSafetyMargin, 0.0, 0.5, 0.1);
        this.verticalSmoothingSpeed = finiteClamp(this.verticalSmoothingSpeed, 0.0, 60.0, 14.0);
        this.collisionInSpeed = finiteClamp(this.collisionInSpeed, 0.0, 60.0, 32.0);
        this.collisionOutSpeed = finiteClamp(this.collisionOutSpeed, 0.0, 60.0, 9.0);
        this.shoulderTransitionSpeed = finiteClamp(this.shoulderTransitionSpeed, 0.0, 60.0, 11.0);
        this.motionStrength = finiteClamp(this.motionStrength, 0.0, 1.0, 0.35);
        if (this.defaultShoulder == null) {
            this.defaultShoulder = this.compositionPreset != null && !this.compositionPreset.isCustom()
                ? this.compositionPreset.shoulder()
                : ShoulderSide.RIGHT;
        }
        reconcilePreset();
    }

    public void applyPreset(CompositionPreset preset) {
        this.compositionPreset = preset == null ? CompositionPreset.CINEMATIC_RIGHT_SHOULDER : preset;
        if (this.compositionPreset.isCustom()) {
            return;
        }
        this.distance = this.compositionPreset.distance();
        this.shoulderOffset = this.compositionPreset.shoulderOffset();
        this.verticalOffset = this.compositionPreset.verticalOffset();
        this.defaultShoulder = this.compositionPreset.shoulder();
        validate();
    }

    public CompositionPreset reconcilePreset() {
        for (CompositionPreset preset : CompositionPreset.values()) {
            if (preset.matches(this.distance, this.shoulderOffset, this.verticalOffset, this.defaultShoulder)) {
                this.compositionPreset = preset;
                return preset;
            }
        }
        this.compositionPreset = CompositionPreset.CUSTOM;
        return this.compositionPreset;
    }

    private static double finiteClamp(double value, double minimum, double maximum, double fallback) {
        return Double.isFinite(value) ? CameraMath.clamp(value, minimum, maximum) : fallback;
    }
}
