package select;

import app.TimeProfileSharedObject;
import model.RectRegion;
import model.RegionType;
import model.Trajectory;
import model.TrajectoryMeta;
import org.lwjgl.Sys;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TimeProfileManager {
    ExecutorService threadPool;
    int threadNum;
    Trajectory[] trajFull;
    TrajectoryMeta[] trajMetaFull;
    RectRegion region;

    boolean isMeta = false;

    public TimeProfileManager(int threadNum, Trajectory[] trajFull, RectRegion region) {
        threadPool = new ThreadPoolExecutor(threadNum, threadNum, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        this.threadNum = threadNum;
        this.trajFull = trajFull;
        this.region = region;
        TimeProfileSharedObject.getInstance().trajRes = new Trajectory[threadNum][];
    }

    public TimeProfileManager(int threadNum, TrajectoryMeta[] trajFull, RectRegion region) {
        threadPool = new ThreadPoolExecutor(threadNum, threadNum, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        this.threadNum = threadNum;
        this.trajMetaFull = trajFull;
        this.region = region;
        TimeProfileSharedObject.getInstance().trajMetaRes = new TrajectoryMeta[threadNum][];
        isMeta = true;
    }

    public void startRun() {
        if (!isMeta) {
            startRunNoMeta();
        } else {
            startRunMeta();
        }
    }

    private void startRunNoMeta() {
        int totalLen = trajFull.length;
        int segLen = totalLen / threadNum;

        for (int i = 0; i < threadNum; i++) {
            TimeProfileWorker tw = new TimeProfileWorker(i * segLen, (i + 1) * segLen, trajFull, i, region);
            threadPool.submit(tw);
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    private void startRunMeta() {
        int totalLen = trajMetaFull.length;
        int segLen = totalLen / threadNum;
        for (int i = 0; i < threadNum; i++) {
            TimeProfileWorker tw = new TimeProfileWorker(i * segLen, (i + 1) * segLen, trajMetaFull, i, region);
            threadPool.submit(tw);
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }
}
