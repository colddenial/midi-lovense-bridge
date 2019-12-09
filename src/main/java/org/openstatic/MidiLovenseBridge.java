package org.openstatic;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Enumeration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.StringTokenizer;

import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JSlider;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.DefaultListModel;
import javax.swing.UIManager;
import javax.swing.ScrollPaneConstants;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.TitledBorder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Desktop;

import org.apache.commons.lang3.StringUtils;

import javax.sound.midi.*;

import org.json.*;
import org.openstatic.lovense.*;
import org.openstatic.lovense.swing.*;
import org.openstatic.midi.*;

public class MidiLovenseBridge extends JFrame implements Runnable, ChangeListener, Receiver, LovenseConnectListener, ActionListener, LovenseToyListener
{
    protected JList toyList;
    private JList midiList;
    private JList rulesList;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenuItem aboutMenuItem;
    private JMenuItem exitMenuItem;
    private JMenuItem addIpItem;

    private LovenseToyListModel lovenseToyListModel;
    protected LovenseToyCellRenderer lovenseToyRenderer;
    private MidiPortCellRenderer midiRenderer;
    private MidiPortListModel midiListModel;
    private Thread mainThread;
    protected DefaultListModel<MidiRelayRule> rules;
    protected LinkedBlockingQueue<Runnable> taskQueue;
    public static MidiLovenseBridge instance;
    private boolean keep_running;
    private long lastToyClick;
    private JSONObject options;


    public MidiLovenseBridge()
    {
        super("Midi Lovense Bridge");
        MidiPortManager.init();
        this.keep_running = true;
        this.options = new JSONObject();
        this.taskQueue = new LinkedBlockingQueue<Runnable>();

        MidiLovenseBridge.instance = this;
        centerWindow();
        this.setLayout(new BorderLayout());
        try
        {
            BufferedImage windowIcon = ImageIO.read(getClass().getResource("/res/windows.png"));
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
        this.toyList = new JList(this.lovenseToyListModel);
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
        lovenseToyScrollPane.setBorder(new TitledBorder("Lovense Toys (double-click to create rule)"));
        JPanel toysAndPower = new JPanel(new BorderLayout());
        JSlider powerSlider = new JSlider(JSlider.VERTICAL, 0, 20, 0);
        powerSlider.setBorder(new TitledBorder("Vibrate"));
        powerSlider.addChangeListener(this);
        powerSlider.setPreferredSize(new Dimension(60, 0));
        toysAndPower.add(lovenseToyScrollPane, BorderLayout.CENTER);
        toysAndPower.add(powerSlider, BorderLayout.EAST);

        // Setup rule list
        this.rulesList = new JList(this.rules);
        JScrollPane ruleScrollPane = new JScrollPane(this.rulesList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ruleScrollPane.setBorder(new TitledBorder("Rules for Incoming MIDI Messages (click to edit)"));
        this.rulesList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               int index = MidiLovenseBridge.this.rulesList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                  MidiRelayRule source = (MidiRelayRule) MidiLovenseBridge.this.rules.getElementAt(index);
                  MidiRelayRuleEditor editor = new MidiRelayRuleEditor(source);
               }
            }
        });

        this.add(toysAndPower, BorderLayout.CENTER);
        this.add(ruleScrollPane, BorderLayout.PAGE_END);

        this.midiListModel = new MidiPortListModel();
        this.midiRenderer = new MidiPortCellRenderer();
        this.midiList = new JList(this.midiListModel);
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

    public static void repaintToys()
    {
        (new Thread (() -> {
            MidiLovenseBridge.instance.toyList.repaint();
        })).start();
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
            this.taskQueue.add(() ->
            {
                try
                {
                    LovenseToy toy = (LovenseToy) MidiLovenseBridge.this.toyList.getSelectedValue();
                    toy.vibrate(v);
                    MidiLovenseBridge.repaintToys();
                } catch (Exception e2) {

                }
            });
        }
    }

    public void run()
    {
        while(this.keep_running)
        {
            try
            {
                Runnable r = this.taskQueue.poll(1, TimeUnit.SECONDS);
                if (r != null)
                {
                    r.run();
                }
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
            String ip_port = JOptionPane.showInputDialog("Please enter an ip and httpPort for Lovense Connect\n(Example: 127.0.0.1:30110)\nIt may take a few moments for your toys to be found");
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
    }

    public void toyRemoved(int idx, LovenseToy toy)
    {
        toy.removeLovenseToyListener(this);
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
                this.taskQueue.add(puf);
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

    public void loadConfig()
    {
        try
        {
            JSONObject configJson = loadJSONObject("config.json");
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
            saveJSONObject("config.json", configJson);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public static JSONObject loadJSONObject(String filename)
    {
        try
        {
            File load_file = new File(filename);
            FileInputStream fis = new FileInputStream(load_file);
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

    public static void saveJSONObject(String filename, JSONObject obj)
    {
        try
        {
            File load_file = new File(filename);
            FileOutputStream fos = new FileOutputStream(load_file);
            PrintStream ps = new PrintStream(fos);
            ps.print(obj.toString());
            ps.close();
            fos.close();
        } catch (Exception e) {
        }
    }
}
