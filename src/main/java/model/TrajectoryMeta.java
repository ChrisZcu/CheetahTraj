package model;

public class TrajectoryMeta {
    private Position[] positions;
    private double score;
    private double metaScore;
    private int trajId;

    /* add */
    private int begin, end;     // the subpart of the this meta traj. [begin, end]
    /* add end */

    private GpsPosition[] gpsPositions;

    public GpsPosition[] getGpsPositions() {
        return gpsPositions;
    }

    public void setGpsPositions(GpsPosition[] gpsPositions) {
        this.gpsPositions = gpsPositions;
    }

    public TrajectoryMeta(int trajId) {
        score = 0;
        this.trajId = trajId;
    }

    public TrajectoryMeta(double score) {
        this.score = score;
    }

    public void setScore(double score) {
        this.score = score;
        metaScore = score;
    }

    public void updateScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public int getTrajId() {
        return trajId;
    }

    public void setPositions(Position[] posi) {
        positions = posi;
    }

    public Position[] getPositions() {
        return positions;
    }

    public void scoreInit() {
        score = metaScore;
    }

    /* add */

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}
