package origin.model;

import de.fhpotsdam.unfolding.geo.Location;

import java.util.ArrayList;
import java.util.List;

public class Trajectory {
    public List<Location> points = new ArrayList<>();
    private double score;
    private int trajId;
    private double greedyScore;
    private double cellScore;

    public Trajectory(int trajId) {
        score = 0;
        this.trajId = trajId;
    }

    public Trajectory(double score) {
        this.score = score;
    }

    Trajectory() {
        score = 0;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public int getTrajId() {
        return trajId;
    }

    public List<Location> getPoints() {
        return points;
    }

    public void setGreedyScore(double greedyScore) {
        this.greedyScore = greedyScore;
    }

    public double getGreedyScore() {
        return greedyScore;
    }

    public void setTrajId(int trajId) {
        this.trajId = trajId;
    }

    public void setCellScore(double cellScore) {
        this.cellScore = cellScore;
    }

    public double getCellScore() {
        return cellScore;
    }

    @Override
    public String toString() {
        String res = "";
        for (Location p : this.points) {
            res = res + "," + p.y + "," + p.x;
        }
        return res.substring(1);
    }
}


