package org.openstatic;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.JSONObject;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class ControlBoxConnection implements Runnable
{
    private WebSocketClient upstreamClient;
    private WebSocketSession session;
    private EventsWebSocket socket;
    private String websocketUri;
    private boolean stayConnected;
    private Thread keepAliveThread;
    private static JmDNS jmdns;
    private static ControlBoxConnection connection;

    public static void main(String[] args)
    {
        ControlBoxConnection.init();
        try
        {
            while(true)
            {
                ControlBoxConnection.displayTextLine(1, "Hello World");
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void displayTextLine(int num, String text)
    {
        JSONObject jo = new JSONObject();
        jo.put("line" + String.valueOf(num), text);
        ControlBoxConnection.transmit(jo);
    }

    public static void backlight(boolean value)
    {
        JSONObject jo = new JSONObject();
        jo.put("backlight", value ? 1 : 0);
        ControlBoxConnection.transmit(jo);
    }

    private static class ControlBoxSearch implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("Service added: " + event.getInfo());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            ServiceInfo serviceInfo = event.getInfo();
            if (serviceInfo.getName().equals("controlbox"))
            {
                boolean reconnect = false;
                Inet4Address[] addresses = serviceInfo.getInet4Addresses();
                int port = serviceInfo.getPort();
                String addr = addresses[0].toString().substring(1);
                String url = "ws://" + addr + ":" + String.valueOf(port);
                System.out.println("Service resolved: " + url);
                if (ControlBoxConnection.connection == null)
                {
                    reconnect = true;
                } else if (!ControlBoxConnection.connection.isConnected()) {
                    reconnect = true;
                }
                if (reconnect)
                {
                    ControlBoxConnection.connection = new ControlBoxConnection(url);
                    ControlBoxConnection.connection.connect();
                }
            } else {
               System.err.println("not mine: " + serviceInfo.getName()); 
            }
        }
    }

    public static void shutDownMDNS()
    {
        if (ControlBoxConnection.jmdns != null)
        {
            System.err.println("Please Wait for mDNS to unregister....");
            try
            {
                ControlBoxConnection.jmdns.unregisterAllServices();
                ControlBoxConnection.jmdns.close();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public static void init()
    {
        try
        {
            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                public void run()
                {
                    ControlBoxConnection.shutDownMDNS();
                }
            });
            if (ControlBoxConnection.jmdns == null)
            {
                // Create a JmDNS instance
                ControlBoxConnection.jmdns = JmDNS.create(InetAddress.getLocalHost());

                // Add a service listener
                ControlBoxConnection.jmdns.addServiceListener("_ws._tcp.local.", new ControlBoxSearch());
                System.err.println("added service listener");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ControlBoxConnection(String websocketUri) {
        this.websocketUri = websocketUri;
        this.stayConnected = true;
    }

    @Override
    public void run() {
        while (this.keepAliveThread != null) {
            try {
                Thread.sleep(10000);
                if (this.isConnected()) {
                    JSONObject jo = new JSONObject();
                    jo.put("ts", System.currentTimeMillis());
                    this.send(jo);
                } else if (this.stayConnected) {
                    System.err.println("No connection detected by keep alive reconnecting...");
                    ControlBoxConnection.this.close();
                    ControlBoxConnection.this.session = null;
                    ControlBoxConnection.this.upstreamClient = null;
                    this.connect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void handleWebSocketEvent(JSONObject j) {
        System.err.println(j.toString());
    }

    public void connect() {
        SslContextFactory sec = new SslContextFactory.Client();
        sec.setValidateCerts(false);
        HttpClient httpClient = new HttpClient(sec);
        ControlBoxConnection.this.upstreamClient = new WebSocketClient(httpClient);
        try {
            ControlBoxConnection.this.socket = new EventsWebSocket();
            ControlBoxConnection.this.upstreamClient.start();
            URI upstreamUri = new URI(this.websocketUri);
            ControlBoxConnection.this.upstreamClient.connect(socket, upstreamUri, new ClientUpgradeRequest());
        } catch (Throwable t2) {
            System.err.println("Error on connect() URI: " + this.websocketUri);
            t2.printStackTrace(System.err);
        }
    }

    public void close() {
        if (this.upstreamClient != null) {
            try {
                this.upstreamClient.stop();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
    }

    public static class EventsWebSocketServlet extends WebSocketServlet {
        @Override
        public void configure(WebSocketServletFactory factory) {
            // System.err.println("Factory Initialized");
            // factory.getPolicy().setIdleTimeout(10000);
            factory.register(EventsWebSocket.class);
        }
    }

    @WebSocket
    public class EventsWebSocket {

        @OnWebSocketMessage
        public void onText(Session session, String message) throws IOException {
            try {
                JSONObject jo = new JSONObject(message);
                ControlBoxConnection.this.handleWebSocketEvent(jo);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        @OnWebSocketConnect
        public void onConnect(Session session) throws IOException {
            // System.err.println("Connected websocket");
            if (session instanceof WebSocketSession) {
                ControlBoxConnection.this.session = (WebSocketSession) session;
                if (ControlBoxConnection.this.keepAliveThread == null) {
                    ControlBoxConnection.this.keepAliveThread = new Thread(ControlBoxConnection.this);
                    ControlBoxConnection.this.keepAliveThread.start();
                }
            } else {
                // System.err.println("Not an instance of WebSocketSession");
            }
        }

        @OnWebSocketClose
        public void onClose(Session session, int status, String reason) {
            // System.err.println("Close websocket");
            ControlBoxConnection.this.close();
            ControlBoxConnection.this.session = null;
            ControlBoxConnection.this.upstreamClient = null;
            if (ControlBoxConnection.this.stayConnected) {
                System.err.println("Connection Closed - Auto Reconnect");
            } else {
                ControlBoxConnection.this.cleanUp();
            }
        }

        @OnWebSocketError
        public void onError(Throwable e) {
            System.err.println("Connection Error - websocket");
            e.printStackTrace(System.err);
            ControlBoxConnection.this.close();
            ControlBoxConnection.this.session = null;
            ControlBoxConnection.this.upstreamClient = null;
            if (ControlBoxConnection.this.stayConnected) {
                System.err.println("Auto Reconnect");
            } else {
                ControlBoxConnection.this.cleanUp();
            }
        }
    }

    public static void transmit(JSONObject jo) {
        if (ControlBoxConnection.connection != null)
        {
            ControlBoxConnection.connection.send(jo);
        }
    }

    public void send(JSONObject jo) {
        if (jo != null && this.session != null) {
            this.session.getRemote().sendStringByFuture(jo.toString());
        }
    }

    private void cleanUp() {
        ControlBoxConnection.this.keepAliveThread = null;
    }

    public boolean isConnected() {
        if (this.session != null) {
            return this.session.isOpen();
        } else if (this.upstreamClient != null) {
            return this.upstreamClient.isStarted();
        } else {
            return false;
        }
    }
}
