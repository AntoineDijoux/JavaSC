import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * A (0 (log (n)) performant version of the DeadlineEngine.
 * Technical: leveraging on the stored long, which I would split 40 bits for the deadline (30 years)
 * and 20 bits of Ids for each deadline (1 million).
 */
public class DeadlineFastManager implements DeadlineEngine {
    /**
     * The present. Used instead of Epoch to save 50 years of epoch time in bits
     */
    private Long _engineStartTimeFromEpoch;

    /**
     * Number of bits reserved by the dates themselves from now
     * 2^40 = 1000 billion seconds = 30 years from now
     */
    private final byte _deadlineDateBytes = 40;

    /**
     * Number of bits reserves for a unique ID at a given date
     * 2^20 = 1 million different entries allowed for a given date
     */
    private final byte _deadlineUniqueIdBytes = 20;

    /**
     * Used to run the callback methods
     */
    private ScheduledExecutorService _executorService;

    /**
     * The deadlines
     * first (1+) 40 bits are the deadline time in the future from now in milliseconds
     * last 20 bits are the unique ID of the deadline
     */
    private TreeSet<Long> _deadlines;

    /**
     * Nb of cores, useful to optimise our executor
     */
    private int _nbCores;

    /**
     * Acceptable timeout in milliseconds for the callbacks to finish
     */
    private final long _shutdownTimeOut;

    /**
     * New instance of this class
     */
    public DeadlineFastManager() {
        this(1000);
    }

    /**
     * New instance of this class
     * @param handlersTimeOut The timeout for handlers requests when polling. Threads will be killed silently after that time.
     */
    public DeadlineFastManager(long handlersTimeOut) {
        _deadlines = new TreeSet<>();
        _engineStartTimeFromEpoch = Instant.EPOCH.toEpochMilli();
        _nbCores = Runtime.getRuntime().availableProcessors();
        _executorService = Executors.newScheduledThreadPool(_nbCores);
        _shutdownTimeOut = handlersTimeOut;
    }

    /**
     * Converts a timefromEpoch to a timeFromNow. Then frees X bits to the left.
     * @param timeFromEpoch
     * @return
     */
    private long getHeadTimeBitwise(long timeFromEpoch)
    {
        var futureMilliseconds = Math.max(timeFromEpoch - _engineStartTimeFromEpoch, 0);
        return futureMilliseconds << _deadlineUniqueIdBytes;
    }

    /**
     * Request a new deadline be added to the engine.  The deadline is in millis offset from
     * unix epoch. https://en.wikipedia.org/wiki/Unix_time
     * The engine will raise an event whenever a deadline (usually now in millis) supplied in the poll method
     * exceeds the request deadline.
     * Note 1: Logarithmic running time ( O(log n) )
     * @param deadlineMs the millis
     * @return An identifier for the scheduled deadline.
     */
    public long schedule(long deadlineMs)
    {
        // We convert to our time notation
        var bitwiseDeadLineTime = getHeadTimeBitwise(deadlineMs);
        var bitwiseDeadLineUpperBound = getHeadTimeBitwise(deadlineMs+1);

        // from the treeSet, we retrieve a subset of all the IDs for that deadline,
        // meaning everything that is bitwise between the deadline and the deadline + 1
        var subTreeForThatDeadline = _deadlines.subSet(bitwiseDeadLineTime, bitwiseDeadLineUpperBound);

        // If we don't have any element in that subset, it means we have no deadline for that time
        if(subTreeForThatDeadline.isEmpty())
        {
            _deadlines.add(bitwiseDeadLineTime);
            return bitwiseDeadLineTime;
        }
        // Else, we already have at least one deadline ID for that time. We add one.
        else
        {
            var newId = subTreeForThatDeadline.last() + 1;
            subTreeForThatDeadline.add(newId);
            return newId;
        }
    }

    /**
     * Remove the scheduled event using the identifier returned when the deadline was scheduled.
     * Note 1: Logarithmic running time ( O(log n) ) as for access to the deadlines
     * @param requestId identifier to cancel.
     * @return true if canceled.
     */
    public boolean cancel(long requestId) {
        return _deadlines.remove(_deadlines);
    }

    /**
     * Supplies a deadline in millis to check against scheduled deadlines.  If any deadlines are triggered the
     * supplied handler is called with the identifier of the expired deadline.
     * To avoid a system flood and manage how many expired events we can handle we also pass in the maximum number of
     * expired deadlines to fire.  Those expired deadlines that weren't raised will be available in the next poll.
     * There is no need for the triggered deadlines to fire in order.
     * Note 1: Logarithmic running time ( O(log n) ) as for access to the deadlines
     * Note 2: Will run in parallel the execution of multiple handler. Won't throw exceptions, will time out silently.
     * @param nowMs   time in millis since epoch to check deadlines against.
     * @param handler to call with identifier of expired deadlines.
     * @param maxPoll count of maximum number of expired deadlines to process.
     * @return number of expired deadlines that fired successfully.
     */
    public int poll(long nowMs, Consumer<Long> handler, int maxPoll)
    {
        // We convert to our time notation
        var bitwiseDeadLineUpperBound = getHeadTimeBitwise(nowMs+1);

        // from the treeSet, we retrieve a subset of all the deadlines where deadline < (deadlineMs + 1)
        var subTreeForThatDeadline = _deadlines.subSet(0L, bitwiseDeadLineUpperBound);

        int i = 0;
        for (Iterator<Long> longIterator = subTreeForThatDeadline.iterator(); longIterator.hasNext() && i < maxPoll;)
        {
            Long element = longIterator.next();
            longIterator.remove();
            runConsumer(handler, element);
        }

        return i;
    }

    /**
     * Runs a Consumer in a multithreaded fashion.
     * Tasks will time out after _shutdownTimeOut. It will print the exception but not throw.
     * @param handler The method to execute
     * @param element The long parameter for the consumer
     */
    private void runConsumer(Consumer<Long> handler, Long element)
    {
        final Future futureTask = _executorService.submit(() -> handler.accept(element));

        // We schedule a task that will cancel the handler after a timeout, to avoid orphan threads in the future
        _executorService.schedule(() -> {
            futureTask.cancel(true);
            IllegalArgumentException iae = new IllegalArgumentException(
                    String.format("Timeout of %sms reached when running callback with long '%s'", _shutdownTimeOut, element));
            iae.printStackTrace();

        }, _shutdownTimeOut, TimeUnit.MILLISECONDS);
    }

    /**
     * 0(1) time complexity
     * @return the number of registered deadlines.
     */
    public int size() {
        return _deadlines.size();
    }

    /**
     * Graceful shutdown of the executor.
     */
    private void gracefulShutDown()
    {
        _executorService.shutdown();
        try {
            if (!_executorService.awaitTermination(_shutdownTimeOut, TimeUnit.MILLISECONDS)) {
                _executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            _executorService.shutdownNow();
        }
    }
}
