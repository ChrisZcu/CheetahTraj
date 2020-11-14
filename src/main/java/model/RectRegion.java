package model;

import app.SharedObject;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.utils.ScreenPosition;

import java.awt.*;

/**
 * Indicates the left-top position and right-bottom position of selected region.
 */
public class RectRegion extends RegionModel {
    public Position leftTop;
    public Position rightBtm;
    public Color color;
    public int id; // modify the type
    public int mapId; // modify the map index

    private Location leftTopLoc;
    private Location rightBtmLoc;

    public RectRegion() {

    }

    public Location getLeftTopLoc() {
        return leftTopLoc;
    }

    public void setLeftTopLoc(Location leftTopLoc) {
        this.leftTopLoc = leftTopLoc;
    }

    public Location getRightBtmLoc() {
        return rightBtmLoc;
    }

    public void setRightBtmLoc(Location rightBtmLoc) {
        this.rightBtmLoc = rightBtmLoc;
    }

    public RectRegion(Position lt, Position rb) {
        this.leftTop = lt;
        this.rightBtm = rb;
    }

    public void initLoc(Location leftTopLoc, Location rightBtmLoc) {
        this.leftTopLoc = leftTopLoc;
        this.rightBtmLoc = rightBtmLoc;
    }

    public void updateScreenPosition(UnfoldingMap map) {
        ScreenPosition lt = map.getScreenPosition(leftTopLoc);
        ScreenPosition rb = map.getScreenPosition(rightBtmLoc);
        leftTop.x = (int) lt.x;
        leftTop.y = (int) lt.y;
        rightBtm.x = (int) rb.x;
        rightBtm.y = (int) rb.y;

    }

    public void updateScreenPosition() {
        updateScreenPosition(SharedObject.getInstance().getMapList()[mapId]);
    }

    public boolean equal(RectRegion r) {
        return this.leftTop.equals(r.leftTop) && this.rightBtm.equals(r.rightBtm);
    }

    public void clear() {
        leftTop = rightBtm = null;
    }

    @Override
    public String toString() {
        int mapIndex = getMapIndex();
        UnfoldingMap map = SharedObject.getInstance().getMapList()[mapIndex];

        Location l1 = map.getLocation(leftTop.x, leftTop.y);
        Location l2 = map.getLocation(rightBtm.x, leftTop.y);
        Location l3 = map.getLocation(rightBtm.x, rightBtm.y);
        Location l4 = map.getLocation(leftTop.x, rightBtm.y);

        StringBuilder info = new StringBuilder();
        info.append("map category: ").append(mapIndex).append("\n").append("location: (").append(l1.toString()).append(",")
                .append(l2.toString()).append(",").append(l3.toString()).append(",").append(l4.toString()).append(")");

        return info.toString();
    }

    private int getMapIndex() {
        int x = leftTop.x;
        int y = leftTop.y;
        float[] mapXList = SharedObject.getInstance().getMapLocInfo()[0];
        float[] mapYList = SharedObject.getInstance().getMapLocInfo()[1];

        int mapWidth = SharedObject.getInstance().getMapWidth();
        int mapHeight = SharedObject.getInstance().getMapHeight();

        for (int i = 0; i < 4; i++) {
            if (x >= mapXList[i] && x <= mapXList[i] + mapWidth
                    && y >= mapYList[i] && y <= mapYList[i] + mapHeight) {
                return i;
            }
        }
        return 0;
    }

    public RectRegion getCorresRegion(int mapId) {
        UnfoldingMap map = SharedObject.getInstance().getMapList()[mapId];

        ScreenPosition leftTopScr = map.getScreenPosition(leftTopLoc);
        ScreenPosition rightBtmScr = map.getScreenPosition(rightBtmLoc);

        RectRegion reg = new RectRegion(new Position(leftTopScr.x, leftTopScr.y), new Position(rightBtmScr.x, rightBtmScr.y));
        reg.color = color;
        reg.id = id;
        reg.initLoc(leftTopLoc, rightBtmLoc);
        reg.mapId = mapId;
        return reg;
    }
}
