package cpen221.mp3.fsftbuffer;

public class ObjectNotFoundException extends Exception {
    /**
     * Exception for ObjectNotFound in FSFTBuffer
     * @param message an error message
     */
    public ObjectNotFoundException(String message) {
        super(message);
    }
}