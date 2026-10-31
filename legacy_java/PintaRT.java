import java.awt.*;
import javax.swing.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.text.DecimalFormat;

public class PintaRT extends JPanel {
    DecimalFormat dosDigitos ;
    int XDIM=40,YDIM=40;
    double temp, dens;
    double ytt[];
    public PintaRT(double t,double rho, int ancho, int alto) {
	XDIM = ancho;
	YDIM = alto;
	dosDigitos = new DecimalFormat("0.00") ;
	temp=t;
	dens=rho;
    }


    public void paintComponent( Graphics g ) {

	super.paintComponent( g );
	Graphics2D g2d = (Graphics2D ) g;
	//	g2d.setPaint(new GradientPaint(5, 30, new Color(0,3,127), 35, 100, new Color(0,155,251), true));
	g2d.setPaint(new Color(147,185,1));
	g2d.fill(new  Rectangle2D.Double(0,0,XDIM,YDIM));
	//	g2d.setPaint(new Color(255,237,147));
	g2d.setPaint(new Color(52,4,165));
	g2d.setFont( new Font("SansSerif", Font.BOLD,12));
	g2d.drawString(" "+DiMol2D.messages.getString("temperatura")+"="+dosDigitos.format(temp),1,22);
	g2d.drawString(" "+DiMol2D.messages.getString("densidad")+"="+dosDigitos.format(dens),10,40);
    }

    public void setRT(double t, double rho) {
	temp=t ;
	dens=rho;
	repaint();
    }
   // used by layout manager to determine preferred size
   public Dimension getPreferredSize() {
       return new Dimension( XDIM, YDIM );
   }

   public Dimension getMaximumSize() {
       return getPreferredSize();
   }

   // used by layout manager to determine minimum size
   public Dimension getMinimumSize() {
       return getPreferredSize();
   }

    } // end class PlotGr

