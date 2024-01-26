package GraphXings.Gruppe4;

import GraphXings.Gruppe4.Strategies.StrategyName;

import java.util.HashMap;

public class StrategiesStopWatch {
    private final HashMap<StrategyName, StopWatch> strategyStopWatches = new HashMap<>();

    public StopWatch getWatch(StrategyName strategyName) {
        if (!strategyStopWatches.containsKey(strategyName)) {
            strategyStopWatches.put(strategyName, new StopWatch());
        }
        return strategyStopWatches.get(strategyName);
    }

    public HashMap<StrategyName, StopWatch> getStrategyStopWatches() {
        return strategyStopWatches;
    }
}
