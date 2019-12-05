package org.openstatic;

import javax.sound.midi.*;
import org.openstatic.lovense.*;
import org.json.*;

public class MidiRelayRule implements LovenseConnectListener
{
    private int command;
    private int channel;
    private int data1;
    private int range;
    private int output;
    private String toyId;
    private String nickname;
    private LovenseToy relayTo;
    
    public static final int FULL_RANGE = 0;
    public static final int TOP_HALF = 1;
    public static final int BOTTOM_HALF_INVERTED = 2;
    public static final int FULL_RANGE_INVERTED = 3;

    public static final int OUTPUT_VIBRATE = 0;
    public static final int OUTPUT_VIBRATE_ONE = 1;
    public static final int OUTPUT_VIBRATE_TWO = 2;
    public static final int OUTPUT_ROTATE = 3;
    public static final int OUTPUT_AIR = 4;

    public MidiRelayRule(int channel, int command, int data1, LovenseToy relayTo)
    {
        this.channel = channel;
        this.command = command;
        this.data1 = data1;
        this.relayTo = relayTo;
        this.toyId = relayTo.getId();
        this.range = 0;
        this.output = 0;
        this.nickname = null;
        LovenseConnect.addLovenseConnectListener(this);
    }
    
    public MidiRelayRule(JSONObject jo)
    {
        this.channel = jo.optInt("channel", 0);
        this.command = jo.optInt("command", ShortMessage.CONTROL_CHANGE);
        this.data1 = jo.optInt("data1", 0);
        this.range = jo.optInt("range", 0);
        this.output = jo.optInt("output", 0);
        this.toyId = jo.optString("toy", null);
        this.nickname = jo.optString("nickname", null);
        try
        {
            this.relayTo = LovenseConnect.getToyById(this.toyId);
        } catch (Exception e) {}
        LovenseConnect.addLovenseConnectListener(this);
    }
    
    public void toyAdded(int idx, LovenseToy toy)
    {
        if (toy.getId().equalsIgnoreCase(this.toyId))
        {
            if (this.relayTo == null)
                this.relayTo = toy;
            else if (!this.relayTo.isConnected() && toy.isConnected())
                this.relayTo = toy;
        }
    }

    public void toyRemoved(int idx, LovenseToy toy)
    {
        if (this.relayTo == toy)
        {
            this.relayTo = null;
        }
    }

