package model;

public class GpsPosition {
    public int x;
    public int y;

    public float lat;
    public float lon;

    public GpsPosition(float x, float y) {
        lat = x;
        lon = y;
    }

    public GpsPosition() {
    }

    public GpsPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        GpsPosition p = (GpsPosition) obj;
//        return (p.lat <= lat + delta && p.lat >= lat - delta)
//                && (p.lon <= lon + delta && p.lon >= lon - delta);
        return (p.x == this.x && p.y == this.y);
    }

    @Override
    public int hashCode() {
        long ln = (long) (0.5 * (x + y) * (x + y + 1) + y);
        return (int) (ln ^ (ln >>> 32));
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
