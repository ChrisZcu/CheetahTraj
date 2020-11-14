package model;

import app.SharedObject;

import java.awt.*;

/**
 * The traj structured block which used to show on one map view.
 * <p>
 * Each map view will have 2 traj blocks,
 * one for background, one for double selected result.
 * All info about the trajectory is here.
 */
public final class TrajBlock {
    private final int mapIdx;         // the idx of the map that this block assigned to.

    private BlockType blockType;
    private Trajectory[] trajList;  // main traj list given to this block (layer main)
    private Trajectory[] trajSltList;       // double select traj list
    private Color mainColor, sltColor;     // two color for two layers
    private int threadNum;
    private int dIdx, rIdx;         // the param for select color

    public TrajBlock(int mapIdx) {
        this.mapIdx = mapIdx;
        this.blockType = BlockType.NONE;
        this.trajList = null;
        this.trajSltList = null;
    }

    public void setNewBlock(BlockType blockType, Trajectory[] trajList,
                            int threadNum, int dIdx, int rIdx) {
        this.blockType = blockType;
        this.trajList = trajList;
        this.threadNum = threadNum;
        this.dIdx = dIdx;
        this.rIdx = rIdx;
    }

    public String getBlockInfoStr(int[] deltaList, double[] rateList) {
        String info = "";
        info += "type=" + blockType;
        // notice that there is no break
        switch (blockType) {
            case VFGS:
                info += " delta=" + deltaList[dIdx];
            case RAND:
                info += " rate=" + rateList[rIdx];
            case FULL:
                // do nothing
        }
        return info;
    }

    public Color getMainColor() {
        return mainColor;
    }

    public void setMainColor(Color mainColor) {
        this.mainColor = mainColor;
    }

    public Color getSltColor() {
        return sltColor;
    }

    public void setSltColor(Color sltColor) {
        this.sltColor = sltColor;
    }

    public int getMapIdx() {
        return mapIdx;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public Trajectory[] getTrajList() {
        return trajList;
    }

    public Trajectory[] getTrajSltList() {
        return trajSltList;
    }

    /**
     * It will be called in {@link SharedObject#setBlockSltAt(int, Trajectory[])}.
     * Hence call that func instead call it directly.
     */
    public void setTrajSltList(Trajectory[] trajSltList) {
        this.trajSltList = trajSltList;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public int getDIdx() {
        return dIdx;
    }

    public int getRIdx() {
        return rIdx;
    }
}
