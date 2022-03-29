package cpen221.mp3;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import cpen221.mp3.server.WikiMediatorClient;
import cpen221.mp3.server.WikiMediatorServer;
import cpen221.mp3.wikimediator.WikiMediator;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Task4Tests {

    private static final int port = 4323;
    private static final String localhost = "127.0.0.1";
    private static final Gson jsonConverter = new Gson();
//    private static WikiMediatorServer server;
//    private static WikiMediator mediator;

//    @BeforeAll
//    static void createServer() throws InterruptedException {
//        File deleteFile = new File("./local/pastSearchGetRequests.ser");
//        deleteFile.delete();
//        deleteFile = new File("./local/requestTimes.ser");
//        deleteFile.delete();
//
//        WikiMediator mediator = new WikiMediator(20, 100);
//        WikiMediatorServer server = new WikiMediatorServer(port, 5, mediator);
//        Thread serverThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//                server.serve();
//                System.out.println("AAAAAAAAAAA");
//            }
//        });
//        serverThread.start();
//        Thread.sleep(5000);
//    }

    @Test
    public void testReadStoreData() throws IOException, InterruptedException {
        WikiMediator m = new WikiMediator(10, 10000);
        WikiMediatorServer server1 = new WikiMediatorServer(port + 5, 5, m);
        WikiMediatorClient client = new WikiMediatorClient(localhost, port + 5);

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {

                server1.serve();
                System.out.println("AAAAAAAAAAA");
            }
        });
        serverThread.start();

        JsonObject json = new JsonObject();
        search(json, "1", "Pegasus", 10);
        for (int i = 0; i < 20; i++) {
            sendRequestAndResetJson(json, client);
        }

        for (int i = 0; i < 20; i++) {
            System.out.println(client.getReply());
        }

        json = new JsonObject();
        addId(json, "1");
        json.addProperty("type", "stop");
        client.sendRequest(json);

        System.out.println(client.getReply());
        client.close();
        Thread.sleep(5000);

        WikiMediatorServer server2 = new WikiMediatorServer(port + 10, 5, new WikiMediator(10, 10000));
        client = new WikiMediatorClient(localhost, port + 10);

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {

                server2.serve();
                System.out.println("AAAAAAAAAAA");
            }
        });
        serverThread.start();

        Thread.sleep(500);

        json = new JsonObject();
        zeitgeist(json, "3", 5);
        sendRequestAndResetJson(json, client);

        System.out.println(client.getReply());
    }

    @Test
    public void testAllActions() throws IOException, InterruptedException {
        File deleteFile = new File("./local/pastSearchGetRequests.ser");
        deleteFile.delete();
        deleteFile = new File("./local/requestTimes.ser");
        deleteFile.delete();

        WikiMediator mediator = new WikiMediator(20, 100);
        WikiMediatorServer server = new WikiMediatorServer(port, 5, mediator);
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {

                server.serve();
                System.out.println("AAAAAAAAAAA");
            }
        });
        serverThread.start();
        Thread.sleep(5000);
        WikiMediatorClient client = new WikiMediatorClient(localhost, port);

        JsonObject json = new JsonObject();
        search(json, "1", "Archfiend", 10);
        sendRequestAndResetJson(json, client);

        json = new JsonObject();
        getPage(json, "2", "Elephant");
        sendRequestAndResetJson(json, client);

        json = new JsonObject();
        zeitgeist(json, "3", 5);
        sendRequestAndResetJson(json, client);

        json = new JsonObject();
        trending(json, "4", 5, 5);
        sendRequestAndResetJson(json, client);

        json = new JsonObject();
        windowedPeakLoad(json, "5", 10);
        sendRequestAndResetJson(json, client);

        shortestPath(json, "6", "Philosophy", "Barack Obama", 180);
        sendRequestAndResetJson(json, client);

        json = new JsonObject();
        addId(json, "7");
        json.addProperty("type", "stop");
        client.sendRequest(json);
        System.out.println(jsonConverter.toJson(json) + "\n");


        for (int i = 0; i < 6; i++) {
            String output = client.getReply();
            System.out.println(output);
            System.out.println("Awdfesgafwesgeafegsfsfadbfsgfgxbdfbgdbg");
        }
        System.out.println("BBBBBBBBBBBBBBBBBBBBBBBB");
        client.close();
    }

    @Test
    public void tooManyClients() throws IOException, InterruptedException {

        File deleteFile = new File("./local/pastSearchGetRequests.ser");
        deleteFile.delete();
        deleteFile = new File("./local/requestTimes.ser");
        deleteFile.delete();

        WikiMediator mediator = new WikiMediator(20, 100);
        WikiMediatorServer server = new WikiMediatorServer(port+1, 5, mediator);
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {

                server.serve();
                System.out.println("AAAAAAAAAAA");
            }
        });
        serverThread.start();
        Thread.sleep(5000);

        JsonObject json = new JsonObject();
        json.addProperty("timeout", 10);
        shortestPath(json, "6", "Philosophy", "Barack Obama", 2);

        ArrayList<WikiMediatorClient> clientList = new ArrayList<>();
        for ( int i = 0; i < 20; i++) {
            addId(json, Integer.toString(i));
            WikiMediatorClient client = new WikiMediatorClient(localhost, port+1);
            clientList.add(client);
            client.sendRequest(json);
        }

        clientList.parallelStream().forEach(client -> {
            try {
                System.out.println(client.getReply() + "\n" + System.currentTimeMillis());
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    @Test
    public void testTimeOut() throws IOException, InterruptedException {
        File deleteFile = new File("./local/pastSearchGetRequests.ser");
        deleteFile.delete();
        deleteFile = new File("./local/requestTimes.ser");
        deleteFile.delete();

        WikiMediator mediator = new WikiMediator(20, 100);
        WikiMediatorServer server = new WikiMediatorServer(port+2, 5, mediator);
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {

                server.serve();
                System.out.println("AAAAAAAAAAA");
            }
        });
        serverThread.start();
        Thread.sleep(5000);

        WikiMediatorClient client = new WikiMediatorClient(localhost, port+2);

        JsonObject json = new JsonObject();
        json.addProperty("timeout", 10);
        shortestPath(json, "6", "Philosophy", "Barack Obama", 2);
        sendRequestAndResetJson(json, client);

        json = new JsonObject();
        shortestPath(json, "6", "Philosophy", "Barack Obama", 3);
        sendRequestAndResetJson(json, client);

        json = new JsonObject();
        getPage(json, "5", "Philosophy");
        json.addProperty("timeout", 0);
        sendRequestAndResetJson(json, client);

        json = new JsonObject();
        addId(json, "7");
        json.addProperty("type", "stop");
        client.sendRequest(json);
        System.out.println(jsonConverter.toJson(json) + "\n");

        for (int i = 0; i < 4; i++) {
            String output = client.getReply();
            System.out.println(output);
            System.out.println("Awdfesgafwesgeafegsfsfadbfsgfgxbdfbgdbg");
        }
        System.out.println("BBBBBBBBBBBBBBBBBBBBBBBB");
        client.close();
    }

    private void windowedPeakLoad(JsonObject json, String id, int timeWindowInSeconds) {
        addId(json, id);
        json.addProperty("type", "windowedPeakLoad");
        if (timeWindowInSeconds >= 0) {
            json.addProperty("timeWindowInSeconds", timeWindowInSeconds);
        }
    }

    private void trending(JsonObject json, String id, int timeLimitInSeconds, int maxItems) {
        addId(json, id);
        json.addProperty("type", "trending");
        json.addProperty("timeLimitInSeconds", timeLimitInSeconds);
        json.addProperty("maxItems", maxItems);
    }

    private void getPage(JsonObject json, String id, String pageTitle) {
        addId(json, id);
        json.addProperty("type", "getPage");
        json.addProperty("pageTitle", pageTitle);
    }

    private void search(JsonObject json, String id, String query, int limit) {
        addId(json, id);
        json.addProperty("type", "search");
        json.addProperty("query", query);
        json.addProperty("limit", limit);
    }

    private void zeitgeist(JsonObject json, String id, int limit) {
        addId(json, id);
        json.addProperty("type", "zeitgeist");
        json.addProperty("limit", limit);
    }

    private void shortestPath(JsonObject json, String id, String pageTitle1, String pageTitle2, int timeout) {
        addId(json, id);
        json.addProperty("type", "shortestPath");
        json.addProperty("pageTitle1", pageTitle1);
        json.addProperty("pageTitle2", pageTitle2);
        json.addProperty("timeout", timeout);
    }

    private void addId(JsonObject json, String id) {
        json.addProperty("id", id);
    }

    private void sendRequestAndResetJson(JsonObject json, WikiMediatorClient client) throws IOException {
        client.sendRequest(json);
        System.out.println(jsonConverter.toJson(json) + "\n");
    }
}
