import java.io.*;
import java.awt.*;
import java.net.*;
import java.awt.image.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
//
// Modulo de impresion de configuraciones instantaneas de la simulacion
// del programa de simulacion Simulation
//
//
public class Print2D implements Printable {
    private static int RHSIZE, RVSIZE, VSIZE, HSIZE;
    private static int NMOL, SIZE, itype, ix[],iy[];
    public Print2D() {

	/* Construct the print request specification.
	 * The print data is a Printable object.
	 * the request additonally specifies a job name, 1 copy, and
	 * portrait orientation of the media.
	 */
	RHSIZE = Simulation.RHSIZE;
	RVSIZE = Simulation.RVSIZE;
	HSIZE = Simulation.HSIZE;
	VSIZE = Simulation.VSIZE;
	SIZE = Simulation.SIZE;
	NMOL = Simulation.NMOL;
	itype = Simulation.itype;
	ix = new int[NMOL+1];
	iy = new int[NMOL+1];
	PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
	aset.add(OrientationRequested.PORTRAIT);
	aset.add(new Copies(1));
	
	/* Create a print job */
	PrinterJob pj = PrinterJob.getPrinterJob();       
	pj.setPrintable(this);
	/* locate a print service that can handle the request */
	PrintService[] services =
	    PrinterJob.lookupPrintServices();
	File f = new File("SnapShot"+Integer.toString(Simulation.ib)+".ps");
	aset.add(new Destination(f.toURI()));
						

	if (services.length > 0) {
	    //	    System.out.println("selected printer " + services[0].getName());
	    try {
		pj.setPrintService(services[0]);
//				pj.pageDialog(aset);
				if(pj.printDialog(aset)) {
				    pj.print(aset);
				}
	    } catch (PrinterException pe) { 
		System.err.println(pe);
	    }
	}
    }
    
    public int print(Graphics g,PageFormat pf,int pageIndex) {
	int i,rhsize, rvsize, hsize, vsize, xorg, yorg, size;
	double ratio;
	if (pageIndex == 0) {
 	    Graphics2D g2d= (Graphics2D)g;
//
// Reescalado de coordenadas al dispositivo de impresion
//
 	    g2d.translate(pf.getImageableX(), pf.getImageableY()); 
	    rhsize = (int)(pf.getImageableWidth()+0.51);
	    rvsize = (int)(pf.getImageableHeight()+0.51);
	    rhsize = Math.min(rhsize,rvsize);
	    ratio = (double)rhsize/(double)RHSIZE;
	    size = (int)((double)SIZE*ratio+0.51);
	    hsize = (int)((double)HSIZE*ratio+0.51);
	    for ( i=1; i<=NMOL; i++) {
		ix[i] = (int)((double)Simulation.ix[i]*ratio+0.51);
		iy[i] = (int)((double)Simulation.iy[i]*ratio+0.51);
	    }
//
// El dibujo es cuadrado
//
	    rvsize = rhsize;
	    vsize = hsize;
	    g2d.setColor( Color.BLACK );
	    g2d.drawRect(0,0,rhsize,rvsize);
	    Simulation.pintacaja(g2d,hsize,vsize,rhsize,rvsize,size);

	    Simulation.dibuja(g2d, ix, iy, Simulation.ax,Simulation.ay,NMOL, hsize, vsize, rhsize, rvsize, size,itype, Simulation.idum, Simulation.elon, Simulation.natcm);
//
//   Titulos
//
	    g2d.setColor( Color.BLACK );
	    g2d.setFont( new Font("SansSerif", Font.ITALIC,16));
	    g2d.drawString(DiMol2D.messages.getString("pasodesimulacion")+" = "+Integer.toString(Simulation.ib),0,rhsize+40);
	    g2d.drawString(DiMol2D.messages.getString("densidad")+" = "+Double.toString(Simulation.densidad),0,rhsize+60);
	    g2d.drawString(DiMol2D.messages.getString("temperatura")+" = "+Double.toString(Simulation.temperatura),0,rhsize+80);
	
	    return Printable.PAGE_EXISTS;					
	}
	else {
	    return Printable.NO_SUCH_PAGE;
	}
    }

}
