import java.awt.*;
import javax.swing.*;
import java.text.DecimalFormat;

public class PlotPanelW extends JPanel {
    DecimalFormat dosDigitos ;
    private double Rcaa, Rcab, Rcac;
    private double r0, y0, r1, y1, eps, uminaa, uminab, uminac, a, b, ap, bp;
    private int ix0, iy0, ix1, iy1, NPLMAX=101, nmin;
    private double XMAX=5.0, XMIN=1.0, YMAX=1.0, YMIN=-1.0;
    private int ircaa, ircab, ircac, nmax, phsize, pvsize;
    private double dr,  y;
    private int i, itype, iheter;
    private int HS=250, VS=200, IHS=60, IVS=40;
    private double xx[], yy[];
    private Graph gr;
    PlotPanelW(int iaa, int iab, int iac, double epsw, double dri, int ityp, int ircmax, int phsizei, int pvsizei ) {
	dosDigitos = new DecimalFormat("0.00") ;
	ircaa = iaa;
	ircab = iab;
	ircac = iac;
	phsize = phsizei;
	pvsize = pvsizei;
	eps = epsw;
	dr = dri;
	itype = ityp;
	iheter = Simulation.iheter;
	nmin = (int)(XMIN/dr+0.51);
	nmax = (int)(XMAX/dr+0.51);
	Rcaa = dr*(double)ircaa;
	Rcab = dr*(double)ircab;
	Rcac = dr*(double)ircac;
	xx = new double[ircmax+1];
	yy = new double[ircmax+1];
	gr = new Graph();
    }

	    
   public void paintComponent( Graphics g )
   {
      super.paintComponent( g );
      uminaa = 3.07*eps*(1.0/Math.pow(Rcaa,10.0)-1.0/Math.pow(Rcaa,4.0));
      uminab = 3.07*eps*(1.0/Math.pow(Rcab,10.0)-1.0/Math.pow(Rcab,4.0));
      uminac = 3.07*eps*(1.0/Math.pow(Rcac,10.0)-1.0/Math.pow(Rcac,4.0));
      gr.drawGraph(g,IHS,IVS,VS,HS,XMIN,XMAX,(int)(eps*YMIN-0.51),(int)(eps*YMAX+0.51),"w(z)","z",0.0);
      if (iheter == 0) {
	  for (i=nmin;i<=ircaa;i++) {
	      xx[i] = (double)i*dr;
	      yy[i] =  3.07*eps*(1.0/Math.pow(xx[i],10.0)-1.0/Math.pow(xx[i],4.0))-uminaa;
	  }
	  g.setColor(Color.BLACK);
	  g.setFont( new Font("SansSerif", Font.BOLD,18));
	  g.setColor(Simulation.colorA);
	  gr.drawGraphLine(g, xx, yy,  nmin, Math.min(ircaa,nmax),IHS,IVS,VS,HS,XMIN,XMAX,(int)(eps*YMIN-0.51),(int)(eps*YMAX+0.51),IHS+HS+10,IVS+VS/2,IHS+HS+40,IVS+VS/2,"P-A");
      }
      if (itype > 1 || iheter == 1) {
	  g.setColor(Color.BLACK);
	  g.setFont( new Font("SansSerif", Font.BOLD,18));
	  g.setColor(Simulation.colorB);
	  for (i=nmin;i<=ircab;i++) {
	      xx[i] = (double)i*dr;
	      yy[i] =  3.07*eps*(1.0/Math.pow(xx[i],10.0)-1.0/Math.pow(xx[i],4.0))-uminab;
	  }
	  gr.drawGraphLine(g, xx, yy,  nmin, Math.min(ircab,nmax),IHS,IVS,VS,HS,XMIN,XMAX,(int)(eps*YMIN-0.51),(int)(eps*YMAX+0.51),IHS+HS+10,IVS+VS/2+20,IHS+HS+40,IVS+VS/2+20,"P-B");

      }
      if (itype > 2 || iheter == 1) {
	  g.setColor(Color.BLACK);
	  g.setFont( new Font("SansSerif", Font.BOLD,18));
	  g.setColor(Simulation.colorC);
	  for (i=nmin;i<=ircac;i++) {
	      xx[i] = (double)i*dr;
	      yy[i] =  3.07*eps*(1.0/Math.pow(xx[i],10.0)-1.0/Math.pow(xx[i],4.0))-uminac;
	  }
	  gr.drawGraphLine(g, xx, yy,  nmin, Math.min(ircac,nmax),IHS,IVS,VS,HS,XMIN,XMAX,(int)(eps*YMIN-0.51),(int)(eps*YMAX+0.51),IHS+HS+10,IVS+VS/2+40,IHS+HS+40,IVS+VS/2+40,"P-C");

      }

   }

   // validate and set diameter, then repaint 
   public void setRcaa( int newDiameter )
   {
       ircaa = newDiameter;
       Rcaa = (double)newDiameter/100.0;
      repaint();
   }
   public void setRcab( int newDiameter )
   {
       ircab = newDiameter;
       Rcab = (double)newDiameter/100.0;
      repaint();
   }

   public void setRcac( int newDiameter )
   {
       ircac = newDiameter;
       Rcac = (double)newDiameter/100.0;
      repaint();
   }

   public void setEp( int newEps )
   {
       eps = (double)newEps/100.0;
      repaint();
   }

   // used by layout manager to determine preferred size
   public Dimension getPreferredSize()
   {
      return new Dimension( phsize-20, pvsize-200 );
   }

   // used by layout manager to determine minimum size
   public Dimension getMinimumSize()
   {
      return getPreferredSize();
   }

} // end class PlotPanelW