    public boolean messageMatches(ShortMessage msg)
    {
        if (this.relayTo != null)
        {
            if ((msg.getChannel()+1) == this.channel || this.channel == 0 )
            {
                if (this.command == ShortMessage.CONTROL_CHANGE && msg.getCommand() == ShortMessage.CONTROL_CHANGE)
                {
                    if (msg.getData1() == this.data1 || this.data1 == 0)
                    {
                        return true;
                    }
                } else if (this.command == ShortMessage.NOTE_ON && (msg.getCommand() == ShortMessage.NOTE_ON || msg.getCommand() == ShortMessage.NOTE_OFF)) {
                    if (msg.getData1() == this.data1)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int dataToVibrate(int data)
    {
        if (this.range == MidiRelayRule.FULL_RANGE) // FULL RANGE
        {
            return Math.round(((float)data) / 6.35f);
        } else if (this.range == MidiRelayRule.TOP_HALF) { // TOP HALF 64-127
            if (data >= 64)
            {
                return Math.round(((float)(data-64)) / 3.20f);
            } else {
                return 0;
            }
        } else if (this.range == MidiRelayRule.BOTTOM_HALF_INVERTED) { // BOTTOM HALF 0-63 INVERTED
            if (data < 64)
            {
                return 20 - Math.round(((float)(data)) / 3.20f);
            } else {
                return 0;
            }
        } else if (this.range == MidiRelayRule.FULL_RANGE_INVERTED) { // FULL RANGE INVERTED
            return 20 - Math.round(((float)data) / 6.35f);
        }
        return 0;
    }
    
    public int dataToAir(int data)
    {
        if (this.range == MidiRelayRule.FULL_RANGE) // FULL RANGE
        {
            return Math.round(((float)data) / 42.33f);
        } else if (this.range == MidiRelayRule.TOP_HALF) { // TOP HALF 64-127
            if (data >= 64)
            {
                return Math.round(((float)(data-64)) / 21.33f);
            } else {
                return 0;
            }
        } else if (this.range == MidiRelayRule.BOTTOM_HALF_INVERTED) { // BOTTOM HALF 0-63 INVERTED
            if (data < 64)
            {
                return 20 - Math.round(((float)(data)) / 21.33f);
            } else {
                return 0;
            }
        } else if (this.range == MidiRelayRule.FULL_RANGE_INVERTED) { // FULL RANGE INVERTED
            return 20 - Math.round(((float)data) / 42.33f);
        }
        return 0;
    }

    public void processMessage(ShortMessage msg)
    {
        if (this.relayTo.isConnected())
        {
            int vv = 0;
            if (this.output == OUTPUT_AIR)
            {
                vv = dataToAir(msg.getData2());
            } else {
                vv = dataToVibrate(msg.getData2());
            }
            if (msg.getCommand() == ShortMessage.NOTE_OFF)
            {
                //Ignore value on note off messages
                vv = 0;
            }
            int existing_vv = this.relayTo.getOutputOneValue();
            
            if (this.output == OUTPUT_VIBRATE_TWO || this.output == OUTPUT_ROTATE || this.output == OUTPUT_AIR)
                existing_vv = this.relayTo.getOutputTwoValue();
            
            if (existing_vv != vv)
            {
                switch(this.output)
                {
                    case OUTPUT_VIBRATE:
                        this.relayTo.vibrate(vv);
                        break;
                    case OUTPUT_VIBRATE_ONE:
                        this.relayTo.vibrate1(vv);
                        break;
                    case OUTPUT_VIBRATE_TWO:
                        this.relayTo.vibrate2(vv);
                        break;
                    case OUTPUT_ROTATE:
                        this.relayTo.rotate(vv);
                        break;
                    case OUTPUT_AIR:
                        this.relayTo.airAuto(vv);
                        break;
                }
            } else {
                //System.err.println("OMIT MATCHING VALUE");
            }
        }
    }

    public LovenseToy getRelayTo()
    {
        return this.relayTo;
    }

    public int getData1()
    {
        return this.data1;
    }

    public int getChannel()
    {
        return this.channel;
    }

    public int getCommand()
    {
        return this.command;
    }
    
    public int getRange()
    {
        return this.range;
    }
    
    public int getOutput()
    {
        return this.output;
    }
    
    public String getNickname()
    {
        return this.nickname;
    }

    public void setData1(int data1)
    {
        this.data1 = data1;
    }

    public void setChannel(int channel)
    {
        this.channel = channel;
    }
    
    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public void setCommand(int command)
    {
        this.command = command;
    }
    
    public void setRange(int range)
    {
        this.range = range;
    }
    
    public void setOutput(int output)
    {
        this.output = output;
    }

    public JSONObject toJSONObject()
    {
        JSONObject jo = new JSONObject();
        jo.put("range", this.range);
        jo.put("output", this.output);
        jo.put("command", this.command);
        jo.put("channel", this.channel);
        jo.put("data1", this.data1);
        if (this.relayTo != null)
        {
            jo.put("toy", this.relayTo.getId());
        } else {
            jo.put("toy", this.toyId);
        }
        jo.put("nickname", this.nickname);
        return jo;
    }

    public String toString()
    {
        String channelText = "[CH=" + String.valueOf(this.getChannel()) + "]";
        if (this.getChannel() == 0)
            channelText = "[ALL Channels]";
        String data1Name = "?";
        String data1Value = "?";
        String rangeText = "";
        String targetText = "";
        String outputText = "";
        if (this.command == ShortMessage.CONTROL_CHANGE)
        {
            data1Name = "CC";
            data1Value = String.valueOf(this.getData1());
        } else if (this.command == ShortMessage.NOTE_ON) {
            data1Name = "NOTE";
            data1Value = MidiLovenseBridge.noteNumberToString(this.getData1());
        }
        if (this.range == MidiRelayRule.FULL_RANGE)
        {
            rangeText = "FULL (0-127)";
        } else if (this.range == MidiRelayRule.TOP_HALF) {
            rangeText = "TOP (64-127)";
        } else if (this.range == MidiRelayRule.BOTTOM_HALF_INVERTED) {
            rangeText = "BOTTOM INV (63-0)";
        } else if (this.range == MidiRelayRule.FULL_RANGE_INVERTED) {
            rangeText = "FULL INV (127-0)";
        }
        switch(this.output)
        {
            case OUTPUT_VIBRATE:
                outputText = "VIBRATE";
                break;
            case OUTPUT_VIBRATE_ONE:
                outputText = "VIBRATE 1";
                break;
            case OUTPUT_VIBRATE_TWO:
                outputText = "VIBRATE 2";
                break;
            case OUTPUT_ROTATE:
                outputText = "ROTATE";
                break;
            case OUTPUT_AIR:
                outputText = "AIR";
                break;
        }
        String data1Text = "(" + data1Name + "=" + data1Value + ")";;
        if (this.getData1() == 0 && this.command == ShortMessage.CONTROL_CHANGE)
            data1Text = "(Every " + data1Name + ")";
        if (this.relayTo == null)
        {
            targetText = this.toyId;
        } else {
            targetText = this.relayTo.getNickname();
        }
        if (this.nickname == null)
        {
            return channelText + " " + data1Text + " " + rangeText + " >> " + targetText + " " + outputText;
        } else {
            return this.nickname + " " + channelText + " " + data1Text + " " + rangeText + " >> " + targetText + " " + outputText;
        }
    }
}
