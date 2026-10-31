// Fig. 14.8: Interaccion.java
// Using JSliders to size an oval.
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class Interaccion extends JFrame  {
    private JSlider rcaaSlider, rcabSlider;
    private PlotPanel myPanel;
    private Container container;
    private int ircaa, ircab,iheter, IRCAA=250,IRCAB=250;
    private double dr=0.01;
    private static int itype, istart, ipause ;
    public static int PHSIZE=450, PVSIZE=420;
   // set up GUI
   public Interaccion(double rcaa, double rcab, int ityp, int ipa, int ist, Point pos) 
   {
      super(DiMol2D.messages.getString("interacciones") );
      ircaa = (int)(rcaa/dr+0.51);
      ircab = (int)(rcab/dr+0.51);
      itype = ityp;
      iheter = Simulation.iheter;
      ipause = ipa;
      istart = ist;
      // set up PlotPanel
      myPanel = new PlotPanel(ircaa, ircab, dr, itype,Math.max(IRCAA,IRCAB));
      myPanel.setBackground(Simulation.colorBP);

      // set up JSlider to control rcaa value
      rcaaSlider = 
         new JSlider( SwingConstants.HORIZONTAL, 100, 250, ircaa );
      rcaaSlider.setMinorTickSpacing( 10 );
      rcaaSlider.setMajorTickSpacing( 50 );
      rcaaSlider.setPaintTicks( true );
      rcaaSlider.setLabelTable(makeLabeli(100,250,50));
      rcaaSlider.setPaintLabels( true );

      // register JSlider event listener
      rcaaSlider.addChangeListener(

         new ChangeListener() {  // anonymous inner class

            // handle change in slider value
            public void stateChanged( ChangeEvent e )
            {
		myPanel.setRcaa( rcaaSlider.getValue() );
		if (!rcaaSlider.getValueIsAdjusting()) {
		    Simulation.setCutoffaa((double)rcaaSlider.getValue()*dr);
		}
            }

         } // end anonymous inner class

      ); // end call to addChangeListener


      if (itype > 1||iheter == 1) {

	  rcabSlider = 
	      new JSlider( SwingConstants.HORIZONTAL, 100, 250, ircab );
	  rcabSlider.setMinorTickSpacing( 10 );
	  rcabSlider.setMajorTickSpacing( 50 );
	  rcabSlider.setPaintTicks( true );
	  rcabSlider.setLabelTable(makeLabeli(100,250,50));
	  rcabSlider.setPaintLabels( true );
	  // register JSlider event listener
	  rcabSlider.addChangeListener(

	      new ChangeListener() {  // anonymous inner class

		  // handle change in slider value
		  public void stateChanged( ChangeEvent e )
		      {
			  myPanel.setRcab( rcabSlider.getValue() );
			  if (!rcabSlider.getValueIsAdjusting()) {
			      Simulation.setCutoffab((double)rcabSlider.getValue()*dr);
			  }
		      }
		  
	      } // end anonymous inner class

	      ); // end call to addChangeListener

	  // attach components to content pane
      }
      container = getContentPane();
      container.add( rcaaSlider, BorderLayout.SOUTH );
      if ( ipause != 1 && istart != 0 ) {
	  rcaaSlider.setEnabled(false);
      }
      else {
	      rcaaSlider.setEnabled(true);
      }
      if (itype > 1 || iheter == 1) {
	  container.add( rcabSlider, BorderLayout.NORTH );
	  if ( ipause != 1 && istart != 0 ) {
	      rcabSlider.setEnabled(false);
	  }
	  else {
	      rcabSlider.setEnabled(true);
	  }
      }
      container.add( myPanel, BorderLayout.CENTER );
      setLocation (pos);
      setSize( PHSIZE, PVSIZE );
      setVisible( true );
      
   } // end constructor Interaccion

    public void disiSlider (){
	rcaaSlider.setEnabled(false);
	if (itype > 1|| iheter==1) 	rcabSlider.setEnabled(false);
    }

    public void eniSlider (){
	rcaaSlider.setEnabled(true);
	if (itype > 1|| iheter==1) 	rcabSlider.setEnabled(true);
    }
	

  public Hashtable makeLabeli(int min, int max, int incr) 
  { 
     String[] alpha = { "1.0", "1.5", "2.0", "2.5"};
     Hashtable<Integer,JLabel> hshLabels = new Hashtable<Integer,JLabel>(); 
     int val = min; 
     int i = 0; 
   
     while (val <= max) 
     { 
        hshLabels.put(new Integer(val), new JLabel(alpha[i]) ); 
        val += incr; 
        i++; 
        if (i > 4 ) i = 0; 
     } 
   
     return hshLabels; 
  } 
    public static void setPause(int ip) {
	ipause = ip;
    }
    public static void setStart(int it) {
	istart = it;
    }

    public void repintaInter() {
	container.repaint();
    }

} 

