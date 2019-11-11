package org.openstatic;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

public class MidiSource
{
    private String name;
    private MidiDevice device;
    private Transmitter transmitter;
    private boolean opened;
    private static LinkedHashMap<MidiDevice.Info, MidiSource> localDevices = new LinkedHashMap<MidiDevice.Info, MidiSource>();
    private static Vector<MidiSource> sources = new Vector<MidiSource>();
    private static Vector<MidiSourceListener> listeners = new Vector<MidiSourceListener>();
    private static long lastSourceFetch = 0;

    public MidiSource(MidiDevice device)
    {
        this.device = device;
        this.name = device.getDeviceInfo().getName();
    }

    public void open()
    {
        try
        {
            if (this.transmitter == null)
            {
                this.transmitter = device.getTransmitter();
                this.device.open();
                this.opened = true;
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public boolean isOpened()
    {
        return this.opened;
    }

    public String getName()
    {
        return this.name;
    }

    public MidiDevice getDevice()
    {
        return this.device;
    }

    public void close()
    {
        try
        {
            if (this.transmitter != null)
            {
                this.transmitter.close();
                this.device.close();
                this.opened = false;
                this.transmitter = null;
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public Transmitter getTransmitter()
    {
        this.open();
        return this.transmitter;
    }

    public void setReceiver(Receiver r)
    {
        try
        {
            this.getTransmitter().setReceiver(r);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }


    public boolean equals(MidiSource source)
    {
        return this.name.equals(source.getName());
    }

    public static void refresh()
    {
        refreshLocalDevices();
        Vector<MidiSource> updatedSources = new Vector<MidiSource>();
        updatedSources.addAll(MidiSource.localDevices.values());

         // Check for new sources added
        for(Iterator<MidiSource> updatedSourcesIterator = updatedSources.iterator(); updatedSourcesIterator.hasNext();)
        {
            MidiSource t = updatedSourcesIterator.next();
            if (!MidiSource.sources.contains(t))
            {
                MidiSource.sources.add(t);
                int idx = MidiSource.sources.indexOf(t);
                System.err.println("Source Added: " + String.valueOf(idx) + " - " + t.getName());
                fireSourceAdded(idx, t);
            }
        }

        // check for sources removed
        for(Iterator<MidiSource> oldSourcesIterator = MidiSource.sources.iterator(); oldSourcesIterator.hasNext();)
        {
            MidiSource t = oldSourcesIterator.next();
            if (!updatedSources.contains(t))
            {
                int idx = MidiSource.sources.indexOf(t);
                System.err.println("Source Removed: " + String.valueOf(idx) + " - " + t.getName());
                fireSourceRemoved(idx, t);
            }
        }
        MidiSource.sources = updatedSources;
        MidiSource.lastSourceFetch = System.currentTimeMillis();
    }

    private static void refreshLocalDevices()
    {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        Vector<MidiDevice.Info> newLocalDevices = new Vector<MidiDevice.Info>(Arrays.asList(infos));

        // Check for new devices added
        for(Iterator<MidiDevice.Info> newLocalDevicesIterator = newLocalDevices.iterator(); newLocalDevicesIterator.hasNext();)
        {
            MidiDevice.Info di = newLocalDevicesIterator.next();
            //System.out.println("Local Device Found: " + di.toString());

            if (!MidiSource.localDevices.containsKey(di))
            {
                try
                {
                    MidiDevice device = MidiSystem.getMidiDevice(di);
                    if (device.getMaxTransmitters() != 0)
                    {
                        MidiSource ms = new MidiSource(device);
                        System.err.println("MidiSource Created: " + ms.getName());
                        MidiSource.localDevices.put(di, ms);
                    }
                } catch (MidiUnavailableException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }

        // check for devices removed
        for(Iterator<MidiDevice.Info> oldDevicesIterator = MidiSource.localDevices.keySet().iterator(); oldDevicesIterator.hasNext();)
        {
            MidiDevice.Info di = oldDevicesIterator.next();
            if (!newLocalDevices.contains(di))
            {
                MidiSource.localDevices.remove(di);
            }
        }
    }

    private static void fireSourceAdded(int idx, MidiSource source)
    {
        for (Enumeration<MidiSourceListener> msle = ((Vector<MidiSourceListener>) MidiSource.listeners.clone()).elements(); msle.hasMoreElements();)
        {
            try
            {
                MidiSourceListener msl = msle.nextElement();
                msl.sourceAdded(idx, source);
            } catch (Exception mlex) {
            }
        }
    }

    private static void fireSourceRemoved(int idx, MidiSource source)
    {
        for (Enumeration<MidiSourceListener> msle = ((Vector<MidiSourceListener>) MidiSource.listeners.clone()).elements(); msle.hasMoreElements();)
        {
            try
            {
                MidiSourceListener msl = msle.nextElement();
                msl.sourceRemoved(idx, source);
            } catch (Exception mlex) {
            }
        }
    }

    public static void addMidiSourceListener(MidiSourceListener msl)
    {
        if (!MidiSource.listeners.contains(msl))
        {
            MidiSource.listeners.add(msl);
        }
    }

    public static void removeMidiSourceListener(MidiSourceListener msl)
    {
        if (MidiSource.listeners.contains(msl))
        {
            MidiSource.listeners.remove(msl);
        }
    }

    public static Collection<MidiSource> getSources()
    {
        if ((System.currentTimeMillis() - MidiSource.lastSourceFetch) > 20000)
        {
            MidiSource.refresh();
        }
        return MidiSource.sources;
    }
}
