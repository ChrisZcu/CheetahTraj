package vqgs.model;

import de.fhpotsdam.unfolding.geo.Location;

import java.awt.*;

/**
 * Class for trajectories on the map.
 * The field {@link #score} may have diff meaning
 * in diff situations.
 *
 * @see Location
 */
public final class Trajectory {
    private final int tid;
    private int score;              // used in vfgs cal and color cal and show
    private int originScore;        // origin score. the # of positions on it
    private Location[] locations;   // origin geo data
    private Position[] positions;   // corresponding to locations
    private Color[][] colorMatrix;  // color matrix: delta X rate

    public Trajectory(int tid) {
        this.tid = tid;
    }

    public int getTid() {
        return tid;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void refreshScore() {
        this.score = this.originScore;
    }

    public int getOriginScore() {
        return originScore;
    }

    public void setOriginScore(int originScore) {
        this.originScore = originScore;
    }

    public Location[] getLocations() {
        return locations;
    }

    public void setLocations(Location[] locations) {
        this.locations = locations;
    }

    public Position[] getPositions() {
        return positions;
    }

    public void setPositions(Position[] positions) {
        this.positions = positions;
    }

    public Color[][] getColorMatrix() {
        return colorMatrix;
    }

    public void setColorMatrix(Color[][] colorMatrix) {
        this.colorMatrix = colorMatrix;
    }

    @Override
    public String toString() {
        return "Traj{" +
                "tid=" + tid +
                ", s=" + score +
                ", os=" + originScore +
                '}';
    }
}


