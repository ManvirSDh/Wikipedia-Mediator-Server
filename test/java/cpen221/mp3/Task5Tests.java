package cpen221.mp3;

import org.junit.Test;

import java.util.concurrent.TimeoutException;

import cpen221.mp3.wikimediator.WikiMediator;

public class Task5Tests {


    private static WikiMediator mediator = new WikiMediator(20, 10);


    @Test
    public void testShortestPath() throws TimeoutException {
        String pageTitle1 = "Barack Obama";
        String pageTitle2 = "Michelle Obama";

        System.out.println(mediator.shortestPath(pageTitle1, pageTitle2, 100000));
    }

    @Test
    public void testShortestPathObama() throws TimeoutException {
        System.out.println(mediator.shortestPath("Philosophy", "Barack Obama", 100000));
    }

    @Test
    public void testShortestPath1Jump() throws TimeoutException {
        System.out.println(mediator.shortestPath("Bhai Mani Singh", "Khalsa", 100000));
    }

    @Test
    public void testShortestPathSickoMode() throws TimeoutException {
        System.out.println(mediator.shortestPath("United States", "Travis Scott", 100000));
    }
}
