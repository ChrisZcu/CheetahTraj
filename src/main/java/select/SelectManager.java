package select;


import app.SharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import draw.TrajDrawManager;
import model.BlockType;
import model.RegionType;
import model.TrajBlock;
import model.Trajectory;
import org.apache.commons.lang3.ArrayUtils;
import util.PSC;

import java.util.concurrent.*;

/**
 * select thread pool manager, return the traj index array.
 */
public class SelectManager {
    private RegionType regionType; // ODW
    private UnfoldingMap[] mapList;
    private TrajBlock[] blockList;

    public SelectManager(RegionType regionType, UnfoldingMap[] mapList, TrajBlock[] blockList) {
        this.regionType = regionType;
        this.mapList = mapList;
        this.blockList = blockList;

        System.out.println(regionType);
    }


    private Trajectory[] startMapCal(TrajBlock trajBlock, int opIndex) {
        if (trajBlock.getBlockType() == BlockType.NONE) {
            return new Trajectory[0];
        }

        int threadNum = trajBlock.getThreadNum();

        // TODO Recreate thread pool? Create once may be a better choice.
        ExecutorService threadPool = new ThreadPoolExecutor(threadNum, threadNum, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        long startTime = System.currentTimeMillis();

        Trajectory[] trajList = trajBlock.getTrajList();
        int totLen = trajList.length;
        int segLen = totLen / threadNum;
        Trajectory[] resShowIndex = {};

        if (segLen < PSC.MULTI_THREAD_BOUND) {
            // use single thread instead of multi thread
            threadNum = 1;
            segLen = totLen;
        }

        for (int i = 0; i < threadNum; i++) {
            SelectWorker sw = new SelectWorker(regionType, trajBlock.getTrajList(), i * segLen, (i + 1) * segLen, opIndex, i);
            threadPool.submit(sw);
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.err.println(e);
        }
        System.out.println("ALL Done");
        for (int i = 0; i < threadNum; i++) {
            resShowIndex = ArrayUtils.addAll(resShowIndex, SharedObject.getInstance().getTrajSelectRes()[i]);
        }
        System.out.println(trajBlock.getBlockType() + " time: " + (System.currentTimeMillis() - startTime)
                + " select size: " + resShowIndex.length);
        return resShowIndex;
    }

    public void startRun() {
        TrajDrawManager tdm = SharedObject.getInstance().getTrajDrawManager();
        for (int i = 0; i < 4; i++) {
            TrajBlock trajBlock = blockList[i];
            SharedObject.getInstance().setTrajSelectRes(new Trajectory[trajBlock.getThreadNum()][]);
            Trajectory[] trajAry = startMapCal(trajBlock, i);
            trajBlock.setTrajSltList(trajAry);

            if (trajBlock.getMainColor() != PSC.GRAY) {
                // need to repaint
                trajBlock.setMainColor(PSC.GRAY);
                tdm.cleanImgFor(i, TrajDrawManager.MAIN);
                tdm.startNewRenderTaskFor(i, TrajDrawManager.MAIN);
            }

            tdm.cleanImgFor(i, TrajDrawManager.SLT);
            tdm.startNewRenderTaskFor(i, TrajDrawManager.SLT);
        }
    }
}
