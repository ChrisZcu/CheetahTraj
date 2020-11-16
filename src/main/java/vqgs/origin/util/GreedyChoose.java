package vqgs.origin.util;

import vqgs.origin.model.Trajectory;

/**
 * Max Heap
 */
public class GreedyChoose {
    public static int KSIZE;
    public static Trajectory[] MaxHeap; //所有的k个轨迹
    public static int index = 1; //当前的末尾index,下一个index

    public GreedyChoose(int KSize) {
        KSIZE = KSize;
        MaxHeap = new Trajectory[KSize + 1];

        double MAXVALUE = Double.MAX_VALUE;
        MaxHeap[0] = new Trajectory(MAXVALUE);
        index = 1;
    }

    public void clean() {
        index = 1;
    }

    static boolean GreetOrder() {
        return MaxHeap[1].getScore() >= Math.max(MaxHeap[2].getScore(), MaxHeap[3].getScore());
    }

    static void addNewTraj(Trajectory traj) {
        MaxHeap[index] = traj;
        orderAdjust(index);
        index++;
    }


    //新增加调整
    private static void orderAdjust(int index) {
        while (true) {
            if (MaxHeap[index].getScore() > MaxHeap[index / 2].getScore()) { //父节点
                swag(index, index / 2);
                index = index / 2;
            } else {
                break;
            }
        }
    }

    //删除调整
    void orderAdjust() { //从定向下调整
        int index = 1;
        while (true) {
            if (index * 2 >= this.index)
                break;
            else if (index * 2 + 1 >= this.index) {
                if (MaxHeap[index].getScore() < MaxHeap[index * 2].getScore()) {
                    swag(index, index * 2);
                }
                break;
            } else {
                if (MaxHeap[index].getScore() < Math.max(MaxHeap[index * 2].getScore(), MaxHeap[index * 2 + 1].getScore())) {
                    int tempId = MaxHeap[index * 2].getScore() > MaxHeap[index * 2 + 1].getScore() ? index * 2 : index * 2 + 1;
                    swag(index, tempId);
                    index = tempId;
                } else break;
            }
        }

    }


    private static void swag(int index1, int index2) {
        Trajectory temp = MaxHeap[index1];
        MaxHeap[index1] = MaxHeap[index2];
        MaxHeap[index2] = temp;
    }

    static Trajectory getHeapHead() {
        return MaxHeap[1];
    }

    Trajectory getMaxScoreTraj() {
        index--;
        Trajectory temp = MaxHeap[1];
        MaxHeap[1] = MaxHeap[index];
        orderAdjust();
        return temp;
    }


    Trajectory[] getMaxHeap() {
        return MaxHeap;
    }

    public int[] getTrajIdSet() {
        int[] TrajIdSet = new int[KSIZE];
        for (int i = 0; i < KSIZE; i++) {
            TrajIdSet[i] = MaxHeap[i + 1].getTrajId();
        }
        return TrajIdSet;
    }

}
