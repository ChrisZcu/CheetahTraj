package util;

import model.Trajectory;

/**
 * Max Heap of Traj.
 * <p>
 * Its arr begins at index 1.
 * Some function are removed to reduce func call.
 */
public final class TrajHeap {
	private final Trajectory[] originTrajList;
	private final Trajectory[] trajList;
	private final int maxSize;

	// arr[topIdx] is empty and for next element
	private static int topIdx;

	public TrajHeap(Trajectory[] originTrajList) {
		this.originTrajList = originTrajList;
		this.maxSize = originTrajList.length;
		topIdx = maxSize + 1;
		this.trajList = new Trajectory[topIdx];
	}

	/**
	 * Refresh heap to origin status.
	 * Then it can be used to calculate for a new delta
	 */
	public void refresh() {
		topIdx = maxSize + 1;

		// refresh heap arr
		System.arraycopy(originTrajList, 0, trajList, 1, maxSize);
		for (Trajectory traj : originTrajList) {
			traj.refreshScore();
		}

		// heapify in O(n) times (heapify trajList)
		for (int i = maxSize; i >= 1; i--) {
			shiftDown(i);
		}
	}

	/**
	 * Suppose that the heap is always non-empty.
	 * i.e. topIdx is always valid.
	 */
	public void deleteMax() {
		/*if (topIdx <= 1) {
			return -1;
		}*/
		--topIdx;
		trajList[1] = trajList[topIdx];
		shiftDown(1);
	}

	/**
	 * Suppose that the heap is always non-empty.
	 * @return id of traj at the heap top
	 */
	public Trajectory getTopTraj() {
		return trajList[1];
	}

	/**
	 * @return is the max heap property hold
	 */
	public boolean orderIsGreat() {
		return trajList[1].getScore() >=
				Math.max(trajList[2].getScore(), trajList[3].getScore());
	}

	public void shiftDown(int index) {
		/*if (index <= 0) {
			return;
		}*/

		while (true) {
			int leftIdx = 2 * index;
			if (leftIdx >= topIdx) {
				// no child
				return;
			}
			int rightIdx = 2 * index + 1;
			int maxIdx;

			if (rightIdx >= topIdx) {
				// no right child
				maxIdx = leftIdx;
			} else {
				// has right child, take bigger one
				maxIdx = trajList[leftIdx].getScore() > trajList[rightIdx].getScore() ?
						leftIdx : rightIdx;
			}

			if (trajList[index].getScore() >= trajList[maxIdx].getScore()) {
				// no need for shiftDown
				return;
			}

			Trajectory tmp = trajList[index];
			trajList[index] = trajList[maxIdx];
			trajList[maxIdx] = tmp;

			// move to next
			index = maxIdx;
		}
	}
}
