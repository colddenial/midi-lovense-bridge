package org.openstatic;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.TitledBorder;

import javax.imageio.ImageIO;
import java.awt.image.AffineTransformOp;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import javax.sound.midi.*;

import javax.swing.border.Border;
import org.openstatic.lovense.*;

public class MidiSourceCellRenderer extends JCheckBox implements ListCellRenderer<MidiSource>
{
   private Border selectedBorder;
   private Border regularBorder;

   public MidiSourceCellRenderer()
   {
       super();
       this.setOpaque(false);
       this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 3);
       this.regularBorder = BorderFactory.createLineBorder(new Color(1f,1f,1f,1f), 3);
   }

   @Override
   public Component getListCellRendererComponent(JList<? extends MidiSource> list,
                                                 MidiSource device,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
      this.setText(device.getName());
      this.setSelected(device.isOpened());
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
