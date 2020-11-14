package model;

public class TrajToQuality {
    TrajectoryMeta trajectoryMeta;
    double quality;

    public TrajToQuality(TrajectoryMeta trajectoryMeta, double quality){
        this.trajectoryMeta = trajectoryMeta;
        this.quality = quality;
    }

    public TrajectoryMeta getTrajectory() {
        return trajectoryMeta;
    }

    public void setTrajectory(TrajectoryMeta trajectoryMeta) {
        this.trajectoryMeta = trajectoryMeta;
    }

    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }
}
