package eu.chargetime.ocpp;

import eu.chargetime.ocpp.v1_6.WebSocketConnection;

import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Thomas Volden on 20-Apr-16.
 */
public class Client
{
    private String CALLFORMAT = "[2,\"%s\",\"%s\",{%s}]";

    private WebSocketConnection webSocketConnection;
    private Queue queue;
    private HashMap<String, CompletableFuture<String>> promises;

    public Client() {
        promises = new HashMap<>();
    }

    public void connect(String uri) throws Exception
    {
        queue = new Queue();
        Transmitter transmitter = new Transmitter()
        {
            @Override
            public void receivedMessage(String s) {
                System.out.println("ChargePoint   - Message received: " + s);
                String id = getUniqueId(s);
                promises.get(id).complete(queue.restoreRequest(id));
            }

            @Override
            public void connected() { }

            @Override
            public void disconnected() { }
        };
        webSocketConnection = new WebSocketConnection(URI.create(uri), transmitter);
    }

    public void disconnect()
    {
        try {
            webSocketConnection.disconnect();
        } catch (Exception ex) {
            System.err.println(ex.getStackTrace());
        }
    }

    private String getUniqueId(String message)
    {
        if (message == null || "".equals(message))
            return "";

        String[] segments = message.substring(1, message.length()-1).split(",");
        return segments[1].substring(1, segments[1].length()-1);
    }

    public CompletableFuture<String> send(String request, String payload)
    {
        String id = queue.store(request);
        CompletableFuture<String> promis = new CompletableFuture<>();
        promises.put(id, promis);
        webSocketConnection.sendMessage(String.format(CALLFORMAT, id, request, payload));
        return promis;
    }
}
