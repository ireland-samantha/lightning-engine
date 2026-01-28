package ca.samanthaireland.lightning.engine.util;

public record Vector3(long x, long y, long z) {
    private static final Vector3 ZERO = new Vector3(0, 0, 0);

    public static Vector3 zero() {
        return ZERO;
    }

    public static Vector3 of(int x, int y, long z) {
        return new Vector3(x, y, z);
    }
}
