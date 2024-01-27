package GraphXings.Gruppe4.Strategies;

/**
 * Represents the strategy names.
 * This is used to keep the memory footprint as low as possible
 * instead of allocating many strings in the statistics.
 */
public enum StrategyName {
    MaximizeDiagonalCrossing,
    MaximizePlaceInDenseRegion,
    MaximizePlaceVertexOnEdge,
    MinimizePlaceAtBorder,
    MinimizePlaceNextToOpponent,
    MinimizeRandomSampleMove,
    MaximizeRandomSampleMove,
    RandomMove,
    MaximizePointReflection,
    MaximizePointReflectionFromBorder,
    MaximizeGrid,
    MinimizeGridAngle,
    Unknown
}
