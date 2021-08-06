import java.util.function.Function;

/**
 *
 * @param <K>
 * @param <V>
 */
public interface Cache<K, V> {
    /**
     *  Returns the cached value V linked to the provided key K.
     *  If not in the cache, the provided function will be used to retrieve the value. It will then be cached and returned.
     * @param key The key to retrieve the value
     * @param dataRetrievalFunction A function to generate the value V from a key K
     * @return The matching value for the given key
     */
    V get(K key, Function<K, V> dataRetrievalFunction);
}
