package GraphXings.NewFiles;

import java.util.ArrayList;
import java.util.List;
import GraphXings.NewFiles.GraphDracula.MaximizingStrategy;
import GraphXings.NewFiles.GraphDracula.MinimizingStrategy;

public class StrategyOptimization {
    public List<MaximizingStrategy> maxStrats = new ArrayList<>();
    public List<MinimizingStrategy> minStrats = new ArrayList<>();

    public StrategyOptimization() {
        this.maxStrats.add(MaximizingStrategy.Mirroring);
        this.maxStrats.add(MaximizingStrategy.Annealing);
        this.maxStrats.add(MaximizingStrategy.RadialStuff);

        this.minStrats.add(MinimizingStrategy.BorderWalk);
        this.minStrats.add(MinimizingStrategy.EdgeDirectionMean);
        this.minStrats.add(MinimizingStrategy.TreeMinimizer);
    }
}