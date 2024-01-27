package GraphXings.Gruppe4;

public class StopWatch {

    private long elapsed = 0;
    private boolean isStartable = true;
    private long startTime = 0;
    private long currentElapsedLap = 0;

    /**
     * Construct a new stop watch with a name i.e. a name from the StrategyName enum
     * @param watchName An element of type T
     */
    public StopWatch() {
    }

    /**
     * Start the timer if it is not running.
     * @return True on successful start
     */
    public boolean startTimer() {
        if (isStartable) {
            startTime = System.nanoTime();
            isStartable = false;
            return true;
        }
        return false;
    }

    /**
     * Stops the timer if the timer is running.
     * @return True on successful stop
     */
    public boolean stopTimer() {
        if (!isStartable) {
            long stopTime = System.nanoTime();
            currentElapsedLap = stopTime - startTime;
            elapsed += currentElapsedLap;
            isStartable = true;
            return true;
        }
        return false;
    }

    /**
     * Get the total elapsed time throughout all laps
     * @return Time in nano seconds
     */
    public long getTotalElapsedTime() {
        return elapsed;
    }

    /**
     * Get the elapsed time of the current lap.
     * @return Time in nano seconds
     */
    public long getCurrentElapsedLap() {
        return currentElapsedLap;
    }
}
