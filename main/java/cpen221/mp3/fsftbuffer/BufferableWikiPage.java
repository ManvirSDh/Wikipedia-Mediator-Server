package cpen221.mp3.fsftbuffer;

/**
 * Datatype used to store pages from wikipedia into a FSFT buffer.
 */
public class BufferableWikiPage implements Bufferable {

    private String page;
    private String title;

    public BufferableWikiPage(String title, String page) {
        this.page = page;
        this.title = title;
    }

    public String id() {
        return new String(this.title);
    }

    public String getPageText() {
        return new String(this.page);
    }
}
