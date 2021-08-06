import org.junit.Test;
import java.util.Arrays;
import java.util.stream.IntStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
    public void testMultiThreadPrimitive1()
    {
        // Arrange
        int[] range = IntStream.rangeClosed(1, 5000).toArray();
        Cache<Integer, Integer> cacheManager = new CacheManager();

        // Act
        Arrays.stream(range).parallel().forEach(x ->
        {
            assertEquals(x+1, (int) cacheManager.get(x, (y) -> getData(y)));
        });
    }



    /**
     * Attempt at testing the multi threading writing and reading with collision.
     * Could use timers mixed with thread sleep function to simulate access to an external database,
     * to check the locks efficiency.
     */
    //@Test
    public void testMultiThreadPrimitive2()
    {
        // Arrange
        int[] range = IntStream.rangeClosed(1, 5000).toArray();

        // I create an inverse range to be sure to create a collision
        int[] reversedRange = reverse(range);

        int[][]ranges = new int[5][];
        ranges[0] = range;
        ranges[1] = range;
        ranges[2] = reversedRange;
        ranges[3] = reversedRange;
        ranges[4] = reversedRange;

        Cache<Integer, Integer> cacheManager = new CacheManager();

        // Act
/*        var task1 = CompletableFuture.runAsync({
            Arrays.stream(ranges[0]).parallel().forEach(x ->
              {
                  assertEquals(x+1, (int) cacheManager.get(x, (y) -> getData(y)));
              });
        });*/
    }

    @Test
    public void testReverse()
    {
        int[] range = {1,2,3,4};
        int[] reversedRange = reverse(range);

        assertEquals(4, reversedRange[0]);
        assertEquals(3, reversedRange[1]);
        assertEquals(2, reversedRange[2]);
        assertEquals(1, reversedRange[3]);
    }

    /**
     * Will create a new array that is the reverse of the original one
     */
    private int[] reverse(int[] originalArray)
    {
        int[] reversedArray = new int[originalArray.length];

        int count = originalArray.length-1;
        for(int tempValue : originalArray) {
            reversedArray[count] = tempValue;
            count--;
        }

        return reversedArray;
    }


}
