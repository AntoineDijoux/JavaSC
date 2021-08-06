import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.*;

public class CacheManagerTest {

    /**
     * @throws if the integer is null
     */
    private Integer getData(Integer i)
    {
        return i.intValue() + 1;
    }

    private String getData(String s)
    {
        if(s == null)
            return "Nullvalue";

        return s.concat("value");
    }

    @Test
    public void testAddPrimitive()
    {
        // Arrange
        Cache<Integer, Integer> cacheManager = new CacheManager();

        // Act
        var result = cacheManager.get(5,  x-> getData(x));

        // Assert
        assertEquals(6, (int) result);
    }

    @Test
    public void testAddReference()
    {
        // Arrange
        Cache<String, String> cacheManager = new CacheManager();

        // Act
        var result = cacheManager.get("hello",  x-> getData(x));

        // Assert
        assertEquals("hellovalue", result);
    }

    @Test
    public void AddNullValue()
    {
        // Arrange
        Cache<String, String> cacheManager = new CacheManager();

        // Act
        var result = cacheManager.get("hello",  x-> null);

        // Assert
        assertNull(result);
    }

    @Test
    public void AddNullKey()
    {
        // Arrange
        Cache<String, String> cacheManager = new CacheManager();

        // Act
        var result = cacheManager.get(null,  (x) -> getData((String)null));

        // Assert
        assertEquals("Nullvalue", result);
    }

    @Test
    public void AddNullKeyAndValue()
    {
        // Arrange
        Cache<String, String> cacheManager = new CacheManager();

        // Act
        var result = cacheManager.get(null,  (x) -> null);

        // Assert
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void AssertExceptionHandling()
    {
        // Arrange
        Cache<Integer, Integer> cacheManager = new CacheManager();

        // Act
        var result = cacheManager.get(5,  (x) -> getData((Integer) null));
    }

    @Test
    public void testMultiThreadPrimitive()
    {
        Cache cacheManager = new CacheManager();

    }

    @Test
    public void testMultiThreadReference()
    {
        Cache cacheManager = new CacheManager();

    }

}
