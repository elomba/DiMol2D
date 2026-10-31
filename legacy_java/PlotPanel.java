import java.awt.*;
import javax.swing.*;
import java.text.DecimalFormat;

public class PlotPanel extends SiModulos {
    DecimalFormat dosDigitos ;
    private double Rcaa;
    private double Rcab;
    private double r0, y0, r1, y1, uminaa, uminab, a, b, ap, bp;
    private int ix0, iy0, ix1, iy1, NPLMAX=101, nmin;
    private double XMAX=2.0, XMIN=1.0, YMAX=1.0, YMIN=-1.0;
    private int ircaa, ircab, nmax;
    private double dr,  y;
    private int i, itype;
    private int HS=250, VS=200, IHS=60, IVS=40;
    private double xx[], yy[];
    private Graph gr;
    PlotPanel(int iaa, int iab, double dri, int ityp, int ircmax ) {
	dosDigitos = new DecimalFormat("0.00") ;
	ircaa = iaa;
	ircab = iab;
	dr = dri;
	itype = ityp;
	nmin = (int)(XMIN/dr+0.51);
	nmax = (int)(XMAX/dr+0.51);
	Rcaa = dr*(double)ircaa;
	Rcab = dr*(double)ircab;
	xx = new double[ircmax+1];
	yy = new double[ircmax+1];
	gr = new Graph();
    }

	    
   public void paintComponent( Graphics g )
   {
      super.paintComponent( g );
      uminaa = 4.0*(1.0/Math.pow(Rcaa,12.0)-1.0/Math.pow(Rcaa,6.0));
      uminab = 4.0*(1.0/Math.pow(Rcab,12.0)-1.0/Math.pow(Rcab,6.0));
      for (i=nmin;i<=ircaa;i++) {
	  xx[i] = (double)i*dr;
	  yy[i] =  4.0*(1.0/Math.pow(xx[i],12.0)-1.0/Math.pow(xx[i],6.0))-uminaa;
      }
      gr.drawGraph(g,IHS,IVS,VS,HS,XMIN,XMAX,YMIN,YMAX,"u(r)","r",0.0);
      g.setColor(Color.BLACK);
      g.setFont( new Font("SansSerif", Font.BOLD,18));
      g.drawString("      "+DiMol2D.messages.getString("alcanceaa")+" = "+dosDigitos.format(Rcaa),0,Interaccion.PVSIZE-120);
      g.setColor(colorA);
      gr.drawGraphLine(g, xx, yy,  nmin, Math.min(ircaa,nmax),IHS,IVS,VS,HS,XMIN,XMAX,YMIN,YMAX,IHS+HS+10,IVS+VS/2,IHS+HS+40,IVS+VS/2,"A-A");
      if (itype > 1||Simulation.iheter==1) {
	  g.setColor(Color.BLACK);
	  g.setFont( new Font("SansSerif", Font.BOLD,18));
	  g.drawString("     " +DiMol2D.messages.getString("alcanceab")+" = "+dosDigitos.format(Rcab),0,20);
	  g.setColor(colorB);
	  for (i=nmin;i<=ircab;i++) {
	      xx[i] = (double)i*dr;
	      yy[i] =  4.0*(1.0/Math.pow(xx[i],12.0)-1.0/Math.pow(xx[i],6.0))-uminab;
	  }
	  gr.drawGraphLine(g, xx, yy,  nmin, Math.min(ircab,nmax),IHS,IVS,VS,HS,XMIN,XMAX,YMIN,YMAX,IHS+HS+10,IVS+VS/2+20,IHS+HS+40,IVS+VS/2+20,"A-B");

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

   // used by layout manager to determine preferred size
   public Dimension getPreferredSize()
   {
      return new Dimension( 200, 200 );
   }

   // used by layout manager to determine minimum size
   public Dimension getMinimumSize()
   {
      return getPreferredSize();
   }

} // end class PlotPanel

