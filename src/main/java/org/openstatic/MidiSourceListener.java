package org.openstatic;

public interface MidiSourceListener
{
    public void sourceAdded(int idx, MidiSource source);
    public void sourceRemoved(int idx, MidiSource source);
}
