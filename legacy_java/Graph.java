import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
import java.text.DecimalFormat;

public class Graph {
    DecimalFormat unDigito ;
    int ix[],iy[];
    
    Graph () {
	unDigito = new DecimalFormat("0.0") ;
	ix = new int[2000];
	iy = new int[2000];
    }

    public void drawGraph(Graphics g, int ihs,int ivs,int vs,int hs,double xmin, double xmax, double ymin, double ymax, String label,String label2, double baseline) {

	double a, b, ap, bp;
	a = (xmax-xmin)/(double)hs;
	b = xmin-a*(double)ihs;
	ap = -(ymax-ymin)/(double)vs;
	bp = ymax-ap*(double)ivs;
	g.setColor(Color.BLACK);
	g.setFont( new Font("SansSerif", Font.BOLD,16));
	g.drawString(unDigito.format(xmin),ihs,vs+3*ivs/2);
	g.drawString(unDigito.format(xmax),ihs+hs,vs+3*ivs/2);
	g.drawString(unDigito.format(ymax),ihs-ivs-5,ivs+5);
	g.drawString(unDigito.format(ymin),ihs-ivs-5,vs+ivs+5);
	g.setFont( new Font("TimesRoman", Font.BOLD,16));
	g.setColor(new Color(175,0,50));
	g.drawString(label2,ihs+hs/2,10+vs+3*ivs/2);
	g.drawString(label,ihs-40,ivs+vs/2);
	g.setColor(Color.BLACK);
	g.setFont( new Font("SansSerif", Font.BOLD,18));
	g.drawRect(ihs,ivs,hs,vs);
	setTicks(g, 4, 4, ihs, ivs+vs, hs, vs, 0);
	setTicks(g, 4, 4, ihs, ivs+10, hs, vs+10, 1);
	setTicks(g, 4, 4, ihs+hs-10, ivs+vs, hs, vs, 2);
	g.drawLine((int)((xmin-b)/a+0.51),(int)((baseline-bp)/ap+0.51),(int)((xmax-b)/a+0.51),(int)((baseline-bp)/ap+0.51));
    }
    
    public void drawGraphLine(Graphics g, double[] x, double[] y, int nmn, int n, int ihs,int ivs,int vs,int hs,double xmin, double xmax, double ymin, double ymax, int ixl, int iyl, int ixl1, int iyl1, String label ) {
	double a, b, ap, bp, r0,y0,r1, y1;
	int ix0, ix1, iy0, iy1, i ;
	a = (xmax-xmin)/(double)hs;
	b = xmin-a*(double)ihs;
	ap = -(ymax-ymin)/(double)vs;
	bp = ymax-ap*(double)ivs;
	r0 = x[nmn];
	y0 = y[nmn];
	ix0 = (int)((r0-b)/a+0.51);
	iy0 = (int)((y0-bp)/ap+0.51);
	g.drawLine(ixl,iyl,ixl1,iyl1);
	g.setFont( new Font("SansSerif", Font.BOLD,16));
	g.drawString(label,ixl+35,iyl1+5);
	for (i=nmn; i < n ; i++) {
	    r1 = x[i+1];
	    y1 = y[i+1];
	    ix1 = (int)((r1-b)/a+0.51);
	    iy1 = (int)((y1-bp)/ap+0.51);
	    g.drawLine(ix0,iy0,ix1,iy1);
	    ix0 = ix1;
	    iy0 = iy1;
	}
    }

