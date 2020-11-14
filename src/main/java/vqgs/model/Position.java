package model;

/**
 * Pixel level position on the screen
 */
public final class Position {
    private final int x, y;
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position(double x, double y) {
        this.x = (int) x;
        this.y = (int) y;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    @Override
    public final String toString() {
        return "Position{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        /*if (!(obj instanceof Position)) {
            return false;
        }*/
        Position p = (Position) obj;
        return (p.x == this.x && p.y == this.y);
    }

    @Override
    public final int hashCode() {
        long ln = (long) (0.5 * (x + y) * (x + y + 1) + y);
        return (int) (ln ^ (ln >>> 32));
    }

    /** @deprecated */
    public static int hashCode(int x, int y) {
        long ln = (long) (0.5 * (x + y) * (x + y + 1) + y);
        return (int) (ln ^ (ln >>> 32));
    }
}
