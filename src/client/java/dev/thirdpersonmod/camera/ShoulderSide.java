package dev.thirdpersonmod.camera;

public enum ShoulderSide {
    LEFT(-1.0),
    RIGHT(1.0);

    private final double sign;

    ShoulderSide(double sign) {
        this.sign = sign;
    }

    public double sign() {
        return this.sign;
    }

    public ShoulderSide opposite() {
        return this == LEFT ? RIGHT : LEFT;
    }
}
