package model;

import java.util.ArrayList;

/**
 * different group of way point
 */
public class WayPointGroup {
    private int groupId;
    private int wayPointLayer = 0;
    private ArrayList<ArrayList<RectRegion>> wayPointLayerList;

    public WayPointGroup() {
        wayPointLayerList = new ArrayList<>();
    }

    public WayPointGroup(int groupId) {
        this.groupId = groupId;
        wayPointLayerList = new ArrayList<>();
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getWayPointLayer() {
        return wayPointLayer;
    }

    public void setWayPointLayer(int wayPointLayer) {
        this.wayPointLayer = wayPointLayer;
    }

    public ArrayList<ArrayList<RectRegion>> getWayPointLayerList() {
        return wayPointLayerList;
    }

    public void setWayPointLayerList(ArrayList<ArrayList<RectRegion>> wayPointLayerList) {
        this.wayPointLayerList = wayPointLayerList;
    }

    public void cleanWayPointRegions() {
        wayPointLayerList.clear();
    }

    public void updateWayPointLayer() {
        if (wayPointLayerList.size() == wayPointLayer + 1) {
            wayPointLayer++;
        }
    }

    public ArrayList<RectRegion> getAllRegions() {
        ArrayList<RectRegion> res = new ArrayList<>();
        if (wayPointLayerList != null) {
            for (ArrayList<RectRegion> regionList : wayPointLayerList) {
                res.addAll(regionList);
            }
        }
        return res;
    }

    public void addWayPoint(RectRegion r) {
        if (wayPointLayerList.size() <= wayPointLayer) {
            wayPointLayerList.add(new ArrayList<>());
        }
        wayPointLayerList.get(wayPointLayer).add(r);
        //TODO update the shared object
    }

    public WayPointGroup getCorrWayPointGroup(int mapId) {
        WayPointGroup wayPointGroup = new WayPointGroup(groupId);
        wayPointGroup.setWayPointLayer(wayPointLayer);
        for (ArrayList<RectRegion> wList : wayPointLayerList) {
            ArrayList<RectRegion> tmp = new ArrayList<>();
            for (RectRegion r : wList) {
                tmp.add(r.getCorresRegion(mapId));
            }
            wayPointGroup.getWayPointLayerList().add(tmp);
        }
        return wayPointGroup;
    }
}
