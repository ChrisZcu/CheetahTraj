package draw;

import java.util.Random;

public class ThreadStopTest {
    public static void main(String[] args) throws InterruptedException {
        ThreadStopTest manager = new ThreadStopTest();

        manager.startNewTask();
        Thread.sleep(30);
        manager.stopTask();

        manager.startNewTask();
        manager.startNewTask();
        Thread.sleep(30);
        manager.stopTask();

        manager.startNewTask();
    }

    public final DrawWorkerStarter[] workerList = new DrawWorkerStarter[1];
    public final int[] resList = new int[1];
    public int id = 0;
    public int resId = -1;

    private static final class DrawWorkerStarter extends Thread {
        public volatile boolean stop = false;

        private final int id;
        private final ThreadStopTest manager;

        public DrawWorkerStarter(ThreadStopTest manager, int id) {
            this.id = id;
            this.setName("thread-" + id);
            this.manager = manager;
        }

        @Override
        public void run() {
            System.out.println(this.getName() + " start");
            long time = -System.currentTimeMillis();
            Random rand = new Random();
            int res = 0;
            int len = 1000_0000;
            for (int i = 0; i < len; i++) {
                if (stop) {
                    System.out.println(this.getName()
                            + " cancel, progress=" + (100.0 * i / len) + "%");
                    return;
                }
                res += rand.nextInt();
            }
            time += System.currentTimeMillis();
            System.out.println(this.getName() + " finished in " + time);
            manager.saveRes(res, id);
        }
    }

    public void saveRes(int res, int resId) {
        resList[0] = res;
        this.resId = resId;
        System.out.printf("res=%d, resId=%d%n", res, resId);
    }

    public void startNewTask() {
        id ++;
        DrawWorkerStarter worker = new DrawWorkerStarter(this, id);
        workerList[0] = worker;
        worker.start();
    }

    public void stopTask() {
        workerList[0].stop = true;
    }
}