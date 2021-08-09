import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * @deprecated use {@link DeadlineFastManager} instead
 * first attempt at the implementation of the DeadlineEngine, here to show my thought process to the recruiter.
 * I started working with a treemap (deadline / ID). Then I thought I could use only a treeSet by
 * leveraging on the long, which I would split 40 bits for the deadline (30 years)
 * and 20 bits of Ids for each deadline (1 million).
 */
@Deprecated
public class DeadlineManager implements DeadlineEngine {

    /**
     * The present
     */
    private Long startingTimeFromEpoch;

    /**
     * key is the deadline,
     * value is a collection of unique IDs for that deadline
     * Note 1: I considered storing Short instead of longs for the unique IDs, but perhaps we will
     * have more than 2^16 unique IDs for important deadlines (who said dividends or, market open/close?)
     * Note 2: I also considered storing small types instead of Long for the deadline,
     * but even one year in millisecond is larger than 2 ^32;
     * Note 3: I also considered storing everything on the same long, using the first 40 bytes as the timestamp,
     * the last 24 as a unique ID, something like that. But that could break the specs if more than 2^24 deadlines
     * at the same time.
     * Note 4: Finally, we use an ArrayDeque for storing the IDs, as adding or removing at the tail is 0(1).
     * ArrayDeque also has an internal growth mechanism of a power of 2 instead of +10 for Arraylists.
     */
    private TreeMap<Long, ArrayDeque<Long>> _deadlines;

    public DeadlineManager() {
        this._deadlines = new TreeMap<>();
        startingTimeFromEpoch=Instant.EPOCH.toEpochMilli();
        throw new java.lang.UnsupportedOperationException("Deprecated, use DeadlineFastManager instead.");
    }

    /**
     * Request a new deadline be added to the engine.  The deadline is in millis offset from
     * unix epoch. https://en.wikipedia.org/wiki/Unix_time
     * The engine will raise an event whenever a deadline (usually now in millis) supplied in the poll method
     * exceeds the request deadline.
     *
     * @param deadlineMs the millis
     * @return An identifier for the scheduled deadline.
     */
    public long schedule(long deadlineMs) {
        /**
         * All values in the past are stored at the same index 0 to reduce the tree size
         */
        long actualDuration = Math.max(deadlineMs - startingTimeFromEpoch, 0);

        if(!_deadlines.containsKey(actualDuration))
        {
            _deadlines.put(actualDuration, new ArrayDeque<>());
        };

        return actualDuration;
    }

    /**
     * Remove the scheduled event using the identifier returned when the deadline was scheduled.
     *
     * @param requestId identifier to cancel.
     * @return true if canceled.
     */
    public boolean cancel(long requestId) {
        return false;
    }

    /**
     * Supplies a deadline in millis to check against scheduled deadlines.  If any deadlines are triggered the
     * supplied handler is called with the identifier of the expired deadline.
     * To avoid a system flood and manage how many expired events we can handle we also pass in the maximum number of
     * expired deadlines to fire.  Those expired deadlines that weren't raised will be available in the next poll.
     * There is no need for the triggered deadlines to fire in order.
     *
     * @param nowMs   time in millis since epoch to check deadlines against.
     * @param handler to call with identifier of expired deadlines.
     * @param maxPoll count of maximum number of expired deadlines to process.
     * @return number of expired deadlines that fired successfully.
     */
    public int poll(long nowMs, Consumer<Long> handler, int maxPoll) {
        return 0;
    }

    /**
     * @return the number of registered deadlines.
     */
    public int size() {
        return 0;
    }
}
