// Fig. 14.8: Interaccion.java
// Using JSliders to size an oval.
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class Composicion extends JFrame implements CommonVars{
    private JSlider xmolaSlider, xmolbSlider;
    private Container container;
    private JLabel label, labelB;
    private int ixma=50, ixmb=50,ixmc=0;
    private double dr=0.01;
    private static int itype, istart, ipause ;
    public static int PHSIZE=550, PVSIZE;
    private GridLayout grid;
   // set up GUI
   public Composicion( int natm[], int ityp, int ipa, int ist, Point pos) 
   {
      super(DiMol2D.messages.getString("mole") );
      itype = ityp;
      ipause = ipa;
      istart = ist;
      if (itype == 2) {
	  grid = new GridLayout(1,2) ;
	  PVSIZE = 100 ;
      }
      else if ( itype == 3) {
	  grid = new GridLayout(2,2) ;
	  PVSIZE = 160 ;
      }
	  
      // set up JSlider to control xmol value
      if (itype == 2) ixma = (100*natm[1])/natm[2];
      if (itype == 3) {
	  ixma = (100*natm[1])/natm[3];
	  ixmb = (100*(natm[2]-natm[1]))/natm[3];
      }
      xmolaSlider = 
         new JSlider( SwingConstants.HORIZONTAL, 0, 100, ixma );
      xmolaSlider.setMinorTickSpacing( 5 );
      xmolaSlider.setMajorTickSpacing( 25 );
      xmolaSlider.setPaintTicks( true );
      xmolaSlider.setLabelTable(makeLabeli(0,100,25));
      xmolaSlider.setPaintLabels( true );
      xmolaSlider.setBackground(colorFnd);
      xmolaSlider.setForeground(new Color(1,1,1));
      // register JSlider event listener
      xmolaSlider.addChangeListener(

         new ChangeListener() {  // anonymous inner class

            // handle change in slider value
            public void stateChanged( ChangeEvent e )
            {
		if (!xmolaSlider.getValueIsAdjusting()) {
		    Simulation.setNparta((double)xmolaSlider.getValue()*0.01);
		    //		    System.out.println(" slidera ="+xmolaSlider.getValue()+"\n");
		}
            }

         } // end anonymous inner class

      ); // end call to addChangeListener


      if (itype > 2) {

	  xmolbSlider = 
	      new JSlider( SwingConstants.HORIZONTAL, 0, 100, ixmb );
	  xmolbSlider.setMinorTickSpacing( 5 );
	  xmolbSlider.setMajorTickSpacing( 25 );
	  xmolbSlider.setPaintTicks( true );
	  xmolbSlider.setLabelTable(makeLabeli(0,100,25));
	  xmolbSlider.setPaintLabels( true );
	  xmolbSlider.setBackground(colorFnd);
	  xmolbSlider.setForeground(new Color(1,1,1));

	  // register JSlider event listener
	  xmolbSlider.addChangeListener(

	      new ChangeListener() {  // anonymous inner class

		  // handle change in slider value
		  public void stateChanged( ChangeEvent e )
		      {
			  if (!xmolbSlider.getValueIsAdjusting()) {
			      Simulation.setNpartb((double)xmolbSlider.getValue()*0.01);
			      //			      System.out.println(" sliderb ="+xmolbSlider.getValue()+"\n");
			  }
		      }
		  
	      } // end anonymous inner class

	      ); // end call to addChangeListener

	  // attach components to content pane
      }
      container = getContentPane();
      container.setLayout(grid);
      label = new JLabel();
      label.setText(DiMol2D.messages.getString("moleA"));
      label.setHorizontalTextPosition( SwingConstants.CENTER );
      label.setFont( new Font("SansSerif", Font.BOLD,14));
      container.add(label);
      container.add( xmolaSlider);
      label.setBackground(colorFnd);
      container.setBackground(colorFnd);
      if ( ipause != 1 && istart != 0 ) {
	  xmolaSlider.setEnabled(false);
      }
      else {
	  xmolaSlider.setEnabled(true);
      }
      if (itype > 2) {
	  labelB = new JLabel();
	  labelB.setText(DiMol2D.messages.getString("moleB"));
	  labelB.setHorizontalTextPosition( SwingConstants.CENTER );
	  labelB.setFont( new Font("SansSerif", Font.BOLD,14));
	  container.add(labelB);

	  container.add( xmolbSlider );
	  if ( ipause != 1 && istart != 0 ) {
	      xmolbSlider.setEnabled(false);
	  }
	  else {
	      xmolbSlider.setEnabled(true);
      }
      }
      setLocation (pos);
      setSize( PHSIZE, PVSIZE );
      setVisible( true );
      
   } // end constructor Composicion

    public void disiSlider (){
	xmolaSlider.setEnabled(false);
	if (itype > 2) 	xmolbSlider.setEnabled(false);
    }

    public void eniSlider (){
	xmolaSlider.setEnabled(true);
	if (itype > 2) 	xmolbSlider.setEnabled(true);
    }
    public void setSliderA (int valuea){
	xmolaSlider.setValue(valuea);
    }	

    public void setSliderB (int valueb){
	xmolbSlider.setValue(valueb);
    }	


  public Hashtable makeLabeli(int min, int max, int incr) 
  { 
     String[] alpha = { "0.0", "0.25", "0.50", "0.75", "1.0"};
     Hashtable<Integer,JLabel> hshLabels = new Hashtable<Integer,JLabel>(); 
     int val = min; 
     int i = 0; 
   
     while (val <= max) 
     { 
        hshLabels.put(new Integer(val), new JLabel(alpha[i]) ); 
        val += incr; 
        i++; 
        if (i > 5 ) i = 0; 
     } 
   
     return hshLabels; 
  } 
    public static void setPause(int ip) {
	ipause = ip;
    }
    public static void setStart(int it) {
	istart = it;
    }

    public void repintaCompos() {
	container.repaint();
    }

} 

