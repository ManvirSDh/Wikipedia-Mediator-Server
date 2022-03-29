package cpen221.mp3.server;

import cpen221.mp3.wikimediator.WikiMediator;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/*
 * Representation Invariants (RI):
 * maxClients >= 0      ->  maxNumbers of requests must be non-negative
 *
 * Abstraction Function (AF):
 * WikiMediatorServer accepts requests from a network socket and passes them to a WikiMediator. It then sends
 * the responses to the network socket.
 * All clients receive their own thread, and all requests/responses are represented as JSONs.
 *
 * Running represents the state of the server, and is changed when the server shuts down and stops
 * accepting connections.
 * serverSocket is the ServerSocket through which the server communicates with its clients.
 * maxClients is the maximum numbers of clients the server can respons to at once.
 * mediator is the WikiMediator service through which all the client requests are fed,
 * and from which all the responses come.
 */

public class WikiMediatorServer {
    private static final Gson jsonConverter = new Gson();

    boolean running;
    private final ServerSocket serverSocket;
    private final int maxClients;
    private final WikiMediator mediator;


    /**
     * Start a server at a given port number, with the ability to process
     * up to n requests concurrently.
     *
     * @param port the port number to bind the server to, 9000 <= {@code port} <= 9999
     * @param n the number of concurrent requests the server can handle, 0 < {@code n} <= 32
     * @param wikiMediator the WikiMediator instance to use for the server, {@code wikiMediator} is not {@code null}
     */
    public WikiMediatorServer(int port, int n, WikiMediator wikiMediator) {
        maxClients = n;
        mediator = wikiMediator;
        running = true;

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException io) {
            throw new RuntimeException("Server failed to initialize");
        }
    }

    /**
     * Conducts all server business, until the server is told to shut down.
     */
    public void serve() {
        mediator.readStorage();
        ExecutorService handler = Executors.newFixedThreadPool(maxClients);
        while (running) {
            try {
                final Socket socket = serverSocket.accept();
                Thread h = new Thread(() -> {
                    {
                        try {
                            handle(socket);
                        } finally {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                throw new RuntimeException("Error in handling connection.");
                            }
                        }
                    }
                });
                handler.execute(h);

            }   catch (IOException io) {
                io.printStackTrace();
                throw new RuntimeException("Error in accepting connection.");
            }
        }
        mediator.storeRequests();
        handler.shutdown();
        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException io) {
                io.printStackTrace();
                throw new RuntimeException("Error in closing connection.");
            }
        }
    }

    /**
     * Handles a single client, and responds to their requests.
     * @param socket    The socket through which the client will
     *                  communicate.
     */
    private void handle(Socket socket) {
        try {

            try (socket; BufferedReader in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream())); PrintWriter out = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream()), true)) {
                String request;
                while ((request = in.readLine()) != null) {

                    JsonObject json = jsonConverter.fromJson(request, JsonObject.class);
                    JsonObject response = new JsonObject();

                    ExecutorService timerExecutor = Executors.newSingleThreadExecutor();
                    Future<Void> future = timerExecutor.submit(() -> {
                        createResponse(json, response);
                        return null;
                    });

                    response.add("id", json.get("id"));

                    try {
                        if (json.has("timeout")) {
                            future.get(json.get("timeout").getAsInt(), TimeUnit.SECONDS);
                        } else {
                            future.get();
                        }
                    } catch (TimeoutException e) {
                        addErrorMessage(response);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Error in creating response.");
                    }

                    out.println(jsonConverter.toJson(response));
                    if (response.has("response") && response.get("response").getAsString().equals("bye")) {
                        running = false;
                        serverSocket.close();
                    }
                }
            }
        } catch (IOException io) {
            io.printStackTrace();
            throw new RuntimeException("Error in handling client");
        }

    }

    /**
     * Given a request, create the appropriate response by using a
     * WikiMediator Service
     * @param json  client request
     * @param response  response to client
     */
    private void createResponse(JsonObject json, JsonObject response) {
        String result;
        boolean successful = false;

        try {
            switch (json.get("type").getAsString()) {
                case "search":
                    result = mediator.search(
                                    json.get("query").getAsString(),
                                    json.get("limit").getAsInt()).toString();
                    successful = true;
                    break;

                case "getPage":
                    result = mediator.getPage(
                            json.get("pageTitle").getAsString()
                    );
                    successful = true;
                    break;

                case "zeitgeist":
                    result = mediator.zeitgeist(
                            json.get("limit").getAsInt()
                    ).toString();
                    successful = true;
                    break;

                case "trending":
                    result = mediator.trending(
                            json.get("timeLimitInSeconds").getAsInt(),
                            json.get("maxItems").getAsInt()
                    ).toString();
                    successful = true;
                    break;

                case "windowedPeakLoad":
                    if (json.has("timeWindowInSeconds")) {
                        result = Integer.toString(mediator.windowedPeakLoad(
                                json.get("timeWindowInSeconds").getAsInt()));
                    } else {
                        result = Integer.toString(mediator.windowedPeakLoad());
                    }
                    successful = true;
                    break;

                case "shortestPath":
                    try {
                        result = mediator.shortestPath(
                                json.get("pageTitle1").getAsString(),
                                json.get("pageTitle2").getAsString(),
                                json.get("timeout").getAsInt()
                        ).toString();
                        successful = true;
                    } catch (TimeoutException e) {
                        result =  "Operation timed out";
                        addErrorMessage(response);
                    }
                    break;

                case "stop":
                    result = "bye";
                    break;
                default:
                    result = "Operation not recognized.";
                    response.addProperty("status", "failed");
            }
        } catch (NullPointerException np) {
            result = "Error in parsing request";
            response.addProperty("status", "failed");
        }
        response.addProperty("response", result);
        if (successful) {
            response.addProperty("status", "success");
        }
    }

    /**
     * Add a timeout error message to response.
     * @param response  Response to which the error message
     *                  should be added.
     */
    private void addErrorMessage (JsonObject response) {
        response.addProperty("status", "failed");
        response.addProperty("response", "Operation timed out.");
    }
}
