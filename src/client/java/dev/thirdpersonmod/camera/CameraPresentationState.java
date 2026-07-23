package dev.thirdpersonmod.camera;

public record CameraPresentationState(
    boolean active,
    float fovOffsetDegrees,
    boolean correctedCrosshairEnabled
) {
    public static final CameraPresentationState INACTIVE = new CameraPresentationState(false, 0.0F, false);
}
