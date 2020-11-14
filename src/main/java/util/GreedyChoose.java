package util;

import model.Trajectory;

/**
 * Max Heap
 */
public class GreedyChoose {
    public int KSIZE;
    public Trajectory[] MaxHeap; //所有的k个轨迹
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

    public boolean GreetOrder() {
        if (index <= 3) {
            if (index == 3)
                return MaxHeap[1].getScore() >= MaxHeap[2].getScore();
            else if (index == 2) {
                return true;
            }

        }
        return MaxHeap[1].getScore() >= Math.max(MaxHeap[2].getScore(), MaxHeap[3].getScore());
    }

    public void addNewTraj(Trajectory traj) {
        MaxHeap[index] = traj;
        orderAdjust(index);
        index++;
    }


    //新增加调整
    private void orderAdjust(int index) {
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
    public void orderAdjust() { //从定向下调整
        int indexs = 1;
        while (true) {
            if (indexs * 2 >= index)
                break;
            else if (indexs * 2 + 1 >= index) {
                if (MaxHeap[indexs].getScore() < MaxHeap[indexs * 2].getScore()) {
                    swag(indexs, indexs * 2);
                }
                break;
            } else {
                if (MaxHeap[indexs].getScore() < Math.max(MaxHeap[indexs * 2].getScore(), MaxHeap[indexs * 2 + 1].getScore())) {
                    int tempId = MaxHeap[indexs * 2].getScore() > MaxHeap[indexs * 2 + 1].getScore() ? indexs * 2 : indexs * 2 + 1;
                    swag(indexs, tempId);
                    indexs = tempId;
                } else break;
            }
        }

    }


    private void swag(int index1, int index2) {
        Trajectory temp = MaxHeap[index1];
        MaxHeap[index1] = MaxHeap[index2];
        MaxHeap[index2] = temp;
    }

    public Trajectory getHeapHead() {
        return MaxHeap[1];
    }

    public Trajectory getMaxScoreTraj() {
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
