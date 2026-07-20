package dev.thirdpersonmod.camera;

import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;

public final class CameraMath {
    private static final double MAX_DELTA_SECONDS = 0.1;

    private CameraMath() {
    }

    public static double deltaSeconds(DeltaTracker deltaTracker) {
        double seconds = deltaTracker.getRealtimeDeltaTicks() / 20.0;
        if (!Double.isFinite(seconds) || seconds <= 0.0) {
            return 0.0;
        }
        return Math.min(seconds, MAX_DELTA_SECONDS);
    }

    public static double exponentialFactor(double speed, double deltaSeconds) {
        if (speed <= 0.0 || deltaSeconds <= 0.0) {
            return 0.0;
        }
        return 1.0 - Math.exp(-speed * deltaSeconds);
    }

    public static double smooth(double current, double target, double speed, double deltaSeconds) {
        return current + (target - current) * exponentialFactor(speed, deltaSeconds);
    }

    public static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static Vec3 desiredPosition(
        Vec3 eyeAnchor,
        Vec3 forward,
        Vec3 right,
        Vec3 up,
        double distance,
        double shoulderOffset,
        double verticalOffset
    ) {
        return eyeAnchor
            .subtract(forward.scale(distance))
            .add(right.scale(shoulderOffset))
            .add(up.scale(verticalOffset));
    }
}
