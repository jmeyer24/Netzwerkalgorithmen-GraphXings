package GraphXings.NewFiles;

import java.util.ArrayList;
import java.util.List;
import GraphXings.NewFiles.MixingPlayer.Strategy;

public class ConfigParameterOptimization {
    public List<Double> percentages = new ArrayList<>();
    public List<Double> relativeCircleSizes = new ArrayList<>();
    public List<Integer> sampleSizes = new ArrayList<>();
    public List<MixingPlayer.Strategy> strategies = new ArrayList<>();

    public ConfigParameterOptimization() {
        for (double i = 0.85; i <= 0.95 + 0.01; i += 0.05) {
            this.percentages.add(i);
        }
        for (double i = 0.45; i <= 0.55 + 0.01; i += 0.025) {
            this.relativeCircleSizes.add(i);
        }
        for (int i = 25; i <= 35; i += 5) {
            this.sampleSizes.add(i);
        }
        // this.strategies.add(Strategy.BruteForce);
        this.strategies.add(Strategy.Mirroring);
        this.strategies.add(Strategy.Annealing);
    }

    public ConfigParameterOptimization(boolean any) {
        this.percentages.add(0.93);
        this.relativeCircleSizes.add(0.5);
        this.sampleSizes.add(30);
        this.strategies.add(Strategy.Annealing);
    }
}
