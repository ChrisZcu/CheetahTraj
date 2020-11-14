package select;

import app.SharedObject;
import model.RectRegion;
import model.RegionType;
import model.Trajectory;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static select.SelectAlg.*;

/**
 * backend for select algorithm.
 */
public class SelectWorker extends Thread {
    private RegionType regionType;
    private int begin;
    private int end;
    private int optIndex;
    private Trajectory[] trajectory;
    private int trajResId;

    public SelectWorker(RegionType regionType, Trajectory[] trajectory, int begin, int end, int optIndex, int trajResId) {
        this.regionType = regionType;
        this.trajectory = trajectory;
        this.begin = begin;
        this.end = end;
        this.optIndex = optIndex;
        this.trajResId = trajResId;
    }

    @Override
    public void run() {
        Trajectory[] res;
        switch (regionType) {
            case O_D:
                res = getODTraj(begin, end, trajectory, optIndex);
                break;
            case WAY_POINT:
                res = getWayPointTraj(begin, end, trajectory, optIndex);
                break;
            case O_D_W:
                res = getODWTraj(begin, end, trajectory, optIndex);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + regionType);
        }

        System.out.println("into thread" + trajResId + " : res number = " + res.length);
        SharedObject.getInstance().getTrajSelectRes()[trajResId] = res;
    }
}
