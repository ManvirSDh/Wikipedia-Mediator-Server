package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeoutException;

public class Task3Tests {
    private static WikiMediator mediator = new WikiMediator(20, 10);
    private static WikiMediator mediator2 = new WikiMediator(20, 10);


    @Test
    public void testSearch() {
        System.out.println(mediator.search("1984", 10));
    }

    @Test
    public void testGetPage() {
        Long time = System.currentTimeMillis();
        mediator.getPage("Bhai Nand Lal");
        System.out.println(System.currentTimeMillis()-time);
        time = System.currentTimeMillis();
        mediator.getPage("Bhai Nand Lal");
        System.out.println(System.currentTimeMillis()-time);
        time = System.currentTimeMillis();
        mediator.getPage("Bhai Nand Lal");
        System.out.println(System.currentTimeMillis()-time);
        time = System.currentTimeMillis();
        mediator.getPage("Bhai Nand Lal");
        System.out.println(System.currentTimeMillis()-time);
        time = System.currentTimeMillis();
        mediator.getPage("Obama");
        System.out.println(System.currentTimeMillis()-time);
        time = System.currentTimeMillis();
        mediator.getPage("Obama");
        System.out.println(System.currentTimeMillis()-time);
    }

    @Test
    public void testZeitgeist() {
        testGetPage();
        testSearch();
        System.out.println(mediator.zeitgeist(5));
    }

    @Test
    public void testTrending() {
        testGetPage();
        testSearch();
        System.out.println(mediator.trending(1, 9));
    }

    @Test
    public void testWindowPeakLoad() {
        testZeitgeist();
        testTrending();
        System.out.println(mediator.windowedPeakLoad(1));
        System.out.println(mediator.windowedPeakLoad());
    }

    @Test
    public void testShortestPath() throws TimeoutException {
        String pageTitle1 = "Barack Obama";
        String pageTitle2 = "Michelle Obama";

//        Assertions.assertEquals(mediator.shortestPath(pageTitle1, pageTitle2, 100000), mediator.shortestPath(pageTitle1, pageTitle2, 100000));
          System.out.println(mediator.shortestPath(pageTitle1, pageTitle2, 100000));
    }

    @Test
    public void testStoreLoad() {
        mediator.zeitgeist(10);
        File deleteFile = new File("./local/pastSearchGetRequests.ser");
        deleteFile.delete();
        deleteFile = new File("./local/requestTimes.ser");
        deleteFile.delete();

        mediator.storeRequests();
        mediator2.readStorage();
        System.out.println("\n\n\n\n\n\n" + mediator2.zeitgeist(10));
    }
}
