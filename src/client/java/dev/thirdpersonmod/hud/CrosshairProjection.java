package dev.thirdpersonmod.hud;

import java.util.Optional;
import net.minecraft.world.phys.Vec3;

public final class CrosshairProjection {
    private static final double NEAR_PLANE = 0.05;

    private CrosshairProjection() {
    }

    public static Optional<ScreenPoint> project(
        Vec3 cameraPosition,
        Vec3 targetPosition,
        Vec3 forward,
        Vec3 right,
        Vec3 up,
        double verticalFovDegrees,
        double framebufferAspectRatio,
        int guiWidth,
        int guiHeight
    ) {
        if (!finite(cameraPosition)
            || !finite(targetPosition)
            || !finite(forward)
            || !finite(right)
            || !finite(up)
            || !Double.isFinite(verticalFovDegrees)
            || verticalFovDegrees <= 0.0
            || verticalFovDegrees >= 180.0
            || !Double.isFinite(framebufferAspectRatio)
            || framebufferAspectRatio <= 0.0
            || guiWidth <= 0
            || guiHeight <= 0) {
            return Optional.empty();
        }

        Vec3 relative = targetPosition.subtract(cameraPosition);
        double depth = relative.dot(forward);
        if (!Double.isFinite(depth) || depth <= NEAR_PLANE) {
            return Optional.empty();
        }

        double tangent = Math.tan(Math.toRadians(verticalFovDegrees) * 0.5);
        if (!Double.isFinite(tangent) || tangent <= 0.0) {
            return Optional.empty();
        }

        double normalizedX = relative.dot(right) / (depth * tangent * framebufferAspectRatio);
        double normalizedY = relative.dot(up) / (depth * tangent);
        if (!Double.isFinite(normalizedX) || !Double.isFinite(normalizedY)) {
            return Optional.empty();
        }

        double x = (normalizedX + 1.0) * 0.5 * guiWidth;
        double y = (1.0 - normalizedY) * 0.5 * guiHeight;
        return Optional.of(new ScreenPoint(x, y));
    }

    public static ScreenPoint clampToViewport(ScreenPoint point, int guiWidth, int guiHeight, int inset) {
        if (guiWidth <= 0 || guiHeight <= 0 || inset < 0) {
            throw new IllegalArgumentException("viewport dimensions and inset must be valid");
        }

        double minimumX = Math.min(inset, guiWidth / 2.0);
        double maximumX = Math.max(guiWidth - inset, guiWidth / 2.0);
        double minimumY = Math.min(inset, guiHeight / 2.0);
        double maximumY = Math.max(guiHeight - inset, guiHeight / 2.0);
        return new ScreenPoint(
            Math.max(minimumX, Math.min(maximumX, point.x())),
            Math.max(minimumY, Math.min(maximumY, point.y()))
        );
    }

    private static boolean finite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    public record ScreenPoint(double x, double y) {
    }
}
