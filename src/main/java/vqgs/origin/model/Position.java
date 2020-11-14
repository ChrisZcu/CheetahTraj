package origin.model;

public class Position {
    int x;
    int y;
    int code;

    public Position(double x, double y) {
        this.x = (int) x;
        this.y = (int) y;
        code = hashCode();
    }

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Position)) {
            return false;
        }
        Position p = (Position) obj;
        return (p.x == this.x && p.y == this.y);
    }

    @Override
    public int hashCode() {     // ???
        long ln = (long) (0.5 * (x + y) * (x + y + 1) + y);
        return (int) (ln ^ (ln >>> 32));
    }
}
