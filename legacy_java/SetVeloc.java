import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

    public class SetVeloc extends JFrame
    {
	private JSlider velSlider;
	private int NVELMIN=0,NVELMAX=400,NVELSTP=100;
	Container cont ;
	public SetVeloc (){
	    super(DiMol2D.messages.getString("sincrot"));
	    JLabel label ;
	    cont = getContentPane();
	    cont.setLayout( new BorderLayout(1,1));
	    label = new JLabel();
	    label.setText(DiMol2D.messages.getString("sincrotime"));		    
	    label.setHorizontalTextPosition( SwingConstants.CENTER );
	    label.setFont( new Font("SansSerif", Font.BOLD,14));
	    cont.add(label,BorderLayout.CENTER);
	    label.setBackground(new Color(147,185,1));
	    cont.setBackground(new Color(147,185,1));


	    velSlider = 
		new JSlider( SwingConstants.HORIZONTAL, NVELMIN, NVELMAX, Simulation.NSLEEP );
	    velSlider.setMajorTickSpacing( NVELSTP );
	    velSlider.setMinorTickSpacing( NVELSTP/4 );
	    velSlider.setPaintTicks( true );
	    velSlider.setBackground(new Color(147,185,1));
	    velSlider.setForeground(new Color(1,1,1));
	    velSlider.setPaintLabels( true );
	    velSlider.setSnapToTicks( false );

	    velSlider.addChangeListener(new ChangeListener() {public void stateChanged( ChangeEvent e )
	      {
		 
		      if (!velSlider.getValueIsAdjusting()) {
			  Simulation.NSLEEP = velSlider.getValue();
		      }
		 
	      }
		} // end anonymous inner class
		); // end call to addChangeListener
	    cont.add(velSlider,BorderLayout.SOUTH);
	    setSize(320,100);
	    setVisible(true);

	}
	public void setnpslider (int n) {
	    velSlider.setValue(n);
	}
	public void disnpslider () {
	    velSlider.setEnabled(false);
	}
	public void ennpslider () {
	    velSlider.setEnabled(true);
	}
	public void repintaV() {
	    cont.repaint();
	}

    }
