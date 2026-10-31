import java.awt.*;
import javax.swing.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.text.DecimalFormat;

public class Manometer extends JPanel {
    DecimalFormat unDigito ;
    int X0=40,Y0=40,NTICK=20,NMTICK=10,NTICKL=20,ANCHO,ALTO,XDIM,YDIM,X1,Y1,ALTOL,altura,i,j,ic,nl;
    double PMIN=-1.0, PMAXL=1.0,PMAX=40.0, pres, ytick, dy, dly, da, dla;
    double ytt[];
    public Manometer(double presi,int ancho, int alto) {
	ANCHO = ancho;
	ALTO = alto;
	unDigito = new DecimalFormat("0.0") ;
	pres = presi;
    }

    public void limpia () {
	repaint();
    }

    public void paintComponent( Graphics g ) {

	super.paintComponent( g );
	nl = (int) (Math.log(PMAX/PMAXL)/0.1+0.51);
	ytt = new double[nl+1];
	ic = 0;
	for (j=0;j<=nl/10;j++) {
	    for (i=1;i<=9;i++) {
		ic++;
		ytt[ic] = (double)i*Math.pow(10.0,(double)j);
	    }
	}
	dy = (PMAXL-PMIN)/(double)NTICK;
	dly = (PMAX-PMAXL)/(double)NTICKL;
	X1= X0+ANCHO;
	Y1=Y0+ALTO;
	ALTOL=ALTO/2;
	XDIM = X1+80;
	YDIM = Y1+40;
	da = (double)ALTOL/(double)(NTICK);
	dla =  (double)(ALTO-ALTOL)/(Math.log(PMAX/PMAXL)/Math.log(10.0));
	Graphics2D g2d = (Graphics2D ) g;
	g2d.setPaint(new GradientPaint(5, 30, new Color(0,3,127), 35, 100, new Color(0,155,251), true));
	g2d.fill(new  Rectangle2D.Double(0,0,XDIM,YDIM));
	g2d.setPaint(new Color(255,237,147));
	g2d.setFont( new Font("SansSerif", Font.BOLD,10));
	g2d.drawString(" "+DiMol2D.messages.getString("presion")+" ",35,20);
	if ( pres <= PMAXL) {
	    altura = (int)((double)ALTOL*(pres-PMIN)/(PMAXL-PMIN)+0.51);
	}
	else   {
	    altura = (int)((double)(ALTO-ALTOL)*(Math.log(pres)-Math.log(PMAXL))/(Math.log(PMAX)-Math.log(PMAXL))+0.51) +ALTOL;
	}
	g2d.setPaint(new Color(210,226,255));
	g2d.fill(new  Rectangle2D.Double(X0,Y1-altura,ANCHO,altura));
	g2d.setPaint(Color.RED);
	g2d.setStroke(new BasicStroke(2.5f));
	g2d.draw(new Rectangle2D.Double(X0,Y0,ANCHO,ALTO));
	g2d.setPaint(Color.YELLOW);
	g2d.setStroke(new BasicStroke(1.5f));
	g2d.draw(new Line2D.Double(X1+10,Y0,X1+10,Y1));
	for (i = 0; i<= NTICK;i++) {
	    ytick = PMIN + dy*(double)i;
	    altura = (int)((double)i*da);
	    g2d.draw(new Line2D.Double(X1+10,Y1-altura,X1+15,Y1-altura));
	    if (i%NMTICK == 0) {
		g2d.draw(new Line2D.Double(X1+10,Y1-altura,X1+20,Y1-altura));
		g2d.drawString(unDigito.format(ytick), X1+25, Y1-altura);
	    }
	}
 	for (i = 1; i< NTICKL;i++) {
	    ytick = ytt[i];
	    altura = (int)(dla*(Math.log(ytick/PMAXL)/Math.log(10.0))+ALTOL);
	    if ( ytick <= PMAX) {
		g2d.draw(new Line2D.Double(X1+10,Y1-altura,X1+15,Y1-altura));
		if (i%NMTICK == 0 || i == NTICKL-1 || ytick == PMAX){
		    g2d.draw(new Line2D.Double(X1+10,Y1-altura,X1+20,Y1-altura));
		    g2d.drawString(unDigito.format(ytick), X1+25, Y1-altura);
		}
	    }
	}
    }

    public void setPresure(double presion) {
	pres = presion ;
	repaint();
    }
   // used by layout manager to determine preferred size
   public Dimension getPreferredSize() {
       return new Dimension( XDIM-10, YDIM-10 );
   }

   public Dimension getMaximumSize() {
       return getPreferredSize();
   }

   // used by layout manager to determine minimum size
   public Dimension getMinimumSize() {
       return getPreferredSize();
   }

    } // end class PlotGr

