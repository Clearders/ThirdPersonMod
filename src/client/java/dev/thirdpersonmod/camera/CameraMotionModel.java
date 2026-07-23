package dev.thirdpersonmod.camera;

final class CameraMotionModel {
    static final double SPEED_START = 0.08;
    static final double SPEED_FULL = 0.30;
    static final double MOVEMENT_SMOOTHING_SPEED = 5.0;
    static final double FOCUS_IN_SMOOTHING_SPEED = 12.0;
    static final double FOCUS_OUT_SMOOTHING_SPEED = 8.0;

    private static final double MAX_DISTANCE_BOOST = 1.0;
    private static final double MAX_FOCUS_PULL_IN = 0.75;
    private static final double MAX_FOCUS_CENTERING = 0.35;
    private static final double MAX_FOV_BOOST_DEGREES = 7.0;
    private static final double MAX_EFFECTIVE_DISTANCE = 12.0;

    private CameraMotionModel() {
    }

    static double movementTarget(double speed) {
        if (!Double.isFinite(speed)) {
            return 0.0;
        }
        double normalized = CameraMath.clamp((speed - SPEED_START) / (SPEED_FULL - SPEED_START), 0.0, 1.0);
        return normalized * normalized * (3.0 - 2.0 * normalized);
    }

    static MotionTargets targets(
        double baseDistance,
        double baseShoulderOffset,
        double minimumDistance,
        double strength,
        double movement,
        double focus,
        boolean cinematicMotionEnabled,
        boolean dynamicFovEnabled
    ) {
        if (!cinematicMotionEnabled) {
            return new MotionTargets(
                CameraMath.clamp(baseDistance, minimumDistance, MAX_EFFECTIVE_DISTANCE),
                baseShoulderOffset,
                0.0
            );
        }

        double safeStrength = finiteClamp(strength, 0.0, 1.0, 0.35);
        double safeMovement = finiteClamp(movement, 0.0, 1.0, 0.0);
        double safeFocus = finiteClamp(focus, 0.0, 1.0, 0.0);
        double unfocusedMovement = safeMovement * (1.0 - safeFocus);

        double effectiveDistance = baseDistance + safeStrength * (
            MAX_DISTANCE_BOOST * unfocusedMovement - MAX_FOCUS_PULL_IN * safeFocus
        );
        effectiveDistance = CameraMath.clamp(effectiveDistance, minimumDistance, MAX_EFFECTIVE_DISTANCE);

        double effectiveShoulderOffset = baseShoulderOffset * (
            1.0 - MAX_FOCUS_CENTERING * safeStrength * safeFocus
        );
        double fovOffsetDegrees = dynamicFovEnabled
            ? MAX_FOV_BOOST_DEGREES * safeStrength * unfocusedMovement
            : 0.0;

        return new MotionTargets(effectiveDistance, effectiveShoulderOffset, fovOffsetDegrees);
    }

    private static double finiteClamp(double value, double minimum, double maximum, double fallback) {
        return Double.isFinite(value) ? CameraMath.clamp(value, minimum, maximum) : fallback;
    }

    record MotionTargets(double distance, double shoulderOffset, double fovOffsetDegrees) {
    }
}
