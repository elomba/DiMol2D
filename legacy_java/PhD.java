import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class PhD extends JFrame {
    private PlotPD myPanel;
    public static int PHSIZE=380, PVSIZE=380;
   // set up GUI
   public PhD(int px, int py) {
      super(DiMol2D.messages.getString("diagramafases"));
      // set up PlotPanel
      myPanel = new PlotPD((70*PHSIZE)/100,(70*PVSIZE)/100);
      myPanel.setBackground(new Color(237,255,215));

      // set up JSlider to control rcaa value
      Container container = getContentPane();

      container.add( myPanel, BorderLayout.CENTER );

      setSize( PHSIZE, PVSIZE );
      setResizable(false);
      setLocation(px,py);
      setVisible( true );

   } // end constructor PD
   
public void repintaPD(  )
   {
      myPanel.repaint();
   }
public void ptransient(double temp, double dens  )
   {
       myPanel.plotpoint(temp, dens);
       myPanel.repaint();
   }
} 

