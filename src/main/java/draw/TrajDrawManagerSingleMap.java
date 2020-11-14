package draw;

import app.TimeProfileSharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import model.Trajectory;
import model.TrajectoryMeta;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.Arrays;
import java.util.concurrent.*;

public class TrajDrawManagerSingleMap {

    private Trajectory[] trajTotal;
    private TrajectoryMeta[] trajMetaTotal;
    private int threadNum;
    private PApplet app;
    private UnfoldingMap map;
    private boolean isMeta = false;

    public TrajDrawManagerSingleMap(Trajectory[] trajTotal, int threadNum, PApplet app, UnfoldingMap map) {
        this.trajTotal = trajTotal;
        this.threadNum = threadNum;
        this.app = app;
        this.map = map;

        TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[threadNum];
    }

    public TrajDrawManagerSingleMap(TrajectoryMeta[] trajMetaTotal, int threadNum, PApplet app, UnfoldingMap map) {
        this.trajMetaTotal = trajMetaTotal;
        this.threadNum = threadNum;
        this.app = app;
        this.map = map;
        isMeta = true;

        TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[threadNum];
    }

    TrajDrawWorkerSingleMap[] workerList;

    public void startDraw() {
        if (!isMeta)
            workerList = startDrawWorker();
        else {
            workerList = startDrawWorkerMeta();
        }
    }

    private TrajDrawWorkerSingleMap[] startDrawWorker() {
        ExecutorService drawPool = new ThreadPoolExecutor(threadNum, threadNum,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        int segLen = trajTotal.length / threadNum;

        TrajDrawWorkerSingleMap[] workList = new TrajDrawWorkerSingleMap[threadNum];
        for (int i = 0; i < threadNum; i++) {
            workList[i] = new TrajDrawWorkerSingleMap(app.createGraphics(1000, 800), map,
                    i * segLen, (i + 1) * segLen, trajTotal);
            drawPool.submit(workList[i]);
        }
        return workList;
    }

    private TrajDrawWorkerSingleMap[] startDrawWorkerMeta() {
        ExecutorService drawPool = new ThreadPoolExecutor(threadNum, threadNum,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        int segLen = trajMetaTotal.length / threadNum;

        TrajDrawWorkerSingleMap[] workList = new TrajDrawWorkerSingleMap[threadNum];
        for (int i = 0; i < threadNum; i++) {
            workList[i] = new TrajDrawWorkerSingleMap(app.createGraphics(1000, 800), map,
                    i * segLen, (i + 1) * segLen, trajMetaTotal);
            drawPool.submit(workList[i]);
        }
        return workList;
    }

    public void interrupt() {
        TimeProfileSharedObject.getInstance().trajImageMtx = new PGraphics[0];
        for (TrajDrawWorkerSingleMap worker : workerList) {
            if (worker == null) {
                return;
            }
            worker.stop = true;
        }
        Arrays.fill(workerList, null);
    }
}
