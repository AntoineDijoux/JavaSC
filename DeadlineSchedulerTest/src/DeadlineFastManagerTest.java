import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.*;

public class DeadlineFastManagerTest {

    private DeadlineFastManager _dm = new DeadlineFastManager();

    private long getPastDate()
    {
        LocalDateTime now = LocalDateTime.now().minusYears(1);
        return now.toEpochSecond(ZoneOffset.UTC)*1000;
    }

    private long getFutureDate()
    {
        LocalDateTime now = LocalDateTime.now().plusYears(10);
        return now.toEpochSecond(ZoneOffset.UTC)*1000;
    }

    @Test
    public void testSchedule1()
    {
        long result1 = _dm.schedule(getPastDate());
        long result2 = _dm.schedule(getPastDate());

        // Assert
        assertNotEquals(result1, result2);
        assertEquals(2, _dm.size());
    }

    @Test
    public void testSchedule2()
    {
        long result1 = _dm.schedule(getFutureDate());
        long result2 = _dm.schedule(getFutureDate());

        // Assert
        assertNotEquals(result1, result2);
        assertEquals(2, _dm.size());
    }

    @Test
    public void testCancel1()
    {
        // Arrange
        long result1 = _dm.schedule(getFutureDate());

        // Act
        var success = _dm.cancel(result1);

        // Assert
        assertTrue(success);
    }

    @Test
    public void testCancel2()
    {
        // Arrange
        long result1 = _dm.schedule(getFutureDate());

        // Act
        var success = _dm.cancel(result1 + 1);

        // Assert
        assertFalse(success);
    }

    @Test
    public void testCancel3()
    {
        // Arrange
        long result1 = _dm.schedule(getPastDate());

        // Act
        var success = _dm.cancel(result1);

        // Assert
        assertTrue(success);
    }

    @Test
    public void testCancel4()
    {
        // Arrange
        long result1 = _dm.schedule(getPastDate());

        // Act
        var success = _dm.cancel(result1 + 1);

        // Assert
        assertFalse(success);
    }

    @Test
    public void testCancel5()
    {
        // Arrange
        long result1 = _dm.schedule(getPastDate());
        long result2 = _dm.schedule(getPastDate()+1);
        long result3 = _dm.schedule(getPastDate()+2);

        // check the size
        assertEquals(3, _dm.size());

        // we remove one
        var success = _dm.cancel(result1);
        assertTrue(success);
        assertEquals(2, _dm.size());

        // we try to remove again the same one
        success = _dm.cancel(result1);
        assertEquals(2, _dm.size());
        assertFalse(success);
    }

    @Test
    public void testCancel6()
    {
        // Arrange
        long result1 = _dm.schedule(getFutureDate());
        long result2 = _dm.schedule(getFutureDate()+1);
        long result3 = _dm.schedule(getFutureDate()+2);

        // check the size
        assertEquals(3, _dm.size());

        // we remove one
        var success = _dm.cancel(result1);
        assertTrue(success);
        assertEquals(2, _dm.size());

        // we try to remove again the same one
        success = _dm.cancel(result1);
        assertEquals(2, _dm.size());
        assertFalse(success);


    }
}
