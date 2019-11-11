package org.openstatic;

import javax.sound.midi.*;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import java.util.Enumeration;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.*;

public class MidiSourceListModel implements ListModel<MidiSource>, MidiSourceListener
{
    private Vector<ListDataListener> listeners = new Vector<ListDataListener>();

    public MidiSourceListModel()
    {
    }

    public void sourceAdded(int idx, MidiSource source)
    {
        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                ListDataListener ldl = ldle.nextElement();
                ListDataEvent lde = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, idx, idx);
                ldl.intervalAdded(lde);
            } catch (Exception mlex) {
            }
        }
    }

    public void sourceRemoved(int idx, MidiSource source)
    {
        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                ListDataListener ldl = ldle.nextElement();
                ListDataEvent lde = new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED, idx, idx);
                ldl.intervalRemoved(lde);
            } catch (Exception mlex) {
            }
        }
    }

    public int getSize()
    {
        try
        {
            return MidiSource.getSources().size();
        } catch (Exception e) {
            return 0;
        }
    }

    public MidiSource getElementAt(int index)
    {
        try
        {
            MidiSource[] sources = MidiSource.getSources().toArray(new MidiSource[0]);
            return sources[index];
        } catch (Exception e) {
            return null;
        }
    }

    public void addListDataListener(ListDataListener l)
    {
        if (!this.listeners.contains(l))
            this.listeners.add(l);
    }

    public void removeListDataListener(ListDataListener l)
    {
        try
        {
            this.listeners.remove(l);
        } catch (Exception e) {}
    }
}
