import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class GrFrame extends JFrame {
    private PlotGr myPanel;
    public static int PHSIZE=420, PVSIZE=320;
   // set up GUI
   public GrFrame(int ngr, double dri) 
   {
      super(DiMol2D.messages.getString("gr"));
      // set up PlotPanel
      myPanel = new PlotGr(ngr, dri);
      myPanel.setBackground(Simulation.colorBP);

      // set up JSlider to control rcaa value
      Container container = getContentPane();

      container.add( myPanel, BorderLayout.CENTER );

      setSize( PHSIZE, PVSIZE );
      setResizable(false);
      setVisible( true );

   } // end constructor GrFrame
   
public void repintaGr(  )
   {
      myPanel.repaint();
   }

} 

