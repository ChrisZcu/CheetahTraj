package model;

public final class TrajToSubpart {
    private final int trajId;     // the id of the ORIGIN traj
    private final int beginPosIdx, endPosIdx;     // the sub part we selected
    public double quality = 0;

    public TrajToSubpart(int trajId, int beginPosIdx, int endPosIdx) {
        this.trajId = trajId;
        this.beginPosIdx = beginPosIdx;
        this.endPosIdx = endPosIdx;
    }

    public int getTrajId() {
        return trajId;
    }

    public int getBeginPosIdx() {
        return beginPosIdx;
    }

    public int getEndPosIdx() {
        return endPosIdx;
    }

    @Override
    public String toString() {
        return "TrajToSubpart{" +
                "trajId=" + trajId +
                ", beginPosIdx=" + beginPosIdx +
                ", endPosIdx=" + endPosIdx +
                '}';
    }

    public static String serialize(TrajToSubpart tts) {
        return tts.trajId + "," + tts.beginPosIdx + "," + tts.endPosIdx + "," + tts.quality;
    }

    public static TrajToSubpart antiSerialize(String str) {
        String[] strArr = str.split(",");
        return new TrajToSubpart(Integer.parseInt(strArr[0]), Integer.parseInt(strArr[1]),
                Integer.parseInt(strArr[2]));
    }
}
