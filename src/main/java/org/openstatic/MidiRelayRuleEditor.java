package org.openstatic;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.DefaultListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.TitledBorder;
import javax.swing.ImageIcon;
import javax.swing.WindowConstants;

import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dialog;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import java.util.Vector;

import org.openstatic.lovense.*;
import org.openstatic.lovense.swing.*;
import javax.sound.midi.*;

public class MidiRelayRuleEditor extends JDialog implements ActionListener
{
    private MidiRelayRule rule;
    private JComboBox data1Selector;
    private JComboBox channelSelector;
    private JComboBox commandSelector;
    private JComboBox rangeSelector;
    private JComboBox outputSelector;
    private JTextField nicknameField;
    private JButton saveButton;
    private JButton deleteButton;
    private JLabel data1Label;
    private DefaultListModel<String> data1Options;

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == this.data1Selector)
        {
            this.rule.setData1(this.data1Selector.getSelectedIndex());
        }
        if (e.getSource() == this.channelSelector)
        {
            this.rule.setChannel(this.channelSelector.getSelectedIndex());
        }
        if (e.getSource() == this.outputSelector)
        {
            this.rule.setOutput(this.outputSelector.getSelectedIndex());
        }
        if (e.getSource() == this.commandSelector)
        {
            int cmd = this.commandSelector.getSelectedIndex();
            changeCommandSelector(cmd);
        }
        if (e.getSource() == this.rangeSelector)
        {
            this.rule.setRange(this.rangeSelector.getSelectedIndex());
        }
        if (e.getSource() == this.deleteButton)
        {
            if (MidiLovenseBridge.instance.rules.contains(this.rule))
                MidiLovenseBridge.instance.rules.removeElement(this.rule);
            LovenseConnect.removeLovenseConnectListener(this.rule);
            this.dispose();
        }
        if (e.getSource() == this.saveButton)
        {
            if (!"".equals(this.nicknameField.getText()))
                this.rule.setNickname(this.nicknameField.getText());
            else
                this.rule.setNickname(null);
            if (!MidiLovenseBridge.instance.rules.contains(this.rule))
                MidiLovenseBridge.instance.rules.addElement(this.rule);
            this.dispose();
        }
    }

    private DefaultComboBoxModel<String> getMidiNotes()
    {
        DefaultComboBoxModel<String> noteList = new DefaultComboBoxModel<String>();
        for(int i = 0; i < 128; i++)
        {
            String noteName = MidiLovenseBridge.noteNumberToString(i);
            noteList.insertElementAt(String.valueOf(i) + " - " + noteName, i);
        }
        return noteList;
    }

    public DefaultComboBoxModel<String> getCCList()
    {
        DefaultComboBoxModel<String> ccList = new DefaultComboBoxModel<String>();
        for(int i = 0; i < 128; i++)
        {
            if (i == 0)
            {
                ccList.insertElementAt("ALL",0);
            } else {
                ccList.insertElementAt(String.valueOf(i),i);
            }
        }
        return ccList;
    }

    private void changeCommandSelector(int value)
    {
        if (value == 0)
        {
            this.rule.setCommand(ShortMessage.CONTROL_CHANGE);
            this.data1Label.setText("Select CC#");
            this.data1Selector.setModel(getCCList());
            this.data1Selector.setSelectedIndex(this.rule.getData1());
        } else if (value == 1) {
            this.rule.setCommand(ShortMessage.NOTE_ON);
            this.data1Label.setText("Select NOTE");
            this.data1Selector.setModel(getMidiNotes());
            this.data1Selector.setSelectedIndex(this.rule.getData1());
        }
    }
    
    public MidiRelayRuleEditor(MidiRelayRule rule)
    {
        this(rule, false);
    }
    
    public MidiRelayRuleEditor(MidiRelayRule rule, boolean newRule)
    {
        super(MidiLovenseBridge.instance, "Rule Editor", true);
        this.setLayout(new BorderLayout());
        this.rule = rule;

        Vector<String> commandList = new Vector<String>();
        commandList.add("CONTROL CHANGE");
        commandList.add("NOTE ON/OFF");
        
        Vector<String> rangeList = new Vector<String>();
        rangeList.add("FULL (0-127)");
        rangeList.add("TOP (64-127)");
        rangeList.add("BOTTOM INVERTED (63-0)");
        rangeList.add("FULL INVERTED (127-0)");
        
        Vector<String> outputList = new Vector<String>();
        outputList.add("VIBRATE");
        outputList.add("VIBRATE ONE");
        outputList.add("VIBRATE TWO");
        outputList.add("ROTATE");
        outputList.add("AIR");

        Vector<String> midiChannels = new Vector<String>();
        for(int i = 0; i < 17; i++)
        {
            if (i == 0)
            {
                midiChannels.add("ALL");
            } else {
                midiChannels.add(String.valueOf(i));
            }
        }

        this.channelSelector = new JComboBox(midiChannels);
        this.channelSelector.setEditable(false);
        this.channelSelector.addActionListener(this);

        this.data1Selector = new JComboBox();
        this.data1Selector.setEditable(false);
        this.data1Selector.addActionListener(this);

        this.commandSelector = new JComboBox(commandList);
        this.commandSelector.setEditable(false);
        this.commandSelector.addActionListener(this);
        
        this.rangeSelector = new JComboBox(rangeList);
        this.rangeSelector.setEditable(false);
        this.rangeSelector.addActionListener(this);
        
        this.outputSelector = new JComboBox(outputList);
        this.outputSelector.setEditable(false);
        this.outputSelector.addActionListener(this);
    
        this.nicknameField = new JTextField("");
        this.nicknameField.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel formPanel = new JPanel(new GridLayout(7,2));
        if (this.rule.getRelayTo() != null)
        {
            JLabel toyLabel = new JLabel("<html><body><b>" + this.rule.getRelayTo().toString() + "</b><br />" + this.rule.getRelayTo().getDevice().getHostname() + "</html>");
            try
            {
              ImageIcon icon = new ImageIcon(MidiLovenseBridge.instance.lovenseToyRenderer.getIconForToy(this.rule.getRelayTo().getName()));
              toyLabel.setIcon(icon);
            } catch (Exception e) {
              e.printStackTrace(System.err);
            }
            this.add(labelComponent("Toy", toyLabel), BorderLayout.PAGE_START);
        }
        
        formPanel.add(new JLabel("Rule Name", SwingConstants.CENTER));
        formPanel.add(this.nicknameField);
    

        formPanel.add(new JLabel("Select Command", SwingConstants.CENTER));
        formPanel.add(this.commandSelector);

        formPanel.add(new JLabel("Select Channel", SwingConstants.CENTER));
        formPanel.add(this.channelSelector);

        this.data1Label = new JLabel("Select CC#", SwingConstants.CENTER);
        formPanel.add(this.data1Label);
        formPanel.add(this.data1Selector);

        formPanel.add(new JLabel("Data Range", SwingConstants.CENTER));
        formPanel.add(this.rangeSelector);
        
        formPanel.add(new JLabel("Toy Output", SwingConstants.CENTER));
        formPanel.add(this.outputSelector);
        
        if (newRule)
        {
            this.saveButton = new JButton("Create Rule");
            this.deleteButton = new JButton("Cancel");
        } else {
            this.saveButton = new JButton("Save Rule");
            this.deleteButton = new JButton("Delete Rule");
        }
        this.saveButton.addActionListener(this);
        this.deleteButton.addActionListener(this);
        
        formPanel.add(deleteButton);
        formPanel.add(saveButton);

        this.add(formPanel, BorderLayout.CENTER);

        this.channelSelector.setSelectedIndex(this.rule.getChannel());
        if (this.rule.getCommand() == ShortMessage.CONTROL_CHANGE)
        {
            this.commandSelector.setSelectedIndex(0);
            this.changeCommandSelector(0);
        } else if (this.rule.getCommand() == ShortMessage.NOTE_ON) {
            this.commandSelector.setSelectedIndex(1);
            this.changeCommandSelector(1);
        }
        this.rangeSelector.setSelectedIndex(this.rule.getRange());
        this.outputSelector.setSelectedIndex(this.rule.getOutput());
        if (this.rule.getNickname() != null)
            this.nicknameField.setText(this.rule.getNickname());
        centerWindow();

    }

    public JPanel labelComponent(String label, Component c)
    {
        JPanel x = new JPanel(new GridLayout(1,2));
        x.add(new JLabel(label, SwingConstants.CENTER));
        x.add(c);
        return x;
    }

    public void centerWindow()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final float WIDTH = screenSize.width;
        final float HEIGHT = screenSize.height;
        int wWidth = 400;
        int wHeight = 380;
        int x = (int) ((WIDTH/2f) - ( ((float)wWidth) /2f ));
        int y = (int) ((HEIGHT/2f) - ( ((float)wHeight) /2f ));
        this.setBounds(x, y, wWidth, wHeight);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);
    }
}
