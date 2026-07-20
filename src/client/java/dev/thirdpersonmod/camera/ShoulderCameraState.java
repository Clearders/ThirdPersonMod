package dev.thirdpersonmod.camera;

public final class ShoulderCameraState {
    private CameraOwnerState owner = CameraOwnerState.INACTIVE;
    private CameraOwnerState previousOwner = CameraOwnerState.INACTIVE;
    private ShoulderSide shoulder;
    private double currentShoulderOffset;
    private double currentDistance;
    private double currentVerticalOffset;
    private boolean initialized;

    public ShoulderCameraState(ShoulderSide shoulder) {
        this.shoulder = shoulder;
    }

    public void enterPlayer(double initialDistance) {
        setOwner(CameraOwnerState.PLAYER);
        this.currentShoulderOffset = 0.0;
        this.currentVerticalOffset = 0.0;
        this.currentDistance = Math.max(0.0, initialDistance);
        this.initialized = true;
    }

    public void leavePlayer(CameraOwnerState newOwner) {
        setOwner(newOwner);
        clearSmoothing();
    }

    public void resetForLifecycleChange() {
        setOwner(CameraOwnerState.INACTIVE);
        clearSmoothing();
    }

    private void setOwner(CameraOwnerState newOwner) {
        if (this.owner != newOwner) {
            this.previousOwner = this.owner;
            this.owner = newOwner;
        }
    }

    private void clearSmoothing() {
        this.currentShoulderOffset = 0.0;
        this.currentDistance = 0.0;
        this.currentVerticalOffset = 0.0;
        this.initialized = false;
    }

    public void toggleShoulder() {
        this.shoulder = this.shoulder.opposite();
    }

    public CameraOwnerState owner() {
        return this.owner;
    }

    public CameraOwnerState previousOwner() {
        return this.previousOwner;
    }

    public ShoulderSide shoulder() {
        return this.shoulder;
    }

    public double currentShoulderOffset() {
        return this.currentShoulderOffset;
    }

    public void currentShoulderOffset(double value) {
        this.currentShoulderOffset = value;
    }

    public double currentDistance() {
        return this.currentDistance;
    }

    public void currentDistance(double value) {
        this.currentDistance = value;
    }

    public double currentVerticalOffset() {
        return this.currentVerticalOffset;
    }

    public void currentVerticalOffset(double value) {
        this.currentVerticalOffset = value;
    }

    public boolean initialized() {
        return this.initialized;
    }
}
