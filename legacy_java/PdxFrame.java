import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class PdxFrame extends JFrame {
    private PlotPdx myPanel;
    public static int PHSIZE=420, PVSIZE=320;
   // set up GUI
   public PdxFrame(int ngr, double dri) 
   {
      super(DiMol2D.messages.getString("perfdenshor"));
      // set up PlotPanel
      myPanel = new PlotPdx(ngr, dri);
      myPanel.setBackground(new Color(237,255,215));

      // set up JSlider to control rcaa value
      Container container = getContentPane();

      container.add( myPanel, BorderLayout.CENTER );

      setSize( PHSIZE, PVSIZE );
      setResizable(false);
      setVisible( true );

   } // end constructor GrFrame
   
public void repintaPdx(  )
   {
      myPanel.repaint();
   }

} 

