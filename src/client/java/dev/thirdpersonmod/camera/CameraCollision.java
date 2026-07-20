package dev.thirdpersonmod.camera;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CameraCollision {
    private static final double EPSILON = 1.0E-6;

    public double findAvailableDistance(
        Level level,
        Entity focusedEntity,
        Vec3 eyeAnchor,
        Vec3 desiredPosition,
        Vec3 right,
        Vec3 up,
        double nominalDistance,
        double radius,
        double safetyMargin
    ) {
        Vec3 path = desiredPosition.subtract(eyeAnchor);
        double pathLength = path.length();
        if (pathLength < EPSILON || nominalDistance <= 0.0) {
            return 0.0;
        }

        double nearestPathDistance = Double.POSITIVE_INFINITY;
        nearestPathDistance = trace(level, focusedEntity, eyeAnchor, desiredPosition, nearestPathDistance);

        Vec3 rightOffset = right.scale(radius);
        Vec3 upOffset = up.scale(radius);
        nearestPathDistance = traceOffset(level, focusedEntity, eyeAnchor, desiredPosition, rightOffset, upOffset, nearestPathDistance);
        nearestPathDistance = traceOffset(level, focusedEntity, eyeAnchor, desiredPosition, rightOffset.scale(-1.0), upOffset, nearestPathDistance);
        nearestPathDistance = traceOffset(level, focusedEntity, eyeAnchor, desiredPosition, rightOffset, upOffset.scale(-1.0), nearestPathDistance);
        nearestPathDistance = traceOffset(
            level,
            focusedEntity,
            eyeAnchor,
            desiredPosition,
            rightOffset.scale(-1.0),
            upOffset.scale(-1.0),
            nearestPathDistance
        );

        if (!Double.isFinite(nearestPathDistance)) {
            return nominalDistance;
        }

        double safePathDistance = Math.max(0.0, nearestPathDistance - safetyMargin);
        return CameraMath.clamp(nominalDistance * safePathDistance / pathLength, 0.0, nominalDistance);
    }

    private double traceOffset(
        Level level,
        Entity focusedEntity,
        Vec3 eyeAnchor,
        Vec3 desiredPosition,
        Vec3 horizontalOffset,
        Vec3 verticalOffset,
        double nearestPathDistance
    ) {
        Vec3 offset = horizontalOffset.add(verticalOffset);
        return trace(level, focusedEntity, eyeAnchor.add(offset), desiredPosition.add(offset), nearestPathDistance);
    }

    private double trace(
        Level level,
        Entity focusedEntity,
        Vec3 start,
        Vec3 end,
        double nearestPathDistance
    ) {
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, focusedEntity));
        if (hit.getType() == HitResult.Type.MISS) {
            return nearestPathDistance;
        }
        return Math.min(nearestPathDistance, start.distanceTo(hit.getLocation()));
    }
}
