package cpen221.mp3.fsftbuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Representation Invariants (RI):
 * buffer.keySet().equals(timeMap.keySet()) -> keySet of buffer and timeMap match
 * buffer.keySet().size() <= fixedCapacity -> buffer and timeMap always have less keys than fixedCapacity
 *
 * Abstraction Function (AF):
 * FSFTBuffer contains keys of buffer and timeMap in bufferables, where each key in buffer maps to an object
 * in the FSFTBuffer with that id() and each key in timeMap maps to the time at which the object mapped is timed-out
 * Here,
 * fixedCapacity represents the numbers of bufferables allowed in the FSFTBuffer
 * timeout represents the amount of time that a bufferable can last without acted on by other methods.
 *
 * To make FSFT Buffer Thread Safe:
 * The keyword "synchronized" was used to safeguard mutable fields.
 * buffer and timeMap are converted into synchronizedMaps to account for thread safety
 *
 */

public class FSFTBuffer<T extends Bufferable> {

    //variable to checkRep
    private static final boolean CHECK_RI = false;

    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    public static final long SECONDS_TO_MILLIS = 1000;

    private final int fixedCapacity;
    private final long timeout;
    private final Map<String, T> buffer;
    private final Map<String, Long> timeMap;


     // checkRep method to check for Representation Invariants
    synchronized private void checkRep() {
        assert buffer.size() == timeMap.size() :
                "buffer.size() and timeMap.size() should be equal";

        assert buffer.keySet().equals(timeMap.keySet()) :
                "buffer.keySet and timeMap.keySet should be equal";

        assert buffer.size() <= fixedCapacity : "buffer size exceeds capacity";
    }

    /**
     * Create a buffer with a fixed capacity and a timeout value.
     * Objects in the buffer that have not been refreshed within the
     * timeout period are removed from the cache.
     *
     * @param capacity the number of objects the buffer can hold
     * @param timeout  the duration, in seconds, an object should
     *                 be in the buffer before it times out
     */
    public FSFTBuffer(int capacity, int timeout) {
        this.fixedCapacity = capacity;
        this.timeout = timeout * SECONDS_TO_MILLIS;
        buffer = Collections.synchronizedMap(new HashMap<>());
        timeMap = Collections.synchronizedMap(new HashMap<>());
        if (CHECK_RI) {
            checkRep();
        }
    }


    /**
     * Create a buffer with default capacity and timeout values.
     */
    public FSFTBuffer() {
        this(DSIZE, DTIMEOUT);
    }

    /**
     * Add a value to the buffer.
     * If the buffer is full then remove the least recently accessed
     * object to make room for the new object.
     */
    synchronized public boolean put(T t) {
        if (buffer.containsKey(t.id()) && !isTimedOut(t.id())) {
            if (CHECK_RI) {
                System.out.println("Failed to add: " + t.id());
            }

            return false;
        }
        if (buffer.containsKey(t.id()) && !isTimedOut(t.id())) {
            return false;
        }
        if (buffer.keySet().size() == fixedCapacity) {
            updateBuffer();
        }
        if (buffer.keySet().size() == fixedCapacity) {
            List<String> list = new ArrayList<>(timeMap.keySet());

            String id = timeMap.keySet().parallelStream()
                    .reduce(list.get(0), (x, y) -> (timeMap.get(x) < timeMap.get(y)) ? x : y);

            timeMap.remove(id);
            buffer.remove(id);
        }
        buffer.put(t.id(), t);
        timeMap.put(t.id(), System.currentTimeMillis() + timeout);

        if (CHECK_RI) {
            checkRep();
            System.out.println("Add: " + t.id());
        }
        return true;
    }

    /**
     * @param id the identifier of the object to be retrieved
     * @return the object that matches the identifier from the
     * buffer
     * @throws  ObjectNotFoundException if no bufferable in the buffer has t.id() equal to id
     */
    public T get(String id) throws ObjectNotFoundException {

        if (touch(id)) {
            return buffer.get(id);
        }

        throw new ObjectNotFoundException("Object not found in the buffer");

        /* Do not return null. Throw a suitable checked exception when an object
            is not in the cache. You can add the checked exception to the method
            signature. */
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * This method is used to mark an object as "not stale" so that its
     * timeout is delayed.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise
     */
    public boolean touch(String id) {
        synchronized (this) {
            long currentTime = System.currentTimeMillis();
            boolean check = buffer.containsKey(id) && !isTimedOut(id);

            if (!check) {
                buffer.remove(id);
                timeMap.remove(id);
                return false;
            }

            timeMap.replace(id, currentTime + timeout);
        }
        if (CHECK_RI) {
            checkRep();
        }
        return true;
    }


    /**
     * Update an object in the buffer.
     * This method updates an object and acts like a "touch" to
     * renew the object in the cache.
     *
     * @param t the object to update
     * @return true if successful and false otherwise
     */
    public synchronized boolean update(T t) {
        if (CHECK_RI) {
            checkRep();
        }
        buffer.replace(t.id(), t);
        return touch(t.id());
    }

    // Helper Methods in private

    //checks for time-out
    synchronized private boolean isTimedOut(String id) {
        long currentTime = System.currentTimeMillis();
        boolean checkMapId = !(timeMap.get(id) == null);
        return checkMapId && timeMap.get(id) < currentTime;
    }

    //updates the buffer and timeMap by removing objects that have timed out
    synchronized private boolean updateBuffer() {
        long currentTime = System.currentTimeMillis();
        int prevBufferSize = buffer.size();
        Set<String> validKeys =
                timeMap.keySet().parallelStream().filter(x -> timeMap.get(x) > currentTime)
                        .collect(Collectors.toSet());
        timeMap.keySet().removeIf(x -> !validKeys.contains(x));
        buffer.keySet().removeIf(x -> !validKeys.contains(x));

        if (CHECK_RI) {
            checkRep();
        }

        return prevBufferSize > buffer.size();
    }
}