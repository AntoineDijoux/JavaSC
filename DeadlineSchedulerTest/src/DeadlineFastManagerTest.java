import org.junit.Test;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class DeadlineFastManagerTest {

    private DeadlineFastManager _dm = new DeadlineFastManager();

    /**
     *
     */
    private Consumer<Long> _filePrinter = x -> {
        try {
            Thread.sleep(50l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        writeToFile(x);
    };

    /**
     * Simple task that sleeps 50ms and print the passed long
     */
    private Consumer<Long> _printer = x -> {
        try {
            Thread.sleep(50l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(x);
    };

    /**
     * appends to a file the given long
     * @param myLong
     */
    private void writeToFile(Long myLong) {
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(new FileOutputStream(
                    new File("C:\\temp\\javaTest.txt"),
                    true /* append = true */));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        writer.println(myLong);
        writer.close();
    }

    /**
     * Used to generate a date in the past
     * @return an epoch to milliseconds date in the past
     */
    private long getPastDate()
    {
        LocalDateTime now = LocalDateTime.now().minusYears(1);
        return now.toEpochSecond(ZoneOffset.UTC)*1000;
    }

    /**
     * Used to generate a date in the future
     * @return an epoch to milliseconds date in the future
     */
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

    @Test
    public void testCancel7()
    {
        // Arrange
        long result1 = _dm.schedule(getPastDate());
        long result2 = _dm.schedule(getPastDate()+1);
        long result3 = _dm.schedule(getPastDate()+2);
        long result4 = _dm.schedule(getPastDate()+3);


        // check the size
        assertEquals(4, _dm.size());

        // we remove one
        var success = _dm.cancel(result1);
        assertTrue(success);
        assertEquals(3, _dm.size());

        // we try to execute one out of the 2 remaining
        int pollresult = _dm.poll(Instant.now().toEpochMilli(), _printer, 1);
        assertEquals(2, _dm.size());
        assertEquals(1, pollresult);
    }

    @Test
    public void testPoll1() {
        // Arrange
        long result1 = _dm.schedule(getFutureDate());
        long result2 = _dm.schedule(getFutureDate() + 1);
        long result3 = _dm.schedule(getFutureDate() + 2);

        int pollresult = _dm.poll(Instant.now().toEpochMilli(), _printer, 1);
        assertEquals(0, pollresult);
    }

    @Test
    public void testPoll2() {
        long result1 = _dm.schedule(getPastDate());
        long result2 = _dm.schedule(getPastDate()+1);
        long result3 = _dm.schedule(getPastDate()+2);

        // Tasks from the past should be running, maximum 1
        int pollresult = _dm.poll(Instant.now().toEpochMilli(), _printer, 1);
        assertEquals(1, pollresult);

        // Remaining tasks should be running
        pollresult = _dm.poll(Instant.now().toEpochMilli(), _printer, 5);
        assertEquals(2, pollresult);
    }

    @Test
    public void testPoll3() {
        long result1 = _dm.schedule(getPastDate());
        long result2 = _dm.schedule(getPastDate()+1);
        long result3 = _dm.schedule(getPastDate()+2);
        _dm.schedule(getFutureDate());
        _dm.schedule(getFutureDate()+1);
        _dm.schedule(getFutureDate()+2);

        // Tasks from the past should be running, maximum 1
        int pollresult = _dm.poll(Instant.now().toEpochMilli(), _printer, 1);
        assertEquals(1, pollresult);

        // Remaining tasks should be running
        pollresult = _dm.poll(Instant.now().toEpochMilli(), _printer, 5);
        assertEquals(2, pollresult);
    }

    @Test
    public void testPoll4() throws InterruptedException {
        long result1 = _dm.schedule(Instant.now().toEpochMilli()+1000);

        // Should not trigger right now
        int pollresult = _dm.poll(Instant.now().toEpochMilli(), _printer, 5);
        assertEquals(0, pollresult);

        // we wait a bit
        Thread.sleep(1000);

        // should trigger now
        pollresult = _dm.poll(Instant.now().toEpochMilli(), _printer, 5);
        assertEquals(1, pollresult);
    }
}