    public void drawDashLine(Graphics g, double[] x, double[] y, int nmn, int n, int ihs,int ivs,int vs,int hs,double xmin, double xmax, double ymin, double ymax) {
	double a, b, ap, bp, r0,y0,r1, y1;
	int ix0, ix1, iy0, iy1, i ;
	Graphics2D g2d = (Graphics2D) g;
	a = (xmax-xmin)/(double)hs;
	b = xmin-a*(double)ihs;
	ap = -(ymax-ymin)/(double)vs;
	bp = ymax-ap*(double)ivs;
	r0 = x[nmn];
	y0 = y[nmn];
	ix0 = (int)((r0-b)/a+0.51);
	iy0 = (int)((y0-bp)/ap+0.51);
	float dashes[] = {4};
	g2d.setStroke(new BasicStroke(1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,10,dashes,0));
	for (i=nmn; i < n ; i++) {
	    r1 = x[i+1];
	    y1 = y[i+1];
	    ix1 = (int)((r1-b)/a+0.51);
	    iy1 = (int)((y1-bp)/ap+0.51);
	    g2d.draw(new Line2D.Double(ix0,iy0,ix1,iy1));
	    ix0 = ix1;
	    iy0 = iy1;
	}
	g2d.setStroke(new BasicStroke(1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
    }

    public void drawGraphPoly(Graphics g, double[] x, double[] y, int nmn, int n, int ihs,int ivs,int vs,int hs,double xmin, double xmax, double ymin, double ymax) {
	double a, b, ap, bp, r0,y0,r1, y1;
	int ix0, ix1, iy0, iy1, i ;
	a = (xmax-xmin)/(double)hs;
	b = xmin-a*(double)ihs;
	ap = -(ymax-ymin)/(double)vs;
	bp = ymax-ap*(double)ivs;
	for (i=nmn; i <= n ; i++) {
	    r1 = x[i];
	    y1 = y[i];
	    ix[i] = (int)((r1-b)/a+0.51);
	    iy[i] = (int)((y1-bp)/ap+0.51);
	}
	g.fillPolygon(ix,iy,n-nmn+1);

    }

    public void drawGraphPoint(Graphics g, double x, double y,  int ihs,int ivs,int vs,int hs,double xmin, double xmax, double ymin, double ymax) {
	double a, b, ap, bp, r0,y0,r1, y1;
	int ix0, ix1, iy0, iy1, i, itam=8 ;
	a = (xmax-xmin)/(double)hs;
	b = xmin-a*(double)ihs;
	ap = -(ymax-ymin)/(double)vs;
	bp = ymax-ap*(double)ivs;
	r0 = x;
	y0 = y;
	ix0 = (int)((r0-b)/a+0.51);
	iy0 = (int)((y0-bp)/ap+0.51);
	g.fillOval(ix0-itam/2,iy0-itam/2,itam,itam);
    }

    public void drawGraphCPoint(Graphics g, double x, double y,  int ihs,int ivs,int vs,int hs,double xmin, double xmax, double ymin, double ymax) {
	double a, b, ap, bp, r0,y0,r1, y1;
	int ix0, ix1, iy0, iy1, i, itam=8 ;
	a = (xmax-xmin)/(double)hs;
	b = xmin-a*(double)ihs;
	ap = -(ymax-ymin)/(double)vs;
	bp = ymax-ap*(double)ivs;
	r0 = x;
	y0 = y;
	ix0 = (int)((r0-b)/a+0.51);
	iy0 = (int)((y0-bp)/ap+0.51);
	g.drawOval(ix0-itam/2,iy0-itam/2,itam,itam);
    }


    public void setTicks(Graphics g, int ntx, int nty, int ixor, int iyor, int hsize, int vsize, int kaso ) {
	int i, idx, idy;

	if (kaso == 0 ) {
	    idx = hsize/(ntx+1);
	    for (i = 1 ; i <= ntx ; i++) {
		g.drawLine(ixor+i*idx,iyor,ixor+i*idx,iyor-10);
	    }
	    idy = vsize/(nty+1);
	    for (i = 1 ; i <= nty ; i++) {
		g.drawLine(ixor,iyor-i*idy,ixor+10,iyor-i*idy);
	    }
	}
	else if (kaso == 1) { 
	    idx = hsize/(ntx+1);
	    for (i = 1 ; i <= ntx ; i++) {
		g.drawLine(ixor+i*idx,iyor,ixor+i*idx,iyor-10);
	    }
        }
	else if (kaso == 2) {
	    idy = vsize/(nty+1);
	    for (i = 1 ; i <= nty ; i++) {
		g.drawLine(ixor,iyor-i*idy,ixor+10,iyor-i*idy);
	    }
	}
    }
}
