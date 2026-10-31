// Elongacion.java
// Using JSliders to define the particle elongation
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class Elongacion extends JFrame implements CommonVars {
    private JSlider elonSlider;
    private Container cont;
    private JLabel label;
    private int ELMIN=20,ELMAX=100,ixma=20;
    private static int itype, istart, ipause ;
    public static int PHSIZE=300, PVSIZE=100;
   // set up GUI
   public Elongacion(double elon, int ipa, int ist, Point pos) 
   {
      super(DiMol2D.messages.getString("elong") );
      ipause = ipa;
      istart = ist;
      cont = getContentPane();
      cont.setLayout( new BorderLayout(1,1));	  
      // set up JSlider to control xmol value
      elonSlider = 
         new JSlider( SwingConstants.HORIZONTAL, ELMIN, ELMAX, ixma );
      elonSlider.setMinorTickSpacing( 5 );
      elonSlider.setMajorTickSpacing( 10 );
      elonSlider.setPaintTicks( true );
      elonSlider.setLabelTable(makeLabeli(0,100,10));
      elonSlider.setPaintLabels( true );
      elonSlider.setValue((int)(elon*100));
      elonSlider.setBackground(colorFnd);
      elonSlider.setForeground(new Color(1,1,1));
      // register JSlider event listener
      elonSlider.addChangeListener(

         new ChangeListener() {  // anonymous inner class

            // handle change in slider value
            public void stateChanged( ChangeEvent e )
            {
		if (!elonSlider.getValueIsAdjusting()) {
		    Simulation.setElon((double)elonSlider.getValue()*0.01);
		    //		    System.out.println(" slidera ="+elonSlider.getValue()+"\n");
		}
            }

         } // end anonymous inner class

      ); // end call to addChangeListener

      label = new JLabel();
      label.setText(DiMol2D.messages.getString("elong"));
      label.setHorizontalTextPosition( SwingConstants.CENTER );
      label.setFont( new Font("SansSerif", Font.BOLD,14));
      cont.add(label,BorderLayout.CENTER);
      cont.add( elonSlider,BorderLayout.SOUTH);
      label.setBackground(colorFnd);
      cont.setBackground(colorFnd);
      if ( ipause != 1 && istart != 0 ) {
	  elonSlider.setEnabled(false);
      }
      else {
	  elonSlider.setEnabled(true);
      }
      setLocation (pos);
      setSize( PHSIZE, PVSIZE );
      setVisible( true );
      
   } // end constructor Composicion

    public void disiSlider (){
	elonSlider.setEnabled(false);
    }

    public void eniSlider (){
	elonSlider.setEnabled(true);
    }
    public void setSlider (int valuea){
	elonSlider.setValue(valuea);
    }	


  public Hashtable makeLabeli(int min, int max, int incr) 
  { 
     String[] alpha = { "0.0", "0.1", "0.2", "0.3","0.4","0.5", "0.6","0.7", "0.8","0.9","1.0"};
     Hashtable<Integer,JLabel> hshLabels = new Hashtable<Integer,JLabel>(); 
     int val = min; 
     int i = 0; 
   
     while (val <= max) 
     { 
        hshLabels.put(new Integer(val), new JLabel(alpha[i]) ); 
        val += incr; 
        i++; 
        if (i > 11 ) i = 0; 
     } 
   
     return hshLabels; 
  } 
    public static void setPause(int ip) {
	ipause = ip;
    }
    public static void setStart(int it) {
	istart = it;
    }

    public void repintaElon() {
	cont.repaint();
    }

} 

