// Fig. 14.8: Interaccion.java
// Using JSliders to size an oval.
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class InteraccionW extends JFrame implements CommonVars {
    private JSlider rcaaSlider, rcabSlider, rcacSlider, epSlider;
    private Container container;
    private GridBagLayout disposicion;
    private GridBagConstraints constraints;
    private PlotPanelW myPanelW;
    private int ircaa, ircab, ircac, ieps, IRCAA=500,IRCAB=500, IRCAC=500, IEPS=500;
    private double dr=0.01;
    private static int itype, istart, ipause ;
    public static int PHSIZE=450, PVSIZE,HS=460,VS=550;
   // set up GUI
   public InteraccionW(double rcaa, double rcab, double rcac, double epsw, int ityp, int ipa, int ist, Point pos) 
   {
      super( DiMol2D.messages.getString("interacciones")+ " "+ DiMol2D.messages.getString("atomopared") );
      ircaa = (int)(rcaa/dr+0.51);
      ircab = (int)(rcab/dr+0.51);
      ircac = (int)(rcac/dr+0.51);
      ieps = (int)(epsw/dr+0.51);
      itype = ityp;
      ipause = ipa;
      istart = ist;
      if (ityp == 1) {
	  PVSIZE=460;
      }
      else if (ityp == 2) {
	  PVSIZE=500;
      }
      else if (ityp == 3) {
	  PVSIZE=545;
      }
 
      // set up PlotPanel
      myPanelW = new PlotPanelW(ircaa, ircab, ircac, epsw, dr,  itype,IRCAA, HS, VS);
      myPanelW.setBackground(colorBP);

      // set up JSlider to control rcaa value
      rcaaSlider = 
         new JSlider( SwingConstants.HORIZONTAL, 100, IRCAA, ircaa );
      rcaaSlider.setMinorTickSpacing( 10 );
      rcaaSlider.setMajorTickSpacing( 50 );
      rcaaSlider.setPaintTicks( true );
      rcaaSlider.setLabelTable(makeLabeli(100,IRCAA,50));
      rcaaSlider.setPaintLabels( true );

      // register JSlider event listener
      rcaaSlider.addChangeListener(

         new ChangeListener() {  // anonymous inner class

            // handle change in slider value
            public void stateChanged( ChangeEvent e )
            {
		myPanelW.setRcaa( rcaaSlider.getValue() );
		if (!rcaaSlider.getValueIsAdjusting()) {
		    Simulation.setRW1((double)rcaaSlider.getValue()*dr);
		}
            }

         } // end anonymous inner class

      ); // end call to addChangeListener

      epSlider = 
         new JSlider( SwingConstants.HORIZONTAL, 100, IEPS, ieps );
      epSlider.setMinorTickSpacing( 10 );
      epSlider.setMajorTickSpacing( 50 );
      epSlider.setPaintTicks( true );
      epSlider.setLabelTable(makeLabeli(100,IEPS,50));
      epSlider.setPaintLabels( true );

      // register JSlider event listener
      epSlider.addChangeListener(

         new ChangeListener() {  // anonymous inner class

            // handle change in slider value
            public void stateChanged( ChangeEvent e )
            {
		myPanelW.setEp( epSlider.getValue() );
		if (!epSlider.getValueIsAdjusting()) {
		    Simulation.setEps((double)epSlider.getValue()*dr);
		}
            }

         } // end anonymous inner class

      ); // end call to addChangeListener


      if (itype > 1 || Simulation.iheter == 1) {

	  rcabSlider = 
	      new JSlider( SwingConstants.HORIZONTAL, 100, IRCAB, ircab );
	  rcabSlider.setMinorTickSpacing( 10 );
	  rcabSlider.setMajorTickSpacing( 50 );
	  rcabSlider.setPaintTicks( true );
	  rcabSlider.setLabelTable(makeLabeli(100,IRCAB,50));
	  rcabSlider.setPaintLabels( true );
	  // register JSlider event listener
	  rcabSlider.addChangeListener(

	      new ChangeListener() {  // anonymous inner class

		  // handle change in slider value
		  public void stateChanged( ChangeEvent e )
		      {
			  myPanelW.setRcab( rcabSlider.getValue() );
			  if (!rcabSlider.getValueIsAdjusting()) {
			      Simulation.setRW2((double)rcabSlider.getValue()*dr);
			  }
		      }
		  
	      } // end anonymous inner class

	      ); // end call to addChangeListener

	  // attach components to content pane
      }

      if (itype > 2 || Simulation.iheter == 1) {

	  rcacSlider = 
	      new JSlider( SwingConstants.HORIZONTAL, 100, IRCAC, ircac );
	  rcacSlider.setMinorTickSpacing( 10 );
	  rcacSlider.setMajorTickSpacing( 50 );
	  rcacSlider.setPaintTicks( true );
	  rcacSlider.setLabelTable(makeLabeli(100,IRCAC,50));
	  rcacSlider.setPaintLabels( true );
	  // register JSlider event listener
	  rcacSlider.addChangeListener(

	      new ChangeListener() {  // anonymous inner class

		  // handle change in slider value
		  public void stateChanged( ChangeEvent e )
		      {
			  myPanelW.setRcac( rcacSlider.getValue() );
			  if (!rcacSlider.getValueIsAdjusting()) {
			      Simulation.setRW3((double)rcacSlider.getValue()*dr);
			  }
		      }
		  
	      } // end anonymous inner class

	      ); // end call to addChangeListener

	  // attach components to content pane
      }

      container = getContentPane();
      disposicion = new GridBagLayout();
      container.setLayout(disposicion);
      constraints = new GridBagConstraints();
      JLabel epw = new JLabel(DiMol2D.messages.getString("profundidad"));
      JLabel lrw1 = new JLabel(DiMol2D.messages.getString("alcancep")+"-A ");
      JLabel lrw2 = new JLabel(DiMol2D.messages.getString("alcancep")+"-B ");
      JLabel lrw3 = new JLabel(DiMol2D.messages.getString("alcancep")+"-C ");
      constraints.fill = GridBagConstraints.BOTH;
      addComponent( myPanelW,0,0,4,1);
      constraints.fill = GridBagConstraints.HORIZONTAL;
      addComponent(epw,1,1,1,1);
      addComponent( epSlider, 1,2,4,1);

      addComponent(lrw1,2,1,1,1);
      if (Simulation.iheter == 0) {
	  addComponent( rcaaSlider, 2,2,4,1);
	  if ( ipause != 1 && istart != 0 ) {
	      rcaaSlider.setEnabled(false);
	  }
	  else {
	      rcaaSlider.setEnabled(true);
	  }
      }
      if (itype > 1 || Simulation.iheter==1) {
	  addComponent(lrw2,3,1,1,1);
	  addComponent( rcabSlider, 3,2,4,1);
	  if ( ipause != 1 && istart != 0 ) {
	      rcabSlider.setEnabled(false);
	  }
	  else {
	      rcabSlider.setEnabled(true);
      }
      }

      if (itype > 2 || Simulation.iheter == 1) {
	  addComponent(lrw3,4,1,1,1);
	  addComponent( rcacSlider, 4,2,4,1);
	  if ( ipause != 1 && istart != 0 ) {
	      rcacSlider.setEnabled(false);
	  }
	  else {
	      rcacSlider.setEnabled(true);
      }
      }

      //      container.add( myPanelW, BorderLayout.CENTER );
      setLocation (pos);
      setSize( PHSIZE, PVSIZE );
      setVisible( true );
      
   } // end constructor Interaccion

    public void disiSlider (){
	epSlider.setEnabled(false);
	if (Simulation.iheter == 0) rcaaSlider.setEnabled(false);
	if (itype > 1 || Simulation.iheter == 1) 	rcabSlider.setEnabled(false);
	if (itype > 2 || Simulation.iheter == 1) 	rcacSlider.setEnabled(false);
    }

    public void eniSlider (){
	epSlider.setEnabled(true);
	if (Simulation.iheter == 0) rcaaSlider.setEnabled(true);
	if (itype > 1 || Simulation.iheter == 1) 	rcabSlider.setEnabled(true);
	if (itype > 2 || Simulation.iheter == 1) 	rcacSlider.setEnabled(true);
    }
	

  public Hashtable makeLabeli(int min, int max, int incr) 
  { 
     String[] alpha = { "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0"};
     Hashtable<Integer,JLabel> hshLabels = new Hashtable<Integer,JLabel>(); 
     int val = min; 
     int i = 0; 
   
     while (val <= max) 
     { 
        hshLabels.put(new Integer(val), new JLabel(alpha[i]) ); 
        val += incr; 
        i++; 
        if (i > alpha.length ) i = 0; 
     } 
   
     return hshLabels; 
  } 
    public static void setPause(int ip) {
	ipause = ip;
    }
    public static void setStart(int it) {
	istart = it;
    }

    public void repintaInterW() {
	container.repaint();
    }
	
    private void addComponent( Component component,
      int row, int column, int width, int height )
   {
      // set gridx and gridy 
      constraints.gridx = column;
      constraints.gridy = row;

      // set gridwidth and gridheight
      constraints.gridwidth = width;   
      constraints.gridheight = height;

      // set constraints and add component
      disposicion.setConstraints( component, constraints );  
      container.add( component );      
   }


} 

