package ca.samanthaireland.lightning.engine.util;

public record Vector2(long x, long y) {
    private static final Vector2 ZERO = new Vector2(0, 0);

    public static Vector2 zero() {
        return ZERO;
    }

    public static Vector2 of(int x, int y) {
        return new Vector2(x, y);
    }
}
