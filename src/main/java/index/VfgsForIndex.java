package index;

import model.Position;
import model.TrajToQuality;
import model.TrajectoryMeta;
import util.GreedyChooseMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class VfgsForIndex {

    public static TrajToQuality[] getVfgs(TrajectoryMeta[] trajFull) {
        ArrayList<TrajToQuality> vfgsTraj = new ArrayList<>();
        try {

            double totalScore = getTotalScore(trajFull);
            double lastScore = 0.0;

            GreedyChooseMeta greedyChooseMeta = new GreedyChooseMeta(trajFull.length);
            trajScoreInit(trajFull, greedyChooseMeta);
            HashSet<Position> influScoreSet = new HashSet<>();
            for (int i = 0; i < trajFull.length; i++) {
                while (true) {
                    updateTrajScore(greedyChooseMeta.getHeapHead(), influScoreSet);
                    if (greedyChooseMeta.GreetOrder()) {
                        TrajectoryMeta traj = greedyChooseMeta.getMaxScoreTraj();
                        updateInfluScoreSet(traj, influScoreSet);
                        vfgsTraj.add(new TrajToQuality(traj, (traj.getScore() + lastScore) / totalScore));
                        lastScore += traj.getScore();
                        if (lastScore >= totalScore) {
                            i = trajFull.length + 1;
                        }
                        break;
                    } else {
                        greedyChooseMeta.orderAdjust();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vfgsTraj.toArray(new TrajToQuality[0]);
    }

    private static void updateInfluScoreSet(TrajectoryMeta TrajectoryMeta, HashSet<Position> influSet) {
        influSet.addAll(Arrays.asList(TrajectoryMeta.getPositions()));
    }

    private static void trajScoreInit(TrajectoryMeta[] trajectories, GreedyChooseMeta greedyChooseMeta) {
        for (TrajectoryMeta traj : trajectories) {
            traj.scoreInit();
            greedyChooseMeta.addNewTraj(traj);
        }
    }

    private HashSet<Position> getTotalScoreSet(TrajectoryMeta[] trajFull) {
        HashSet<Position> totalScoreSet = new HashSet<>(trajFull.length);
        for (TrajectoryMeta traj : trajFull) {
            totalScoreSet.addAll(Arrays.asList(traj.getPositions()));
        }
        return totalScoreSet;
    }

    private static int getTotalScore(TrajectoryMeta[] trajFull) {

        HashSet<Position> totalScoreSet = new HashSet<>(trajFull.length);
        for (TrajectoryMeta traj : trajFull) {
            totalScoreSet.addAll(Arrays.asList(traj.getPositions()));
        }
        return totalScoreSet.size();
    }

    private static void updateTrajScore(TrajectoryMeta TrajectoryMeta, HashSet<Position> influScoreSet) {
        double score = 0;
        for (Position position : TrajectoryMeta.getPositions()) {
            if (!influScoreSet.contains(position)) {
                score++;
            }
        }
        TrajectoryMeta.updateScore(score);
    }
}
