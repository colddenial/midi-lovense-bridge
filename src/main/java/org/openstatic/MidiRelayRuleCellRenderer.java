package org.openstatic;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import javax.swing.border.Border;

public class MidiRelayRuleCellRenderer extends JCheckBox implements ListCellRenderer<MidiRelayRule>
{
   private Border selectedBorder;
   private Border regularBorder;

   public MidiRelayRuleCellRenderer()
   {
       super();
       this.setOpaque(false);
       this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 3);
       this.regularBorder = BorderFactory.createLineBorder(new Color(1f,1f,1f,1f), 3);

   }

   @Override
   public Component getListCellRendererComponent(JList<? extends MidiRelayRule> list,
                                                 MidiRelayRule rule,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
      this.setText("<html>" + rule.toString() + "</html>");
      this.setSelected(rule.isEnabled());
      if (isSelected)
      {
         this.setBackground(list.getSelectionBackground());
         this.setForeground(list.getSelectionForeground());
      } else {
         this.setBackground(list.getBackground());
         this.setForeground(list.getForeground());
      }

      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());

      if (isSelected && cellHasFocus)
         this.setBorder(this.selectedBorder);
      else
         this.setBorder(this.regularBorder);

      return this;
   }
}
