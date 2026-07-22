package dev.thirdpersonmod.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CameraMathTest {
    private static final double EPSILON = 1.0E-12;

    @Test
    void zeroSpeedAppliesTargetImmediately() {
        assertEquals(8.0, CameraMath.smooth(2.0, 8.0, 0.0, 0.05), EPSILON);
    }

    @Test
    void positiveSmoothingIsFrameRateIndependent() {
        double oneFrame = CameraMath.smooth(0.0, 10.0, 12.0, 0.05);
        double firstHalf = CameraMath.smooth(0.0, 10.0, 12.0, 0.025);
        double twoHalfFrames = CameraMath.smooth(firstHalf, 10.0, 12.0, 0.025);

        assertEquals(oneFrame, twoHalfFrames, EPSILON);
    }

    @Test
    void smoothingDoesNotOvershoot() {
        double increasing = CameraMath.smooth(2.0, 8.0, 60.0, 0.1);
        double decreasing = CameraMath.smooth(8.0, 2.0, 60.0, 0.1);

        assertTrue(increasing >= 2.0 && increasing <= 8.0);
        assertTrue(decreasing >= 2.0 && decreasing <= 8.0);
    }

    @Test
    void zeroDeltaLeavesCurrentValueUnchanged() {
        assertEquals(2.0, CameraMath.smooth(2.0, 8.0, 0.0, 0.0), EPSILON);
    }
}
