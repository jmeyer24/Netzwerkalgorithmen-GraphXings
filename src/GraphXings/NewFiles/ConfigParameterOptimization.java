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
        for (double i = 0.85; i <= 0.95; i += 0.05) {
            this.percentages.add(i);
        }
        for (double i = 0.4; i <= 0.6; i += 0.025) {
            this.relativeCircleSizes.add(i);
        }
        for (int i = (int) 20.0; i <= 40; i += 5) {
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
