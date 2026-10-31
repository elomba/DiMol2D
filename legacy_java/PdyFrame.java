import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class PdyFrame extends JFrame {
    private PlotPdy myPanel;
    public static int PHSIZE=420, PVSIZE=320;
   // set up GUI
   public PdyFrame(int ngr, double dri) 
   {
       super(DiMol2D.messages.getString("perfdensver"));
      // set up PlotPanel
      myPanel = new PlotPdy(ngr, dri);
      myPanel.setBackground(Simulation.colorBP);

      // set up JSlider to control rcaa value
      Container container = getContentPane();

      container.add( myPanel, BorderLayout.CENTER );

      setSize( PHSIZE, PVSIZE );
      setResizable(false);
      setVisible( true );

   } // end constructor GrFrame
   
public void repintaPdy(  )
   {
      myPanel.repaint();
   }

} 

