package app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class DataProcessing {

    private static String[][] getAry(String filePath) {
        ArrayList<String> metaData = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            for (int i = 0; i < line.split(",").length; i++) {
                System.out.println(line.split(",")[i] + ": " + i);
            }
            while ((line = reader.readLine()) != null) {
                metaData.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("meta data: " + metaData.size());
        String[][] item = new String[metaData.size()][];
        int i = 0;
        for (String meta : metaData) {
            item[i++] = meta.split(",");
        }
        return item;
    }

    //zoomlevel,regionId,regionSize,algorithm,rate,delta,waypointCost,computionCost,mappingCost,renderingCost,quality,qualityCost
    private static void allRecord() {
        String[][] metaAry = getAry("data/localRec/AllRecord.txt");
        System.out.println("zoomlevel,regionId,regionSize,algorithm,rate,delta,waypointCost,computionCost,mappingCost,renderingCost,quality,qualityCost");
        HashMap<String, Double> zoomToQuality = new HashMap<>();
        int cnt = 0;
        for (String[] list : metaAry) {
            if (Integer.parseInt(list[3]) == 1 && list[4].equals("0.001") && list[5].equals("4")) {
                if (list[0].equals("11") && Integer.parseInt(list[3]) == 1 && list[4].equals("0.001") && list[5].equals("4")) {
                    System.out.println(Arrays.toString(list));

                    cnt++;
                }
                if (!zoomToQuality.containsKey(list[0])) {
                    zoomToQuality.put(list[0], 0.0);
                }
                zoomToQuality.put(list[0], zoomToQuality.get(list[0]) + Double.parseDouble(list[10]));
            }
        }
        System.out.println(cnt);
        for (String zoom : zoomToQuality.keySet()) {
            System.out.println(zoom + ": " + zoomToQuality.get(zoom) / (cnt + 1));
        }
    }

    private static void solutionXRecord(String filePath) {

        //zoomlevel,regionId,regionSize,No.trajectory,searchCost,mappingCost,renderCost

        String[][] solutionTime = getAry(filePath);
        int cnt = 0;
        int num = 0;
        HashMap<String, Double> zoomToQuality = new HashMap<>();
        for (String[] list : solutionTime) {
            /*
            if (list[0].equals("11")) {
                cnt++;
            }
             */
            if (!list[0].equals("14")) {
                continue;
            }
            cnt++;
            if (!zoomToQuality.containsKey(list[2])) {
                zoomToQuality.put(list[2], 0.0);
            }
            if (list[2].equals("0.5")) {
                num++;
            }
            zoomToQuality.put(list[2], zoomToQuality.get(list[2]) + Double.parseDouble(list[5])
                    /*
                    +Double.parseDouble(list[5]) + Double.parseDouble(list[6])
                    */
            );
        }
        System.out.println(num + ", " + cnt);

        for (String zoom : zoomToQuality.keySet()) {
            System.out.println(zoom + ", " + zoomToQuality.get(zoom) / num / 1000);
        }
    }

    public static void main(String[] args) {
        solutionXRecord("data/localRec/solutionX4.txt");
    }
}
