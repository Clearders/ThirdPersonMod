package dev.thirdpersonmod.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CameraMotionModelTest {
    private static final double EPSILON = 1.0E-12;

    @Test
    void movementTargetUsesSmoothThresholds() {
        assertEquals(0.0, CameraMotionModel.movementTarget(0.0), EPSILON);
        assertEquals(0.0, CameraMotionModel.movementTarget(CameraMotionModel.SPEED_START), EPSILON);
        assertEquals(0.5, CameraMotionModel.movementTarget(0.19), EPSILON);
        assertEquals(1.0, CameraMotionModel.movementTarget(CameraMotionModel.SPEED_FULL), EPSILON);
        assertEquals(1.0, CameraMotionModel.movementTarget(10.0), EPSILON);
        assertEquals(0.0, CameraMotionModel.movementTarget(Double.NaN), EPSILON);
    }

    @Test
    void subtleMovementExpandsDistanceAndAddsFov() {
        CameraMotionModel.MotionTargets targets = CameraMotionModel.targets(
            3.6, 0.75, 0.35, 0.35, 1.0, 0.0, true, true
        );

        assertEquals(3.95, targets.distance(), EPSILON);
        assertEquals(0.75, targets.shoulderOffset(), EPSILON);
        assertEquals(2.45, targets.fovOffsetDegrees(), EPSILON);
    }

    @Test
    void focusOverridesMovementAndTightensComposition() {
        CameraMotionModel.MotionTargets targets = CameraMotionModel.targets(
            3.6, 0.75, 0.35, 0.35, 1.0, 1.0, true, true
        );

        assertEquals(3.3375, targets.distance(), EPSILON);
        assertEquals(0.658125, targets.shoulderOffset(), EPSILON);
        assertEquals(0.0, targets.fovOffsetDegrees(), EPSILON);
    }

    @Test
    void targetsClampDistanceStrengthAndOptionalFov() {
        CameraMotionModel.MotionTargets high = CameraMotionModel.targets(
            12.0, 2.0, 0.35, 5.0, 1.0, 0.0, true, false
        );
        CameraMotionModel.MotionTargets low = CameraMotionModel.targets(
            1.0, 0.75, 0.8, 1.0, 0.0, 1.0, true, true
        );

        assertEquals(12.0, high.distance(), EPSILON);
        assertEquals(0.0, high.fovOffsetDegrees(), EPSILON);
        assertEquals(0.8, low.distance(), EPSILON);
    }

    @Test
    void disabledMotionReturnsBaseComposition() {
        CameraMotionModel.MotionTargets targets = CameraMotionModel.targets(
            3.6, 0.75, 0.35, 1.0, 1.0, 1.0, false, true
        );

        assertEquals(3.6, targets.distance(), EPSILON);
        assertEquals(0.75, targets.shoulderOffset(), EPSILON);
        assertEquals(0.0, targets.fovOffsetDegrees(), EPSILON);
    }

    @Test
    void configuredSmoothingConvergesWithoutOvershoot() {
        double movement = 0.0;
        for (int frame = 0; frame < 120; frame++) {
            movement = CameraMath.smooth(
                movement,
                1.0,
                CameraMotionModel.MOVEMENT_SMOOTHING_SPEED,
                1.0 / 60.0
            );
            assertTrue(movement >= 0.0 && movement <= 1.0);
        }
        assertTrue(movement > 0.999);
    }
}
