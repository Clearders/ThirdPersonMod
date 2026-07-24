package dev.thirdpersonmod.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import dev.thirdpersonmod.hud.CrosshairProjection;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CrosshairProjectionTest {
    private static final double EPSILON = 1.0E-9;
    private static final Vec3 ORIGIN = Vec3.ZERO;
    private static final Vec3 FORWARD = new Vec3(0.0, 0.0, 1.0);
    private static final Vec3 RIGHT = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    @Test
    void forwardTargetProjectsToScreenCenter() {
        CrosshairProjection.ScreenPoint point = project(new Vec3(0.0, 0.0, 10.0), 90.0, 16.0 / 9.0, 1920, 1080).orElseThrow();

        assertEquals(960.0, point.x(), EPSILON);
        assertEquals(540.0, point.y(), EPSILON);
    }

    @Test
    void horizontalAndVerticalOffsetsUsePerspectiveAndGuiDimensions() {
        CrosshairProjection.ScreenPoint horizontal = project(new Vec3(10.0, 0.0, 10.0), 90.0, 2.0, 200, 100).orElseThrow();
        CrosshairProjection.ScreenPoint vertical = project(new Vec3(0.0, 5.0, 10.0), 90.0, 2.0, 200, 100).orElseThrow();

        assertEquals(150.0, horizontal.x(), EPSILON);
        assertEquals(50.0, horizontal.y(), EPSILON);
        assertEquals(100.0, vertical.x(), EPSILON);
        assertEquals(25.0, vertical.y(), EPSILON);
    }

    @Test
    void aspectRatioAndDynamicFovChangeHorizontalProjection() {
        CrosshairProjection.ScreenPoint wide = project(new Vec3(5.0, 0.0, 10.0), 90.0, 2.0, 200, 100).orElseThrow();
        CrosshairProjection.ScreenPoint narrow = project(new Vec3(5.0, 0.0, 10.0), 90.0, 1.0, 100, 100).orElseThrow();
        CrosshairProjection.ScreenPoint lowerFov = project(new Vec3(5.0, 0.0, 10.0), 60.0, 2.0, 200, 100).orElseThrow();

        assertEquals(125.0, wide.x(), EPSILON);
        assertEquals(75.0, narrow.x(), EPSILON);
        assertTrue(lowerFov.x() > wide.x());
    }

    @Test
    void rejectsNearAndBehindTargetsButKeepsOffscreenDirection() {
        assertTrue(project(new Vec3(0.0, 0.0, 0.05), 90.0, 1.0, 100, 100).isEmpty());
        assertTrue(project(new Vec3(0.0, 0.0, -10.0), 90.0, 1.0, 100, 100).isEmpty());
        assertTrue(project(new Vec3(30.0, 0.0, 10.0), 90.0, 1.0, 100, 100).orElseThrow().x() > 100.0);
    }

    @Test
    void clampsOffscreenPointsInsideCrosshairSafeArea() {
        CrosshairProjection.ScreenPoint clamped = CrosshairProjection.clampToViewport(
            new CrosshairProjection.ScreenPoint(300.0, -50.0),
            200,
            100,
            8
        );

        assertEquals(192.0, clamped.x(), EPSILON);
        assertEquals(8.0, clamped.y(), EPSILON);
    }

    @Test
    void rejectsInvalidInputs() {
        assertTrue(CrosshairProjection.project(
            ORIGIN,
            new Vec3(Double.NaN, 0.0, 10.0),
            FORWARD,
            RIGHT,
            UP,
            90.0,
            1.0,
            100,
            100
        ).isEmpty());
        assertTrue(project(new Vec3(0.0, 0.0, 10.0), 180.0, 1.0, 100, 100).isEmpty());
        assertTrue(project(new Vec3(0.0, 0.0, 10.0), 90.0, 0.0, 100, 100).isEmpty());
    }

    private static Optional<CrosshairProjection.ScreenPoint> project(
        Vec3 target,
        double fov,
        double aspect,
        int width,
        int height
    ) {
        return CrosshairProjection.project(ORIGIN, target, FORWARD, RIGHT, UP, fov, aspect, width, height);
    }
}
