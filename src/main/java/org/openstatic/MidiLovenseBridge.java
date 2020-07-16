package org.openstatic;

import javax.sound.midi.*;
import java.util.Enumeration;
import java.util.StringTokenizer;

import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JSlider;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.TitledBorder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Graphics;
import java.awt.Desktop;


import org.apache.commons.lang3.StringUtils;

import org.json.*;
import org.openstatic.lovense.*;
import org.openstatic.lovense.swing.*;
import org.openstatic.midi.*;

public class MidiLovenseBridge extends JFrame implements Runnable, ChangeListener, Receiver, LovenseConnectListener, ActionListener, LovenseToyListener, MidiPortListener
{
    protected JList<LovenseToy> toyList;
    private JList<MidiPort> midiList;
    private JList<MidiRelayRule> rulesList;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenuItem aboutMenuItem;
    private JMenuItem exitMenuItem;
    private JMenuItem addIpItem;
    private JTabbedPane tabbed;
    private LoggerMidiPort logger;
    private RTPMidiPort rtpMidiPort;
    private LovenseToyListModel lovenseToyListModel;
    protected LovenseToyCellRenderer lovenseToyRenderer;
    private MidiPortCellRenderer midiRenderer;
    private MidiPortListModel midiListModel;
    private MidiRelayRuleCellRenderer ruleRenderer;
    private Thread mainThread;
    private Thread publishToyStatusThread;
    protected DefaultListModel<MidiRelayRule> rules;
    public static MidiLovenseBridge instance;
    private boolean keep_running;
    private long lastToyClick;
    private JSONObject options;
    private MidiRandomizerPort randomizerPort;
    private JButton panicButton;
    private long lastRuleClick;
    private JSlider powerSlider;
    private ImageIcon gears;
    private String hostname;

