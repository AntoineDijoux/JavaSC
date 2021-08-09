import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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



}
