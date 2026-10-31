import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.DecimalFormat;

public class PlotGr extends SiModulos {
    DecimalFormat tresDigitos ;
    private double Rcaa , Rcab;
    private static int ipinta=0, xPos, yPos ;
    private static double xDist , yGmax ;
    private int nmin, nmax , ii , jj , nnmax ;
    private double XMAX=10.0, XMIN=0.0, YMAXO=4.0, YMAX, YMIN=0;
    private double dr,  y;
    private int i, itype;
    private int HS=250, VS=200, IHS=60, IVS=30;
    private double xx[], yy[] , x1[] , y1[] ;
    private Graph gr;
    PlotGr(int ngr, double dri) {
	tresDigitos = new DecimalFormat("0.000") ;
	dr = dri;
	nmin = 1;
	nmax = Math.min(ngr,(int)(XMAX/dr+0.5)) ;
	x1 = new double[2000];
	y1 = new double[2000];
	xx = new double[2000];
	yy = new double[2000];
	gr = new Graph();

	addMouseListener(new MouseAdapter() {  // anonymous inner class
		// handle mouse press event
		public void mousePressed( MouseEvent event ) {
		    coordenadas(1,event.getX(),event.getY());
		    valores(1,event.getX(),event.getY()) ;
		} 
		// handle mouse release event
		public void mouseReleased( MouseEvent event )
		{ 
		    limpia();
		} 
		
	    } // end anonymous inner clas
			 
			 ); // end call to addMouseListener
    }

    public void valores(int pinta , int x , int y) {
	ipinta = pinta ;
	xPos = x ;
        yPos = y ;
	xDist = (xPos-IHS)*(XMAX-XMIN)/(double)HS ;
	yGmax = YMAX - (yPos-IVS)*(YMAX-YMIN)/(double)VS ;
	repaint() ;
    }

    public void coordenadas(int pinta, int x, int y) {
	ipinta = pinta;
	xPos = x;
	yPos = y;
	repaint();
    }

    public void limpia () {
	ipinta = 0;
	repaint();
    }

    public void paintComponent( Graphics g ) {

	super.paintComponent( g );

        itype = Simulation.itype ;
	ii = 0 ;
	for (i=nmin;i<=nmax;i++) {
	    xx[i] = (double)i*dr;
	    yy[i] =  Simulation.gr1[i];
	    if (yy[i] > 0.00) {
		ii = ii + 1 ;
		x1[ii] = xx[i] ;
		y1[ii] = yy[i] ;
	    }
	    nnmax = ii ;
	}
	if (Simulation.gr1[0] > YMAXO) {
	    YMAX = (double)((int)(Simulation.gr1[0]+1.0));
	}
	else {
	    YMAX = YMAXO ;
	}
	if (YMAX > 6.0) YMAX = 6.0;
	gr.drawGraph(g,IHS,IVS,VS,HS,XMIN,XMAX,YMIN,YMAX,"A-A","r",1.0);
	g.setColor(colorA) ;
	gr.drawGraphLine(g, x1, y1,  nmin, nnmax,IHS,IVS,VS,HS,XMIN,XMAX,YMIN,YMAX,IHS+HS+10,IVS+VS/2,IHS+HS+40,IVS+VS/2,"A-A");
	xx[1] = Simulation.rmin;
	xx[2] = Simulation.rmin;
	yy[1] = 0;
	yy[2] = YMAX;
	g.setColor(new Color(24,126,26)) ;
	gr.drawDashLine(g, xx, yy, 1, 2,IHS,IVS,VS,HS,XMIN,XMAX,YMIN,YMAX) ;
	xx[1] = Simulation.rmax;
	xx[2] = Simulation.rmax;
	yy[1] = 0;
	yy[2] = YMAX;
	gr.drawDashLine(g, xx, yy,  1, 2,IHS,IVS,VS,HS,XMIN,XMAX,YMIN,YMAX);

	if (itype > 1 || Simulation.iheter == 1) {
	    ii = 0 ;
	    for (i=nmin;i<=nmax;i++) {
		xx[i] = (double)i*dr;
		yy[i] =  Simulation.gr2[i];
		if (yy[i] > 0.00) {
		    ii = ii + 1 ;
		    x1[ii] = xx[i] ;
		    y1[ii] = yy[i] ;
		}
		nnmax = ii ;
	    }
	    g.setColor(colorB) ;
	    gr.drawGraphLine(g, x1, y1,  nmin, nnmax,IHS,IVS,VS,HS,XMIN,XMAX,YMIN,YMAX,IHS+HS+10,20+IVS+VS/2,IHS+HS+40,20+IVS+VS/2,"A-B");
	}
      if (ipinta == 1) {
	  g.setColor(Color.BLUE);
	  g.setFont( new Font("SansSerif", Font.BOLD,16));
	  //	  g.setFont( new Font("Courier", Font.BOLD,16));
	  g.drawString("r = "+tresDigitos.format(xDist),IHS+0,5*IVS/2+VS);
	  g.drawString("g = "+tresDigitos.format(yGmax),IHS+0,6*IVS/2+VS);
      }
    }

   // validate and set diameter, then repaint 
   public void setGrange( int newGrange ) {
       nmax = Math.min(newGrange,(int)(XMAX/dr+0.5));
       repaint();
   }


   // used by layout manager to determine preferred size
   public Dimension getPreferredSize() {
       return new Dimension( 200, 200 );
   }


   // used by layout manager to determine minimum size
   public Dimension getMinimumSize() {
       return getPreferredSize();
   }

    } // end class PlotGr

