package org.openstatic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.DNSRecord.IPv4Address;

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

public class ControlBoxConnection implements Runnable {
    private WebSocketClient upstreamClient;
    private WebSocketSession session;
    private EventsWebSocket socket;
    private String websocketUri;
    private boolean stayConnected;
    private Thread keepAliveThread;
    private static ArrayList<JmDNS> jmdns;
    private static ControlBoxConnection connection;
    private PropertyChangeSupport propertyChangeSupport;
    private JSONObject properties;
    private static ArrayList<PropertyChangeListener> listeners;
    private static String[] lastLines = new String[4];

    public static void main(String[] args) {
        ControlBoxConnection.init();
        ControlBoxConnection.addStaticPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                System.err.println(evt.toString());
            }
            
        });
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

    public static void addStaticPropertyChangeListener(PropertyChangeListener pcl)
    {
        if (!ControlBoxConnection.listeners.contains(pcl))
        {
            ControlBoxConnection.listeners.add(pcl);
            if (ControlBoxConnection.connection != null)
            {
                ControlBoxConnection.connection.addPropertyChangeListener(pcl);
            }
        }
    }

    public static void displayTextLine(int num, String text)
    {
        if (num >=1 && num <=4)
        {
            if (text == null) text = "                    ";
            if (!text.equals(ControlBoxConnection.lastLines[num-1]))
            {
                ControlBoxConnection.lastLines[num-1] = text;
                JSONObject jo = new JSONObject();
                jo.put("line" + String.valueOf(num), text);
                ControlBoxConnection.transmit(jo);
            }
        }
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
                } else if (!ControlBoxConnection.connection.getWSUri().equals(url)) {
                    reconnect = true;
                }
                if (reconnect)
                {
                    ControlBoxConnection.connection = new ControlBoxConnection(url, ControlBoxConnection.listeners);
                    ControlBoxConnection.connection.connect();
                }
            } else {
               System.err.println("not mine: " + serviceInfo.getName()); 
            }
        }
    }

    public String getWSUri()
    {
        return this.websocketUri;
    }

    public static void shutDownMDNS()
    {
        if (ControlBoxConnection.jmdns != null)
        {
            System.err.println("Please Wait for mDNS to unregister....");
            ControlBoxConnection.jmdns.forEach((mDNS) -> {
                try
                {
                    System.err.println("Unregister " + mDNS.getInetAddress().toString());
                    mDNS.unregisterAllServices();
                    mDNS.close();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            });
        }
    }

    public static void init()
    {
        try
        {
            if (ControlBoxConnection.jmdns == null)
            {
                ControlBoxConnection.jmdns = new ArrayList<JmDNS>();
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                for (NetworkInterface netint : Collections.list(nets))
                {
                    //System.err.println("Interface: " + netint.getDisplayName());
                    // Create a JmDNS instance
                    Enumeration<InetAddress> addresses = netint.getInetAddresses();
                    Collections.list(addresses).forEach((address) -> {
                        if (address instanceof Inet4Address)
                        {
                            try
                            {
                                //System.err.println("Address: " + address.toString());
                                JmDNS jm = JmDNS.create(address);
                                jm.addServiceListener("_ws._tcp.local.", new ControlBoxSearch());
                                ControlBoxConnection.jmdns.add(jm);
                                System.err.println("Created JMDNS for (" + address.toString() + ")!");
                            } catch (Exception e) {
                                //e.printStackTrace();
                            }
                        }
                    });
                }
                Runtime.getRuntime().addShutdownHook(new Thread()
                {
                    public void run()
                    {
                        if (ControlBoxConnection.connection != null)
                        {
                            ControlBoxConnection.connection.stayConnected = false;
                        }
                        ControlBoxConnection.shutDownMDNS();
                    }
                });
            }
            if (ControlBoxConnection.listeners == null)
            {
                ControlBoxConnection.listeners = new ArrayList<PropertyChangeListener>();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ControlBoxConnection(String websocketUri, Collection<PropertyChangeListener> listeners) {
        this.websocketUri = websocketUri;
        this.stayConnected = true;
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        for(PropertyChangeListener pcl : listeners)
        {
            this.propertyChangeSupport.addPropertyChangeListener(pcl);
        }
        this.properties = new JSONObject();
    }

    @Override
    public void run() {
        while (this.keepAliveThread != null && ControlBoxConnection.connection == this) {
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
        if (ControlBoxConnection.connection != this)
        {
            this.close();
        }
    }

    public void handleWebSocketEvent(JSONObject j) {
        //System.err.println(j.toString());
        Set<String> objectFieldSet = j.keySet();
        Iterator<String> iterator = objectFieldSet.iterator();
        while (iterator.hasNext()) {
            String propertyName = iterator.next();
            Object propertyValue = j.opt(propertyName);
            //System.err.println("Fire prop change " + propertyName + " - " + propertyValue);
            this.propertyChangeSupport.firePropertyChange(propertyName, this.properties.opt(propertyName), propertyValue);
            this.properties.put(propertyName, propertyValue);
        }
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
            ControlBoxConnection.lastLines = new String[4];
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
            System.err.println("ControlBox X-MIT: " + jo.toString());
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

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
