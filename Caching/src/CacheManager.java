import java.util.HashMap;
import java.util.function.Function;

public class CacheManager<K, V> implements Cache<K, V> {

    HashMap<K, V> _localCache;

    public CacheManager() {
        _localCache = new HashMap<>();
    }

    /**
     *  Returns the cached value V linked to the provided key K.
     *  If not in the cache, the provided function will be used to retrieve the value. It will then be cached and returned.
     * @param key The key to retrieve the value
     * @param dataRetrievalFunction A function to generate the value V from a key K
     * @return The matching value for the given key
     * @throws IllegalArgumentException When the passed dataRetrievalFunction encounters an exception
     */
    public V get(K key, Function<K, V> dataRetrievalFunction) {

        if(_localCache.containsKey(key))
                return _localCache.get(key);

        // We synchronise only on the key, so not to block the remaining of the cache
        // We check whether key is null, as synchronised keyword cannot handle null
        synchronized (key == null ? this : key) {
            if (_localCache.containsKey(key))
                return _localCache.get(key);

            try{
                V retrievedValue = dataRetrievalFunction.apply(key);
                _localCache.put(key, retrievedValue);
                return retrievedValue;
            }
            catch ( Exception e) {
                throw new IllegalArgumentException("An exception occurred whilst using the passed data retrieval function", e);
            }


        }
    }
}
