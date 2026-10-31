import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

    public class SetNpart extends JFrame implements CommonVars
    {
	private JSlider npSlider;
	public SetNpart (){
	    super(DiMol2D.messages.getString("numeroparticulas"));
	    JLabel label ;
	    Container cont = getContentPane();
	    cont.setLayout( new BorderLayout(1,1));
	    label = new JLabel();
	    label.setText(DiMol2D.messages.getString("numeroparticulas"));
	    label.setHorizontalTextPosition( SwingConstants.CENTER );
	    label.setFont( new Font("SansSerif", Font.BOLD,14));
	    cont.add(label,BorderLayout.CENTER);
	    label.setBackground(new Color(147,185,1));
	    cont.setBackground(new Color(147,185,1));


	    npSlider = 
		new JSlider( SwingConstants.HORIZONTAL, Simulation.NMOLMIN, Simulation.NMOLMAX, Simulation.NMOL );
	    npSlider.setMajorTickSpacing( Simulation.NMSTP );
	    npSlider.setPaintTicks( true );
	    npSlider.setBackground(colorFnd);
	    npSlider.setForeground(new Color(1,1,1));
	    npSlider.setPaintLabels( true );
	    npSlider.setSnapToTicks( true );

	    npSlider.addChangeListener(new ChangeListener() {public void stateChanged( ChangeEvent e )
	      {
		  if ( Simulation.ipause != 1 && Simulation.istart != 0 ) {
		      		JOptionPane.showMessageDialog(null,"No. of particles can only be changed when the program is not running ");
		  }
		  else {
		      if (!npSlider.getValueIsAdjusting()) {
			  Simulation.NMOLN = npSlider.getValue();
			  Simulation.NMOLN = (Simulation.NMOLN/6)*6;
			  Simulation.icn = 1;
		      }
		  }
	      }
		} // end anonymous inner class
		); // end call to addChangeListener
	    cont.add(npSlider,BorderLayout.SOUTH);
	    setSize(280,100);
	    setLocation(Simulation.dimX-500,0);
	    setVisible(true);

	}
	public void setnpslider (int n) {
	    npSlider.setValue(n);
	}
	public void disnpslider () {
	    npSlider.setEnabled(false);
	}
	public void ennpslider () {
	    npSlider.setEnabled(true);
	}


    }
