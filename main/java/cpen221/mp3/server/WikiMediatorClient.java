package cpen221.mp3.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;

/**
 * Class used for testing connections to WikiMediatorServer
 */
public class WikiMediatorClient {
    private final static Gson jsonConverter = new Gson();
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public WikiMediatorClient(String hostname, int port) throws IOException {
        socket = new Socket(hostname, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void sendRequest(JsonObject json) {
        out.println(jsonConverter.toJson(json));
        out.flush();
    }

    public String getReply() throws IOException {
        String reply = in.readLine();
        if (reply == null) {
            throw new IOException("connection terminated unexpectedly");
        }
        return new String(reply);
    }

    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}