    public MidiLovenseBridge()
    {
        super("Midi Lovense Bridge");
        InetAddress ip;
        try 
        {
            ip = InetAddress.getLocalHost();
            this.hostname = ip.getHostName();
        } catch (Exception e) {}
        this.logger = new LoggerMidiPort("Logger");
        this.logger.open();
        this.randomizerPort = new MidiRandomizerPort("Randomizer");
        MidiPortManager.addMidiPortListener(this);
        MidiPortManager.registerVirtualPort("random", this.randomizerPort);
        this.rtpMidiPort = new RTPMidiPort("RTP Network", "MidiLovenseBridge" + hostname , 5014);
        MidiPortManager.registerVirtualPort("rtp", this.rtpMidiPort);

        JSONObject rougeModWheel = MidiRandomizerPort.defaultRuleJSONObject();
        rougeModWheel.put("channel", 1);
        rougeModWheel.put("cc", 1);
        rougeModWheel.put("min", 0);
        rougeModWheel.put("max", 127);
        rougeModWheel.put("smooth", true);
        rougeModWheel.put("changeDelay", 2);
        this.randomizerPort.addRandomRule(rougeModWheel);

        JSONObject footPedal = MidiRandomizerPort.defaultRuleJSONObject();
        footPedal.put("channel", 1);
        footPedal.put("cc", 2);
        footPedal.put("min", 0);
        footPedal.put("max", 127);
        footPedal.put("smooth", false);
        footPedal.put("changeDelay", 5);
        this.randomizerPort.addRandomRule(footPedal);

        JSONObject footPedal2 = MidiRandomizerPort.defaultRuleJSONObject();
        footPedal2.put("channel", 1);
        footPedal2.put("cc", 3);
        footPedal2.put("min", 0);
        footPedal2.put("max", 127);
        footPedal2.put("smooth", true);
        footPedal2.put("changeDelay", 15);
        this.randomizerPort.addRandomRule(footPedal2);

        MidiPortManager.init();
        this.keep_running = true;
        this.options = new JSONObject();

        MidiLovenseBridge.instance = this;
        centerWindow();
        this.setLayout(new BorderLayout());
        try
        {
            BufferedImage windowIcon = ImageIO.read(getClass().getResource("/windows.png"));
            this.gears = new ImageIcon(getClass().getResource("/gears.gif"));
            this.setIconImage(windowIcon);
        } catch (Exception iconException) {}
        
        this.menuBar = new JMenuBar();
        
        this.fileMenu = new JMenu("File");
        this.fileMenu.setMnemonic(KeyEvent.VK_F);
        
        this.exitMenuItem = new JMenuItem("Exit");
        this.exitMenuItem.setMnemonic(KeyEvent.VK_X);
        this.exitMenuItem.addActionListener(this);
        this.exitMenuItem.setActionCommand("exit");
        
        this.addIpItem = new JMenuItem("Enter IP/Port Manually");
        this.addIpItem.setMnemonic(KeyEvent.VK_I);
        this.addIpItem.addActionListener(this);
        this.addIpItem.setActionCommand("ipman");
        
        this.aboutMenuItem = new JMenuItem("About");
        this.aboutMenuItem.setMnemonic(KeyEvent.VK_A);
        this.aboutMenuItem.addActionListener(this);
        this.aboutMenuItem.setActionCommand("about");
        
        this.fileMenu.add(this.aboutMenuItem);
        this.fileMenu.add(this.addIpItem);
        this.fileMenu.add(this.exitMenuItem);
        
        this.menuBar.add(this.fileMenu);
        
        this.setJMenuBar(this.menuBar);
        
        this.rules = new DefaultListModel<MidiRelayRule>();
        this.lovenseToyRenderer = new LovenseToyCellRenderer();
        LovenseConnect.addLovenseConnectListener(this);
        this.lovenseToyListModel = new LovenseToyListModel();

        // Setup toy list
        this.toyList = new JList<LovenseToy>(this.lovenseToyListModel)
        {
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                int toyCount = lovenseToyListModel.getSize();
                if (toyCount == 0)
                {
                    int iconHeight = MidiLovenseBridge.this.gears.getIconHeight();
                    int x = (this.getWidth() - MidiLovenseBridge.this.gears.getIconWidth()) / 2;
                    int y = (this.getHeight() - iconHeight) / 2;
                    MidiLovenseBridge.this.gears.paintIcon(this, g, x, y);
                    g.drawString("Searching for LovenseConnect", x -10, y + iconHeight + 20);
                }
            }
        };
        this.toyList.setCellRenderer(this.lovenseToyRenderer);
        this.toyList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               long cms = System.currentTimeMillis();
               if (cms - MidiLovenseBridge.this.lastToyClick < 500 && MidiLovenseBridge.this.lastToyClick > 0)
               {
                   int index = MidiLovenseBridge.this.toyList.locationToIndex(e.getPoint());
                   if (index != -1)
                   {
                      LovenseToy t = (LovenseToy) MidiLovenseBridge.this.toyList.getSelectedValue();
                      MidiRelayRule newRule = new MidiRelayRule(0, ShortMessage.CONTROL_CHANGE, 0, t);
                      MidiRelayRuleEditor editor = new MidiRelayRuleEditor(newRule, true);
                   }
               }
               MidiLovenseBridge.this.lastToyClick = cms;
            }
        });
        JScrollPane lovenseToyScrollPane = new JScrollPane(this.toyList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        //lovenseToyScrollPane.setBorder(new TitledBorder("Lovense Toys (double-click to create rule)"));
        JPanel toysAndPower = new JPanel(new BorderLayout());
        this.powerSlider = new JSlider(JSlider.VERTICAL, 0, 20, 0);
        powerSlider.setBorder(new TitledBorder("Vibrate"));
        powerSlider.addChangeListener(this);
        powerSlider.setPreferredSize(new Dimension(60, 0));
        toysAndPower.add(lovenseToyScrollPane, BorderLayout.CENTER);

        this.panicButton = new JButton("PANIC!");
        this.panicButton.addActionListener(this);
        this.panicButton.setActionCommand("panic");

        JPanel powerAndPanic = new JPanel(new BorderLayout());
        powerAndPanic.add(powerSlider, BorderLayout.CENTER);
        powerAndPanic.add(this.panicButton, BorderLayout.PAGE_END);

        toysAndPower.add(powerAndPanic, BorderLayout.EAST);

        this.ruleRenderer = new MidiRelayRuleCellRenderer();

        // Setup rule list
        this.rulesList = new JList<MidiRelayRule>(this.rules);
        this.rulesList.setCellRenderer(this.ruleRenderer);
        JScrollPane ruleScrollPane = new JScrollPane(this.rulesList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ruleScrollPane.setBorder(new TitledBorder("Rules for Incoming MIDI Messages (double-click to toggle, right click to edit)"));
        this.rulesList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               int index = MidiLovenseBridge.this.rulesList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                   MidiRelayRule source = (MidiRelayRule) MidiLovenseBridge.this.rules.getElementAt(index);
                   if (e.getButton() == MouseEvent.BUTTON1)
                   {
                       long cms = System.currentTimeMillis();
                       if (cms - MidiLovenseBridge.this.lastRuleClick < 500 && MidiLovenseBridge.this.lastRuleClick > 0)
                       {
                          source.toggleEnabled();
                       }
                       MidiLovenseBridge.this.lastRuleClick = cms;
                   } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                      MidiRelayRuleEditor editor = new MidiRelayRuleEditor(source);
                   }
                   MidiLovenseBridge.repaintRules();
               }
            }
        });
        this.tabbed = new JTabbedPane();
        this.tabbed.addTab("Lovense Toys", toysAndPower);
        this.tabbed.addTab("Logger", this.logger);
        this.add(this.tabbed, BorderLayout.CENTER);
        this.add(ruleScrollPane, BorderLayout.PAGE_END);

        this.midiListModel = new MidiPortListModel();
        this.midiRenderer = new MidiPortCellRenderer();
        this.midiList = new JList<MidiPort>(this.midiListModel);
        this.midiList.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
               int index = MidiLovenseBridge.this.midiList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                  MidiPort source = (MidiPort) MidiLovenseBridge.this.midiListModel.getElementAt(index);
                  if (source.isOpened())
                  {
                      source.close();
                      source.removeReceiver(MidiLovenseBridge.this);
                  } else {
                      source.open();
                      source.addReceiver(MidiLovenseBridge.this);
                  }
                  repaint();
               }
            }
        });
        this.midiList.setCellRenderer(this.midiRenderer);
        JScrollPane scrollPane2 = new JScrollPane(this.midiList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane2.setBorder(new TitledBorder("MIDI Devices"));
        this.add(scrollPane2, BorderLayout.WEST);


        this.mainThread = new Thread(this);
        this.mainThread.setDaemon(true);
        this.mainThread.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread() 
        { 
          public void run() 
          { 
            MidiLovenseBridge.this.keep_running = false;
            System.out.println("Shutdown Hook is running!"); 
            saveConfig();
          } 
        }); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loadConfig();
    }

    public void portAdded(int idx, MidiPort port)
    {
        this.logger.println("MIDI Port Added " + port.getName());
        if (port.canTransmitMessages())
            port.addReceiver(this.logger);
    }
    
    public void portRemoved(int idx, MidiPort port)
    {
        this.logger.println("MIDI Port Removed " + port.getName());
        port.removeReceiver(this.logger);
    }
    
    public void portOpened(MidiPort port)
    {
        this.logger.println("MIDI Port Opened " + port.getName());
    }
    
    public void portClosed(MidiPort port)
    {
        this.logger.println("MIDI Port Closed " + port.getName());
    }
    
    public void mappingAdded(int idx, MidiPortMapping mapping)
    {
        
    }
    
    public void mappingRemoved(int idx, MidiPortMapping mapping)
    {
        
    }
    
    public void mappingOpened(MidiPortMapping mapping)
    {
        
    }
    
    public void mappingClosed(MidiPortMapping mapping)
    {
        
    }

    public static void repaintToys()
    {
        Thread t = new Thread(() -> MidiLovenseBridge.instance.toyList.repaint());
        t.start();
    }

    public static void repaintRules()
    {
        Thread t = new Thread(() -> MidiLovenseBridge.instance.rulesList.repaint());
        t.start();
    }

    protected static String noteNumberToString(int i)
    {
        String[] noteString = new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return noteString[i%12] + String.valueOf( ((int)Math.floor(((float)i)/12f)) - 2);
    }

    public void stateChanged(ChangeEvent e)
    {
        JSlider source = (JSlider)e.getSource();
        if (!source.getValueIsAdjusting())
        {
            final int v = (int)source.getValue();
            (new Thread(() -> {
                try
                {
                    LovenseToy toy = (LovenseToy) MidiLovenseBridge.this.toyList.getSelectedValue();
                    toy.vibrate(v);
                    MidiLovenseBridge.repaintToys();
                } catch (Exception e2) {

                }
            })).start();;
        }
    }

    public void run()
    {
        while(this.keep_running)
        {
            try
            {
                LovenseConnect.refreshIfNeeded();
            } catch (Exception e) {
                //e.printStackTrace(System.err);
            }
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand();
        if (cmd.equals("exit")) {
            System.exit(0);
        } else if (cmd.equals("about")) {
            browseTo("http://openstatic.org/lovense/");
        } else if (cmd.equals("ipman")) {
            String ip_port = JOptionPane.showInputDialog("Please enter an ip and httpsPort for Lovense Connect\n(Example: 127.0.0.1:30110)\nIt may take a few moments for your toys to be found");
            if (ip_port != null)
            {
                StringTokenizer st = new StringTokenizer(ip_port,":");
                String ip = st.nextToken();
                if (st.hasMoreTokens())
                {
                    int port = Integer.valueOf(st.nextToken());
                    LovenseConnect.addDeviceManually(ip, port);
                }
            }
        } else if (cmd.equals("panic")) {
            try
            {
                this.powerSlider.setValue(0);
                for (Enumeration<MidiRelayRule> mrre = this.rules.elements(); mrre.hasMoreElements();)
                {
                    MidiRelayRule mrr = mrre.nextElement();
                    mrr.setEnabled(false);
                }
                MidiLovenseBridge.repaintRules();
                LovenseConnect.getToys().forEach((toy) -> {
                    toy.vibrate(0);
                });
            } catch (Exception e2) {
                
            }
            
        }
    }
    
    public static boolean browseTo(String url)
    {
        try
        {
            Desktop dt = Desktop.getDesktop();
            dt.browse(new URI(url));
            return true;
        } catch (Exception dt_ex) {
            return false;
        }
    }

    public void centerWindow()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final float WIDTH = screenSize.width;
        final float HEIGHT = screenSize.height;
        int wWidth = 800;
        int wHeight = 600;
        Dimension d = new Dimension(wWidth, wHeight);
        this.setSize(d);
        //this.setMaximumSize(d);
        this.setMinimumSize(d);
        //this.setResizable(false);
        int x = (int) ((WIDTH/2f) - ( ((float)wWidth) /2f ));
        int y = (int) ((HEIGHT/2f) - ( ((float)wHeight) /2f ));
        this.setLocation(x, y);
    }

    public static void main(String[] args)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            
        }
        LovenseConnect.setDebug(true);
        MidiLovenseBridge mlb = new MidiLovenseBridge();
        mlb.setVisible(true);
    }

    public void toyAdded(int idx, LovenseToy toy)
    {
        toy.addLovenseToyListener(this);
        this.logger.println("Toy Added " + toy.getNickname());
    }

    public void toyRemoved(int idx, LovenseToy toy)
    {
        toy.removeLovenseToyListener(this);
        this.logger.println("Toy Removed " + toy.getNickname());
    }
    
    public void toyUpdated(LovenseToy toy)
    {
        publishToyStatus(toy);
        MidiLovenseBridge.repaintToys();
    }

    // Example: http://controlbox.lan/display?line{{toy.index}}={{toy.nickname}}%20{{toy.battery})%20{{toy.output1}}
    public void publishToyStatus(LovenseToy toy)
    {
        if (this.options.has("publishStatusUrl"))
        {
            try
            {
                String nickname = toy.getNickname();
                int nickname_length = nickname.length();
                
                String publishStatusUrl = this.options.getString("publishStatusUrl");
                publishStatusUrl = publishStatusUrl.replaceAll("\\{\\{toy.index\\}\\}", String.valueOf(LovenseConnect.toyIndex(toy)+1));
                publishStatusUrl = publishStatusUrl.replaceAll("\\{\\{toy.name\\}\\}", URLEncoder.encode(toy.getName(), "UTF-8"));
                publishStatusUrl = publishStatusUrl.replaceAll("\\{\\{toy.nickname\\}\\}", URLEncoder.encode(nickname, "UTF-8"));
                int batt = toy.getBattery();
                if (batt < 0) batt = 0;
                if (batt > 100) batt = 100;
                publishStatusUrl = publishStatusUrl.replaceAll("\\{\\{toy.battery\\}\\}", URLEncoder.encode(String.valueOf(batt) + "%", "UTF-8"));
                publishStatusUrl = publishStatusUrl.replaceAll("\\{\\{toy.output1\\}\\}", URLEncoder.encode(String.valueOf(toy.getOutputOneValue()), "UTF-8"));
                publishStatusUrl = publishStatusUrl.replaceAll("\\{\\{toy.output2\\}\\}", URLEncoder.encode(String.valueOf(toy.getOutputTwoValue()), "UTF-8"));
                
                String shortStatus = StringUtils.rightPad(StringUtils.abbreviate(nickname, 8), 8) + StringUtils.leftPad(String.valueOf(batt) + "%", 4) + StringUtils.leftPad(String.valueOf(toy.getOutputOneValue()),4) + StringUtils.leftPad(String.valueOf(toy.getOutputTwoValue()),4);
                publishStatusUrl = publishStatusUrl.replaceAll("\\{\\{toy.shortStatus\\}\\}", URLEncoder.encode(shortStatus, "UTF-8"));

                PendingURLFetch puf = new PendingURLFetch(publishStatusUrl);
                boolean launch = true;
                if (this.publishToyStatusThread != null)
                {
                    if (this.publishToyStatusThread.isAlive())
                    {
                        launch = false;
                    }
                }
                if (launch)
                {
                    this.publishToyStatusThread = new Thread(puf);
                    this.publishToyStatusThread.start();
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
    
    private static String shortMessageToString(ShortMessage msg)
    {
        String channelText = "[CH=" + String.valueOf(msg.getChannel()+1) + "]";
        String commandText = "";
        String data1Name = "?";
        String data1Value = "?";
        
        if (msg.getCommand() == ShortMessage.CONTROL_CHANGE)
        {
            data1Name = "CC";
            data1Value = String.valueOf(msg.getData1());
            commandText = "CONTROL CHANGE";
        } else if (msg.getCommand() == ShortMessage.NOTE_ON) {
            data1Name = "NOTE";
            data1Value = MidiLovenseBridge.noteNumberToString(msg.getData1());
            commandText = "NOTE ON";
        } else if (msg.getCommand() == ShortMessage.NOTE_OFF) {
            data1Name = "NOTE";
            data1Value = MidiLovenseBridge.noteNumberToString(msg.getData1());
            commandText = "NOTE OFF";
        }
        String data1Text = "(" + data1Name + "=" + data1Value + ")";;
        return commandText + " " + channelText + " " + data1Text + " value=" + String.valueOf(msg.getData2());
    }

    // Receiver Method
    public void send(MidiMessage msg, long timeStamp)
    {
        if(msg instanceof ShortMessage)
        {
            final ShortMessage sm = (ShortMessage) msg;
            //System.err.println("Recieved Short Message Channel=" + String.valueOf(sm.getChannel()) + " CC=" + String.valueOf(sm.getData1()) + " value=" + String.valueOf(sm.getData2()));
            for (Enumeration<MidiRelayRule> mrre = this.rules.elements(); mrre.hasMoreElements();)
            {
                final MidiRelayRule mrr = mrre.nextElement();
                if (mrr.messageMatches(sm))
                {
                    //System.err.println(shortMessageToString(sm) + " = " + mrr.dataToVibrate(sm.getData2()));
                    try
                    {
                        (new Thread(() -> mrr.processMessage(sm))).start();
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        } else {
            System.err.println("Unknown non-short message " + msg.toString());
        }
    }

    public void close() {}

    public static File getConfigFile()
    {
        File homeDir = new File(System.getProperty("user.home"));
        File configFile = new File(homeDir, ".midi-lovense-bridge.json");
        System.err.println("Config File: " + configFile.toString());
        return configFile;
    }

    public void loadConfig()
    {
        try
        {
            JSONObject configJson = loadJSONObject(getConfigFile());
            System.err.println("Config: " + configJson.toString());
            if (configJson.has("rules"))
            {
                JSONArray rulesJson = configJson.getJSONArray("rules");
                for (int m = 0; m < rulesJson.length(); m++)
                {
                    JSONObject rjo = rulesJson.getJSONObject(m);
                    MidiRelayRule mrr = new MidiRelayRule(rjo);
                    this.rules.addElement(mrr);
                }
            }
            if (configJson.has("options"))
                this.options = configJson.getJSONObject("options");
            if (configJson.has("lovenseDevices"))
                LovenseConnect.addDevicesFromJSONArray(configJson.getJSONArray("lovenseDevices"));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void saveConfig()
    {
        try
        {
            JSONArray rules_ja = new JSONArray();
            for (Enumeration<MidiRelayRule> mrre = this.rules.elements(); mrre.hasMoreElements();)
            {
                MidiRelayRule mrr = mrre.nextElement();
                rules_ja.put(mrr.toJSONObject());
            }
            JSONObject configJson = new JSONObject();
            configJson.put("rules", rules_ja);
            configJson.put("options", this.options);
            configJson.put("lovenseDevices", LovenseConnect.getDevicesAsJSONArray());
            System.err.println("Config: " + configJson.toString());
            saveJSONObject(getConfigFile(), configJson);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public static JSONObject loadJSONObject(File file)
    {
        try
        {
            FileInputStream fis = new FileInputStream(file);
            StringBuilder builder = new StringBuilder();
            int ch;
            while((ch = fis.read()) != -1){
                builder.append((char)ch);
            }
            fis.close();
            JSONObject props = new JSONObject(builder.toString());
            return props;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static void saveJSONObject(File file, JSONObject obj)
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(file);
            PrintStream ps = new PrintStream(fos);
            ps.print(obj.toString());
            ps.close();
            fos.close();
        } catch (Exception e) {
        }
    }
}
