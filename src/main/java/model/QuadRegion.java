package model;

import java.util.ArrayList;
import java.util.List;

public class QuadRegion {
    double minLat;
    double maxLat;
    double minLon;
    double maxLon;

    QuadRegion[] quadRegionChildren;

    TrajToQuality[] trajQuality;

    /* add */
    TrajToSubpart[] trajToSubparts;
    /* add end */

    public QuadRegion() {
    }

    public QuadRegion(QuadRegion[] quadRegionChildren, TrajToQuality[] trajQuality) {
        this.quadRegionChildren = quadRegionChildren;
        this.trajQuality = trajQuality;
    }

    public QuadRegion(double minLat, double maxLat, double minLon, double maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }

    public void setLocation(double minLat, double maxLat, double minLon, double maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    public double getMinLon() {
        return minLon;
    }

    public void setMinLon(double minLon) {
        this.minLon = minLon;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public void setMaxLon(double maxLon) {
        this.maxLon = maxLon;
    }

    public QuadRegion[] getQuadRegionChildren() {
        return quadRegionChildren;
    }

    public void setQuadRegionChildren(QuadRegion[] quadRegionChildren) {
        this.quadRegionChildren = quadRegionChildren;
    }

    public TrajToQuality[] getTrajQuality() {
        return trajQuality;
    }

    public void setTrajQuality(TrajToQuality[] trajQuality) {
        this.trajQuality = trajQuality;
    }

    public TrajToSubpart[] getTrajToSubparts() {
        return trajToSubparts;
    }

    public void setTrajToSubparts(TrajToSubpart[] trajToSubparts) {
        this.trajToSubparts = trajToSubparts;
    }

    public static List<String> serialize(QuadRegion qr) {
        TrajToSubpart[] trajToSubpartList = qr.trajToSubparts;
        List<String> ret = new ArrayList<>(trajToSubpartList.length + 1);
        ret.add(String.valueOf(trajToSubpartList.length));
        for (TrajToSubpart tts : trajToSubpartList) {
            ret.add(TrajToSubpart.serialize(tts));
        }
        return ret;
    }

    public static QuadRegion antiSerialize(List<String> strList) {
        QuadRegion ret = new QuadRegion();
        TrajToSubpart[] trajToSubparts = new TrajToSubpart[Integer.parseInt(strList.get(0))];
        for (int i = 0; i < trajToSubparts.length; i++) {
            String ttsStr = strList.get(i + 1);
            trajToSubparts[i] = TrajToSubpart.antiSerialize(ttsStr);
            trajToSubparts[i].quality = Double.parseDouble(ttsStr.split(",")[3]);
        }
        ret.setTrajToSubparts(trajToSubparts);
        return ret;
    }
}
