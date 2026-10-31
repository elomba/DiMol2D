
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

// import javax.swing.*;
// import javax.swing.event.*;
// import javax.swing.text.*;
// import javax.swing.border.*;
// import javax.swing.colorchooser.*;
// import javax.swing.filechooser.*;

import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.text.DecimalFormat;
import java.net.*;
import java.net.URL;

public class Simulation extends SiModulos implements Runnable
{
    public static JRadioButton items[], button;
    DecimalFormat dosDigitos;
    static Image imagen, dm2d, contador, d0[];
    JLabel label1,label2,label3;
    Image offImg;
    public static Point pos;
    public static int indexrun,indexrun1=0,indexrun2=0;
    private JFrame ventana;
    public native void jsimul(int iarrayx[], int[] iarrayy, double[] arrayx, double[] arrayy, double[] gaa, double[] gab,  double dr, double tst, double rcaa1, double rcab1, double temp, double dens, double sca, int ifr, int ncomp, int ilent, int ngr, int ngry, int isize, int irsize, int inc, int ncp1, double[] ga1, double[] ga2, double ga3[], double ga4[], double rw1, double rw2, double rw3, double rwy3, double eps, int ipa, int ipay, double fgy, int natcm[],  int idum, double elon, int iheter );

    static double pres, temperatura, densidad, densidad_o, rcaa=2.5, rcab=1.122462, tistep=0.005,dip2;
    public static double xmola=1.0, xmolb=0.0, xmolc ;
    public static double rmin=0.95,rmax=1.35, drg=0.02,drgx,drgy,rimin, rimax, rsize, riimax, riimin;
    public static int ipmol=1, ipbon=0, ipgr=0, ipdx=0,ipdy=0,iinter=0, icompos=1, isobr=0,iay=0,istate=0,ippd=0,itt=0, NSLEEP=25;
    static int icontrol, iheter=0, itype=1,itype_old=1,istop=0, itset=0, irhoset=0, irho, icrho=0, icstate=0, inic, inicp=1, ifrsca=1, ncp=10, inpp=0, icpan=0 ;
    static String  message="H";
    public String inicios, parado;
    public static int natcm[], natcm_old[];
    public String colors[] = { "Inicio", "Pausa", "Sigue", "Fin"};
    static int ix[], iy[], ip[];
    static double scaflg = 1.00 ;
    private double fgy;
    static double ax[], ay[], rmux[], rmuy[], gr1[], gr2[], gw1[], gw2[], gw3[], gwy[], rw1=4.165, rw2=4.165, rw3=1.165, rwy=1.165, epsw=2.00, fgy0=-0.02, elon=1.0, tstate[], rstate[];
    static int itemp, iwall=0, iwally =0, igrav=0,itempold=0, IRHOINIT=0 ;
    static int NMOL=450, NMOLMIN=150, NMOLMAX=1050, NMSTP=150, NMOLN, icn=0, NGRMAX=5000, NGRMAXY=5000,ngrm, ngrmy;
    static int HSIZE=800, VSIZE=800, SIZE=20;
    static int RHSIZE=1000, RVSIZE=1000;
    static int idum=1, idum_old=2, ib, ic, istart=0,ipause=0, xmloc, ymloc, dimX, dimY ;
    public  Thread molThread;
    public PopMenu npop;
    public JPopupMenu popupMenu;
    Graphics offGr;
    public Simulation ()
    {
	int i, ibold;
	//
	// Ajusta el panel al 80% de la altura de la pantalla
	//
	URL url = getClass().getResource("Image/drops21.jpg");
	imagen = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/DM2D1-1.png");
	dm2d = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/contador3.gif");
	contador = Toolkit.getDefaultToolkit().getImage(url);
	ip = new int[10];
	d0 = new Image[10];
	url = getClass().getResource("Image/0.gif");
	d0[0] = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/1.gif");
	d0[1] = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/2.gif");
	d0[2] = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/3.gif");
	d0[3] = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/4.gif");
	d0[4] = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/5.gif");
	d0[5] = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/6.gif");
	d0[6] = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/7.gif");
	d0[7] = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/8.gif");
	d0[8] = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Image/9.gif");
	d0[9] = Toolkit.getDefaultToolkit().getImage(url);

	pos = new Point(0,0);
	dosDigitos = new DecimalFormat("0.00");
	
	ix     = new int[NMOLMAX+1];
	iy     = new int[NMOLMAX+1];
	ax     = new double[NMOLMAX+1];
	ay     = new double[NMOLMAX+1];
	rmux   = new double[NMOLMAX+1];
	rmuy   = new double[NMOLMAX+1];
	gr1    = new double[NGRMAX];
	gr2    = new double[NGRMAX];
	gw1    = new double[NGRMAX];
	gw2    = new double[NGRMAX];
	gw3    = new double[NGRMAX];
	gwy    = new double[NGRMAXY];
	tstate = new double[1000];
	rstate = new double[1000];
	natcm  = new int[10];
	natcm_old  = new int[10];
	natcm[1] = NMOL;
	//
	// Add popup menu
	//
	icpan = 1;
	npop = new PopMenu();
	icpan = 0;
    }

    public Dimension getPreferredSize()
   {
       return new Dimension( RHSIZE, RVSIZE );
   }






    public  void startProgram()
    {
	if ( itset != 0 && irhoset != 0 ) {
	    if ( istart == 0 || ipause == 1 ) {
		ipause=0;
		icrho = 0;
		icstate = 0;
		Interaccion.setPause(ipause);
		molThread = new Thread(Simulation.this);
		molThread.start();
		istart = 1;
		Interaccion.setStart(istart);

	    }
	    else { 
		JOptionPane.showMessageDialog(Simulation.this,DiMol2D.messages.getString("progrunning"));
	    };
	}
	else {
	    JOptionPane.showMessageDialog(Simulation.this,DiMol2D.messages.getString("hayqtd"));
	    //
	    // This deselects all radio buttons
	    //
	    button.setSelected(true);
	}
    }
    public synchronized void run()
    {
	int i;
	if ( molThread == null ) {
	    startProgram();
	}
	while ( molThread != null){
		//
		//
		if (istop != 0 ) {
		    ipause = 1;
		    Interaccion.setPause(ipause);
		    molThread = null;
		    istop = 0;
		}
		else {

		    if ( igrav == 0 ){
			fgy = 0.0;
		    }
		    else {
			fgy = fgy0;
		    }
		    natcm[0] = 0;
		    //		
		    //		    System.out.println(" natm "+natcm[1]+" "+natcm[2]+" "+natcm[3]);
		    jsimul(ix,iy,ax,ay,gr1,gr2,drg,tistep,rcaa,rcab,temperatura,densidad, scaflg,ifrsca,itype,NMOL+1,ngrm,ngrmy,HSIZE,RHSIZE,inic,ncp,gw1,gw2,gw3,gwy,rw1,rw2,rw3,rwy,epsw,iwall,iwally,fgy,natcm,idum,elon,iheter);
		    //		    System.out.println(" sale natm "+natcm[1]+natcm[2]+natcm[3]);
		    //
		    // jsimul devuelve la presion directamente atraves de un
		    // metodo de acceso JNI a variables estaticas.
		    //
		    indexrun1++;
		    if (indexrun1%20==0)DiMol2D.manoM.setPresure(pres);
		    //
		    // Reajusta desplazamiento en las coordenadas
		    //		   
		    for (i=1;i<=NMOL;i++){
			ix[i] = ix[i] - SIZE/2;
			iy[i] = iy[i] - SIZE/2;
		    }

		    inic = 1;
		    ib = ib +1;
		    repaint();
		    if (indexrun1%10 == 0) {
			DiMol2D.todo.repaint();
			if (iay == 1)  MetalworksHelp.chtml.repaint();
			if (ipgr == 1) DiMol2D.grf.repintaGr();
			if (ipdx == 1) DiMol2D.pdfx.repintaPdx();
			if (ipdy == 1) DiMol2D.pdfy.repintaPdy();
			if (ippd == 1 & DiMol2D.ppd != null ) DiMol2D.ppd.repintaPD();
			if (isobr == 1) DiMol2D.ap1.repintanos();
			if (DiMol2D.iinter == 1) DiMol2D.app2.repintaInter();
			if (DiMol2D.iinterw == 1) DiMol2D.app2w.repintaInterW();
			if (DiMol2D.invel == 1) DiMol2D.nveloc.repintaV();
		    }
		    try 
			{
			    wait ();
			    Thread.sleep(NSLEEP);
			}
		    catch (InterruptedException ie)
			{
			}
		}
	}


	    if ( ipause == 0 && molThread == null ){
		System.exit(0);
	    }
	}

    public void paintComponent(Graphics g)
    {
	super. paintComponent(g);
	Graphics2D gr2d;
	indexrun2++;
	gr2d = ( Graphics2D) g;
// 	Pinta el fondo de gotas
	gr2d.drawImage(imagen,0,0,Simulation.this);   

// Dibuja la caja de simulacion
	pintacaja(gr2d, HSIZE, VSIZE, RHSIZE, RVSIZE, SIZE);

        // Pinta las moleculas
	//
	if (istart != 0 ) {
	    if ( ((idum == 2 && idum_old == 1) ||  ((idum == 2 && idum_old == 2) && itype < itype_old)|| (idum == 2 && natcm[1] > natcm_old[1])) && ipause == 1 ) {
		dibuja(gr2d, ix, iy,  ax, ay, NMOL, HSIZE, VSIZE, RHSIZE, RVSIZE, SIZE,itype_old,idum_old,elon,natcm_old);
	    }
	    else {
		dibuja(gr2d, ix, iy,  ax, ay, NMOL, HSIZE, VSIZE, RHSIZE, RVSIZE, SIZE,itype,idum,elon,natcm);
	    }
	}

	if (istart == 0 || ipause == 1) {
	    drawrunningtext(gr2d, DiMol2D.messages.getString("runtexto"),DiMol2D.messages.getString("runtexto2"));
	}
	//
	// Pinta el contador
	//
	pintacontador(gr2d,ib);

 	synchronized(this)
 	    {
 		notifyAll();
 	    }
     }
    private void pintacontador(Graphics2D g,int ind){
	int i, ik, ikk, j, ipsum;
	ip[0] = ind%10 ;
	ik = 100000;
	ipsum = ip[0];
	ikk = ib ;
	for (i=5 ; i >= 1; i-- ) {
	    ip[i] = ikk/ik;
	    ikk = ikk%ik;
	    ik = ik/10;
	}
	g.drawImage(contador,RHSIZE-134,RVSIZE-42,Simulation.this);  
	for ( i = 0 ; i <= 5 ; i++ ) {
	    g.drawImage(d0[ip[5-i]],RHSIZE-124+i*19,RVSIZE-35,Simulation.this);
	}
    }
    private void drawrunningtext(Graphics2D g2d, String texto, String texto2) {
	if (indexrun < RVSIZE) {
	    indexrun++ ;
	}
	else {
	    indexrun=0 ;
	}
	g2d.setFont( new Font("SansSerif", Font.BOLD,25));
	g2d.setColor(new Color(57,129,128));
	g2d.drawString(texto,indexrun,indexrun);
	g2d.drawString(texto2,HSIZE-indexrun,VSIZE-indexrun+30);
	g2d.drawString(texto,HSIZE-indexrun,VSIZE-indexrun);
	g2d.drawString(texto2,indexrun,indexrun+30);
	g2d.drawImage(dm2d,indexrun,VSIZE-indexrun,Simulation.this);
	g2d.drawImage(dm2d,HSIZE-indexrun,indexrun,Simulation.this);

    }

    public static void pintacaja(Graphics2D g, int hsize,int vsize,int rhsize,int rvsize,int size) {
	int xorg, yorg;
	g.setColor( Color.BLUE );
	xorg = (rhsize-hsize)/2;
	yorg = (rvsize-vsize)/2;
	g.setColor( Color.BLUE );
	if ( iwally != 1) {	
	    g.drawLine(0,yorg,rhsize,yorg);
	    g.drawLine(0,yorg+vsize,rhsize,yorg+vsize);
	}
	if ( iwall != 1) {
	    g.drawLine(xorg,0,xorg,rvsize);
	    g.drawLine(xorg+hsize,0,xorg+hsize,rvsize);
	}
	if ( iwall == 1) {
	    g.setColor(colorW);
	    g.fillRect(0,0,xorg-(int)(0.835*size)/2,rvsize);
	    g.fillRect(xorg+hsize+(int)(0.835*size)/2,0,rhsize,rvsize);
	}
	if ( iwally == 1) {
	    g.setColor(colorW);
	    g.fillRect(0,0,rhsize+50,yorg-(int)(0.835*size)/2);
	    g.fillRect(0,yorg+vsize+(int)(0.835*size)/2,rhsize+50,rvsize);
	}
		
    }    
    public static void dibuja(Graphics2D g2d, int lx[], int ly[], double ax[], double ay[], int nmol, int hsize,int vsize,int rhsize,int rvsize,int size, int ityp, int idumb, double elong, int natcm[]) {
	int i,j, ir, vecx,vecy;
	double dx, dy, r;
	if ( ipmol == 1) {
	    if ( ityp == 1 ) {
		if (idumb == 1) {
		    for (i = 1 ; i <= nmol ; i++ ) { 
			pintamolecula(g2d, lx[i],ly[i],hsize,vsize,rhsize,rvsize,size,colorA);
		    }
		}
		else {
		    for (i = 1 ; i <= nmol-1 ; i=i+2 ) { 
			drawcirclerectang(g2d,lx[i],lx[i+1],ly[i],ly[i+1],hsize,vsize,rhsize,rvsize,size,colorA,elong);
			pintacirclerectang(g2d,lx[i],lx[i+1],ly[i],ly[i+1],hsize,vsize,rhsize,rvsize,size,colorA,colorB,colorC,iheter,elong);
		    } 
		}
		    
	    }
	    else if ( ityp == 2) {
		for (i = 1 ; i <= natcm[1] ; i++ ) { 
		    pintamolecula(g2d,lx[i],ly[i],hsize,vsize,rhsize,rvsize,size,colorA);
		}
            	if (idumb == 1) {
		    for (i = natcm[1]+1 ; i <= nmol ; i++ ) { 
			pintamolecula(g2d,lx[i],ly[i],hsize,vsize,rhsize,rvsize,size,colorB);
		    }
		}
		else {

		    for (i = 1 ; i <= natcm[1] ; i=i+2 ) { 
			drawcirclerectang(g2d,lx[i],lx[i+1],ly[i],ly[i+1],hsize,vsize,rhsize,rvsize,size,colorA,elong);
			pintacirclerectang(g2d,lx[i],lx[i+1],ly[i],ly[i+1],hsize,vsize,rhsize,rvsize,size,colorA,colorB,colorC,iheter,elong);
		    } 
		    for (i = natcm[1]+1 ; i <= nmol ; i++ ) { 
			pintamolecula(g2d,lx[i],ly[i],hsize,vsize,rhsize,rvsize,size,colorB);
		    }
		}
	    }
	    else if ( ityp == 3) {
		if (idumb == 1) {
		    for (i = 1 ; i <= natcm[1]  ; i++ ) { 
			pintamolecula(g2d,lx[i],ly[i],hsize,vsize,rhsize,rvsize,size,colorA);
		    }
		    for (i = natcm[1]+1 ; i <= natcm[2] ; i++ ) { 
			pintamolecula(g2d,lx[i],ly[i],hsize,vsize,rhsize,rvsize,size,colorB);
		    }
		    for (i = natcm[2]+1 ; i <= nmol ; i++ ) { 
			pintamolecula(g2d,lx[i],ly[i],hsize,vsize,rhsize,rvsize,size,colorC);
		    }
		}
		else {

		    for (i = 1 ; i <= natcm[1] ; i=i+2 ) { 
			drawcirclerectang(g2d,lx[i],lx[i+1],ly[i],ly[i+1],hsize,vsize,rhsize,rvsize,size,colorA,elong);
			pintacirclerectang(g2d,lx[i],lx[i+1],ly[i],ly[i+1],hsize,vsize,rhsize,rvsize,size,colorA,colorB,colorC,iheter,elong);
		    } 
	    		    for (i = natcm[1]+1 ; i <= natcm[2] ; i++ ) { 
			pintamolecula(g2d,lx[i],ly[i],hsize,vsize,rhsize,rvsize,size,colorB);
		    }
		    for (i = natcm[2]+1 ; i <= nmol ; i++ ) { 
			pintamolecula(g2d,lx[i],ly[i],hsize,vsize,rhsize,rvsize,size,colorC);
		    }
		}
	    }
	}
	
	g2d.setColor( Color.BLACK ); 

	for (i = 1 ; i <= nmol ; i++ ) {
	    if ( ipbon == 1 ) {
		for (j = i+1;j<=nmol;j++) {
		    dx = (ax[i]-ax[j]);
		    dy = (ay[i]-ay[j]);
		    r = dx*dx+dy*dy;
		    if ( r >= rimin && r <= rimax) {
			g2d.drawLine(lx[i]+size/2,ly[i]+size/2,lx[j]+size/2,ly[j]+size/2);
		    }
		}
	    }
	}
    }
    
    public static void pintamolecula(Graphics2D g, int x, int y, int hsize, int vsize, int rhsize, int rvsize, int size,Color colormol)
	{
	    g.setPaint(colormol);
	    g.fillOval( x, y, size, size );

	    g.setPaint(Color.BLACK);
	    g.drawOval( x, y, size, size );
	    pinta_offbounds(g,x,y,hsize,vsize,rhsize,rvsize,size,colormol);
	}

    public static void pintacirclerectang(Graphics2D g, int x1, int x2, int y1, int y2, int hsize, int vsize, int rhsize, int rvsize, int size,Color colormol, Color colormol1, Color colormol2, int iheter, double elong)
    
	{
	    double ex, ey, length;
	    int angle, width;

	    if (iheter == 1) {
		//		g.setPaint(colormol2);
		//		g.fillOval( x2, y2, size, size );
		g.setPaint(colormol1);
		if (elong < 0.48) {
		    if (Math.abs(x1-x2) < hsize/2 && Math.abs(y1-y2) < vsize/2) {
			ex = (double)(x2-x1);
			ey = (double)(y2-y1);
			length = Math.sqrt(ex*ex+ey*ey);
			angle = (int)(52.296*Math.asin(ex/length));
			if (ey < 0) angle=180-angle;
			angle = angle-20;
			width = 180 + 40;
			g.fillArc( x1, y1, size, size,angle,width);
			pinta_offbounds_arc(g,x1,y1,hsize,vsize,rhsize,rvsize,size,colormol1,angle,width);
			g.setPaint(colormol2);
			g.fillArc( x2, y2, size, size,angle+180,width );
			pinta_offbounds_arc(g,x2,y2,hsize,vsize,rhsize,rvsize,size,colormol2,angle+180,width);
		    }
		    else {
			ex = (double)(x2-x1);
			ex = ex - (double)hsize*Math.round((double)(x2-x1)/(double)hsize);
			ey = (double)(y2-y1);
			ey = ey - (double)vsize*Math.round((double)(y2-y1)/(double)vsize);
			length = Math.sqrt(ex*ex+ey*ey);
			angle = (int)(52.296*Math.asin(ex/length));
			if (ey < 0) angle=180-angle;
			
			angle = angle-20;
			width = 180+40;
			pinta_offbounds_arc(g,x1,y1,hsize,vsize,rhsize,rvsize,size,colormol1,angle,width);
			g.fillArc( x1, y1, size, size,angle,width );
			g.setPaint(colormol2);
			g.fillArc( x2, y2, size, size,angle+180,width);
			pinta_offbounds_arc(g,x2,y2,hsize,vsize,rhsize,rvsize,size,colormol2,angle+180,width);
		    }
		}
		else {
		    pinta_offbounds(g,x1,y1,hsize,vsize,rhsize,rvsize,size,colormol1);
		    g.fillOval( x1, y1, size, size );
		    g.setPaint(colormol2);
		    g.fillOval( x2, y2, size, size );
		    pinta_offbounds(g,x2,y2,hsize,vsize,rhsize,rvsize,size,colormol2);
		}		    
	    }
	    else {
		g.setPaint(colormol);
		pinta_offbounds(g,x1,y1,hsize,vsize,rhsize,rvsize,size,colormol);
		pinta_offbounds(g,x2,y2,hsize,vsize,rhsize,rvsize,size,colormol);
		g.fillOval( x1, y1, size, size );
		g.fillOval( x2, y2, size, size );
	    }
	    g.setPaint(colormol);
	    fillrectangf(g,x1,x2,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	    pinta_offbounds_rec(g,x1,x2,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	    g.setPaint(Color.BLACK);
	}

     public static void fillrectangf(Graphics2D g, int x1, int x2, int y1, int y2, int hsize, int vsize, int rhsize, int rvsize, int size,Color colormol,Color colormol1, Color colormol2, int iheter)
    { 

	    if ( Math.abs(x1-x2) > hsize/2 ){
		if (x1 < hsize/2) {
		    fillrectang(g,x1,x2-hsize,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
		    fillrectang(g,x1+hsize,x2,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
		}
		else {
		    fillrectang(g,x1-hsize,x2,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
		    fillrectang(g,x1,x2+hsize,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
		}
	    }
	    else if ( Math.abs(y1-y2) > vsize/2  ) {

		if (y1 < vsize/2) {
		    fillrectang(g,x1,x2,y1,y2-vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
		    fillrectang(g,x1,x2,y1+vsize,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
		}
		else {
		    fillrectang(g,x1,x2,y1-vsize,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
		    fillrectang(g,x1,x2,y1,y2+vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
		}

	    }
	    else {
		fillrectang(g,x1,x2,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	    }

    }


    public static void fillrectang(Graphics2D g, int x1, int x2, int y1, int y2, int hsize, int vsize, int rhsize, int rvsize, int size,Color colormol,Color colormol1, Color colormol2, int iheter)
    {
	
	    int xv[], yv[];
	    int  xs, ys, ym, xm;
	    double ex, ey, length;
	    xv = new int[5];
	    yv = new int[5];
	    ex = (double)(x2-x1);
	    ey = (double)(y2-y1);
	    length = Math.sqrt(ex*ex+ey*ey);
	    ex = ex/length;
	    ey = ey/length;
	    xs = (int)(ex*(double)size/2.0);
	    ys = (int)(ey*(double)size/2.0);
	    if (iheter == 0) {
		xv[0] = x1+ys+(int)((double)size/2.0);
		yv[0] = y1-xs+(int)((double)size/2.0);
		xv[1] = x1-ys+(int)((double)size/2.0);
		yv[1] = y1+xs+(int)((double)size/2.0);
		xv[2] = x2-ys+(int)((double)size/2.0);
		yv[2] = y2+xs+(int)((double)size/2.0);
		xv[3] = x2+ys+(int)((double)size/2.0);
		yv[3] = y2-xs+(int)((double)size/2.0);
		xv[4] = xv[0];
		yv[4] = yv[0];
		if ( Math.abs(x1-x2) < hsize/2 &&  Math.abs(y1-y2) < vsize/2) {
		    g.fillPolygon(xv,yv,4);
		}
	    }
	    else {
		xm = (int)((double)(x1+x2)/2.0);
		ym = (int)((double)(y1+y2)/2.0);
		xv[0] = x1+ys+(int)((double)size/2.0);
		yv[0] = y1-xs+(int)((double)size/2.0);
		xv[1] = x1-ys+(int)((double)size/2.0);
		yv[1] = y1+xs+(int)((double)size/2.0);
		xv[2] = xm-ys+(int)((double)size/2.0);
		yv[2] = ym+xs+(int)((double)size/2.0);
		xv[3] = xm+ys+(int)((double)size/2.0);
		yv[3] = ym-xs+(int)((double)size/2.0);
		xv[4] = xv[0];
		yv[4] = yv[0];
		g.setPaint(colormol1);
		if ( Math.abs(x1-x2) < hsize/2 &&  Math.abs(y1-y2) < vsize/2) {
		    g.fillPolygon(xv,yv,4);
		}

		xv[0] = xm+ys+(int)((double)size/2.0);
		yv[0] = ym-xs+(int)((double)size/2.0);
		xv[1] = xm-ys+(int)((double)size/2.0);
		yv[1] = ym+xs+(int)((double)size/2.0);
		xv[2] = x2-ys+(int)((double)size/2.0);
		yv[2] = y2+xs+(int)((double)size/2.0);
		xv[3] = x2+ys+(int)((double)size/2.0);
		yv[3] = y2-xs+(int)((double)size/2.0);
		xv[4] = xv[0];
		yv[4] = yv[0];
  		g.setPaint(colormol2);
		if ( Math.abs(x1-x2) < hsize/2 &&  Math.abs(y1-y2) < vsize/2) {
		    g.fillPolygon(xv,yv,4);
		}
	    }
    } 

    public static void pinta_offbounds_rec(Graphics2D g, int x1, int x2, int y1, int y2, int hsize, int vsize, int rhsize, int rvsize, int size,Color colormol,Color colormol1, Color colormol2, int iheter)
    {
	g.setPaint(colormol);
	if (x1-hsize > 0 && iwall != 1 ) {
	    fillrectangf(g,x1-hsize,x2-hsize,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	    if (y1-vsize > 0 && iwally != 1)fillrectangf(g,x1-hsize,x2-hsize,y1-vsize,y2-vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	    if (y1+vsize < rvsize && iwally != 1)fillrectangf(g,x1-hsize,x2-hsize,y1+vsize,y2+vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	}
	if (x1+hsize < rhsize && iwall != 1 ) {
	    fillrectangf(g,x1+hsize,x2+hsize,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	    if (y1-vsize > 0 && iwally != 1) fillrectangf(g,x1+hsize,x2+hsize,y1-vsize,y2-vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter); 
		if (y1+vsize < rvsize && iwally != 1) fillrectangf(g,x1+hsize,x2+hsize,y1+vsize,y2+vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter); 
	    }
	    if (y1-vsize > 0 && iwally != 1) fillrectangf(g,x1,x2,y1-vsize,y2-vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter); 
	    if (y1+vsize < rvsize && iwally != 1) fillrectangf(g,x1,x2,y1+vsize,y2+vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter); 

	if (x2-hsize > 0 && iwall != 1 ) {
	    fillrectangf(g,x1-hsize,x2-hsize,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	    if (y2-vsize > 0 && iwally != 1)fillrectangf(g,x1-hsize,x2-hsize,y1-vsize,y2-vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	    if (y2+vsize < rvsize && iwally != 1)fillrectangf(g,x1-hsize,x2-hsize,y1+vsize,y2+vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	}
	if (x2+hsize < rhsize && iwall != 1 ) {
	    fillrectangf(g,x1+hsize,x2+hsize,y1,y2,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter);
	    if (y2-vsize > 0 && iwally != 1) fillrectangf(g,x1+hsize,x2+hsize,y1-vsize,y2-vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter); 
		if (y2+vsize < rvsize && iwally != 1) fillrectangf(g,x1+hsize,x2+hsize,y1+vsize,y2+vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter); 
	    }
	    if (y2-vsize > 0 && iwally != 1) fillrectangf(g,x1,x2,y1-vsize,y2-vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter); 
	    if (y2+vsize < rvsize && iwally != 1) fillrectangf(g,x1,x2,y1+vsize,y2+vsize,hsize,vsize,rhsize,rvsize,size,colormol,colormol1,colormol2,iheter); 



    }

    public static void pinta_offbounds(Graphics2D g, int x, int y, int hsize, int vsize, int rhsize, int rvsize, int size,Color colormol)
    {
	g.setPaint(colormol);
	if (x-hsize > 0 && iwall != 1 ) {
		g.fillOval( x-hsize, y, size, size );
		if (y-vsize > 0 && iwally != 1)g.fillOval( x-hsize, y-vsize, size, size );
		if (y+vsize < rvsize && iwally != 1) g.fillOval( x-hsize, y+vsize, size, size );
	    }
	    if (x+hsize < rhsize && iwall != 1 ) {
		g.fillOval( x+hsize, y, size, size );
		if (y-vsize > 0 && iwally != 1) g.fillOval( x+hsize, y-vsize, size, size );
		if (y+vsize < rvsize && iwally != 1) g.fillOval( x+hsize, y+vsize, size, size );
	    }
	    if (y-vsize > 0 && iwally != 1) g.fillOval( x, y-vsize, size, size ) ;
	    if (y+vsize < rvsize && iwally != 1) g.fillOval( x, y+vsize, size, size );
    }

    public static void pinta_offbounds_arc(Graphics2D g, int x, int y, int hsize, int vsize, int rhsize, int rvsize, int size,Color colormol,int angle, int width)
    {
	g.setPaint(colormol);
	if (x-hsize > 0 && iwall != 1 ) {
		g.fillArc( x-hsize, y, size, size, angle, width );
		if (y-vsize > 0 && iwally != 1)g.fillArc( x-hsize, y-vsize, size, size, angle, width );
		if (y+vsize < rvsize && iwally != 1) g.fillArc( x-hsize, y+vsize, size, size, angle, width );
	    }
	    if (x+hsize < rhsize && iwall != 1 ) {
		g.fillArc( x+hsize, y, size, size, angle, width );
		if (y-vsize > 0 && iwally != 1) g.fillArc( x+hsize, y-vsize, size, size, angle, width );
		if (y+vsize < rvsize && iwally != 1) g.fillArc( x+hsize, y+vsize, size, size, angle, width );
	    }
	    if (y-vsize > 0 && iwally != 1) g.fillArc( x, y-vsize, size, size, angle, width ) ;
	    if (y+vsize < rvsize && iwally != 1) g.fillArc( x, y+vsize, size, size, angle, width );
    }


    public static void drawcirclerectang(Graphics2D g, int x1, int x2, int y1, int y2, int hsize, int vsize, int rhsize, int rvsize, int size,Color colormol, double elong)
	{
	    int xv[], yv[];
	    int xl2, yl2, xs, ys;
	    double ex, ey, length;
	    g.setPaint(Color.BLACK);
	    xv = new int[5];
	    yv = new int[5];
	    ex = (double)(x2-x1);
	    ey = (double)(y2-y1);
	    length = Math.sqrt(ex*ex+ey*ey);
	    ex = ex/length;
	    ey = ey/length;
	    xs = (int)(ex*(double)size/2.0);
	    ys = (int)(ey*(double)size/2.0);
	    xv[0] = x1+ys+(int)((double)size/2.0);
	    yv[0] = y1-xs+(int)((double)size/2.0);
	    xv[1] = x1-ys+(int)((double)size/2.0);
	    yv[1] = y1+xs+(int)((double)size/2.0);
	    xv[2] = x2-ys+(int)((double)size/2.0);
	    yv[2] = y2+xs+(int)((double)size/2.0);
	    xv[3] = x2+ys+(int)((double)size/2.0);
	    yv[3] = y2-xs+(int)((double)size/2.0);
	    xv[4] = xv[0];
	    yv[4] = yv[0];

	    g.drawOval( x1, y1, size, size );
	    g.drawOval( x2, y2, size, size );


		    if ( Math.abs(x1-x2) < hsize/2 &&  Math.abs(y1-y2) < vsize/2  ) {
			g.drawPolyline(xv,yv,5);
	    }
	   
	    g.setStroke(new BasicStroke(1.0f));
	}


    public static void pintavector(Graphics g, int x, int y, int vx, int vy, int hsize, int vsize, int rhsize, int rvsize, int size)
	{
	    g.drawLine(x-vx/2+size/2,y-vy/2+size/2,x+vx/2+size/2,y+vy/2+size/2);
	    //	    g.fillOval( x, y, size, size );
	    if (x-hsize > 0) {
		g.drawLine(x-vx/2-hsize+size/2,y-vy/2+size/2,x-hsize+vx/2+size/2,y+vy/2+size/2);
		if (y-vsize > 0)g.drawLine(x-vx/2-hsize+size/2,y-vsize+size/2,x-hsize+vx/2+size/2,y-vsize+vy/2+size/2);
		if (y-vy/2+vsize < rvsize) g.drawLine(x-vx/2-hsize+size/2,y+vsize+size/2,x-hsize+vx/2+size/2,y+vsize+vy/2+size/2);
	    }
	    if (x+hsize < rhsize) {
		g.drawLine(x-vx/2+hsize+size/2,y-vy/2+size/2,x+hsize+vx/2+size/2,y+vy/2+size/2);
		if (y-vsize > 0) g.drawLine(x-vx/2+hsize+size/2,y-vsize+size/2,x+hsize+vx/2+size/2,y-vsize+vy/2+size/2);
		if (y-vy/2+vsize < rvsize) g.drawLine(x-vx/2+hsize+size/2,y-vy/2+vsize+size/2,x-vx/2+hsize+vx/2+size/2,y+vsize+vy/2+size/2);
	    }
	    if (y-vsize > 0) g.drawLine(x-vx/2+size/2,y-vsize+size/2,x+vx/2+size/2,y-vsize+vy/2+size/2);
	    if (y-vy/2+vsize < rvsize) g.drawLine(x-vx/2+size/2,y-vy/2+vsize+size/2,x+vx/2+size/2,y+vsize+vy/2+size/2);
	}

    public static void pintavectordip(Graphics g, int x, int y, int vx, int vy, int hsize, int vsize, int rhsize, int rvsize, int size)
	{
	    g.drawLine(x+size/2,y+size/2,x+vx+size/2,y+vy+size/2);
	    //	    g.fillOval( x, y, size, size );
	    if (x-hsize > 0) {
		g.drawLine(x-hsize+size/2,y+size/2,x-hsize+vx+size/2,y+vy+size/2);
		if (y-vsize > 0)g.drawLine(x-hsize+size/2,y-vsize+size/2,x-hsize+vx+size/2,y-vsize+vy+size/2);
		if (y+vsize < rvsize) g.drawLine(x-hsize+size/2,y+vsize+size/2,x-hsize+vx+size/2,y+vsize+vy+size/2);
	    }
	    if (x+hsize < rhsize) {
		g.drawLine(x+hsize+size/2,y+size/2,x+hsize+vx+size/2,y+vy+size/2);
		if (y-vsize > 0) g.drawLine(x+hsize+size/2,y-vsize+size/2,x+hsize+vx+size/2,y-vsize+vy+size/2);
		if (y+vsize < rvsize) g.drawLine(x+hsize+size/2,y+vsize+size/2,x+hsize+vx+size/2,y+vsize+vy+size/2);
	    }
	    if (y-vsize > 0) g.drawLine(x+size/2,y-vsize+size/2,x+vx+size/2,y-vsize+vy+size/2);
	    if (y+vsize < rvsize) g.drawLine(x+size/2,y+vsize+size/2,x+vx+size/2,y+vsize+vy+size/2);
	}
	


  public static Hashtable makeLabels(int min, int max, int incr, int ist) 
  { 
     String[] alpha = { "0.0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", 
			"0.9", "1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", 
			"1.9", "2.0", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8", 
			"2.9", "3.0", "3.1", "3.2", "3.3", "3.4", "3.5", "3.6", "3.7", "3.8", 
			"3.9", "4.0"};
     Hashtable<Integer,JLabel> hshLabels = new Hashtable<Integer,JLabel>(); 
     int val = min; 
     int i = 0; 
   
     while (val <= max) 
     { 
        hshLabels.put(new Integer(val), new JLabel(alpha[i]) ); 
        val += incr; 
        i = i + ist; 
        if (i > 100 ) i = 0; 
     } 
   
     return hshLabels; 
  } 

    public static void setRW1(double raai ) {
	
	rw1 = raai;
    }

    public static void setRW2(double raai ) {
	
	rw2 = raai;
    }

    public static void setRW3(double raai ) {
	
	rw3 = raai;
    }

    public static void setEps(double epsi ) {
	
	epsw = epsi;
    }



    public static void setCutoffaa(double raai ) {
	
	rcaa = raai;
    }

    public static void setCutoffab(double rabi ) {
	
	rcab = rabi;
    }
     public static void setElon(double eln) {
	 elon = eln;
	 icstate = 1;
     }
    public static void setNparta(double xma) {
	xmola = xma ;
	if (itype == 3) {
	    if (xmola+xmolb > 1.0) {
		xmolb = 1.0 - xmola ;
		DiMol2D.compw.setSliderB((int)(xmolb*100.0));
		natcm[3] = NMOL ;
	    }
	    xmolc = 1 - xmola - xmolc;
	}
        natcm[1] = (int)(xma*(double)NMOL) ;
	if (natcm[1] < idum) natcm[1] = idum;
	if (itype == 3) {
	    if (natcm[1] >= NMOL-1) natcm[1] = NMOL-2;
	    if ( idum > 1) {
		if ( natcm[1]%idum != 0) natcm[1] = idum*( natcm[1]/idum);
	    }
	    natcm[2] = natcm[1] +(int)(xmolb*(double)NMOL); 
	    if (natcm[2] == natcm[1]) natcm[2] = natcm[1]+1;
	    if (natcm[2] == NMOL)  natcm[2] = NMOL-1;  
	    //	    System.out.println(" xma="+xma+" xmb="+xmolb+" naa ="+natcm[1]+" nab "+natcm[2]+" nac="+natcm[3]+" \n");
	}
	if (itype == 2) {
	    xmolb = 1.0 - xmola;
	    natcm[2] = NMOL ;
	    if (natcm[1] < idum) natcm[1]=idum;
	    if (natcm[1] == NMOL) natcm[1]=NMOL-idum;
	    if ( idum > 1) {
		if ( natcm[1]%idum != 0) natcm[1] = idum*( natcm[1]/idum);
	    }
	    //	    System.out.println(" xma="+xma+" naa ="+natcm[1]+" nab "+natcm[2]+" \n");
	}
	icstate = 1;
    }
    public static void setNpartb(double xmb) {
	xmolb = xmb ;
	if (xmola+xmolb > 1.0) {
	    xmola = 1-xmolb ;
	    DiMol2D.compw.setSliderA((int)(xmola*100.0));
	    natcm[1] = (int)(xmola*(double)NMOL) ;
	}
	if (natcm[1] < idum) natcm[1] = idum;
	if (natcm[1] >= NMOL-1) natcm[1] = NMOL-2;
	if ( idum > 1) {
	    if ( natcm[1]%idum != 0) natcm[1] = idum*( natcm[1]/idum);
	}
	natcm[2] = natcm[1] +(int)(xmb*(double)NMOL); 
	if (natcm[2] == natcm[1]) natcm[2] = natcm[1]+1;
	if (natcm[2] >= NMOL)  natcm[2] = NMOL-1;
	natcm[3] = NMOL ;
	//System.out.println(" xma="+xmola+" xmb="+xmb+" naa ="+natcm[1]+" nab "+natcm[2]+" nac="+natcm[3]+" \n");
	xmolc = 1 - xmola - xmolb ;
	icstate = 1;
    }


    public  void paraPrograma() {
	molThread = null ;
	ipause = 1;
    }
    public void resetPopup() 
    {
	if (popupMenu.isVisible()) {
	    popupMenu.setVisible(false);
	    setMenuItems();
	    popupMenu.show(Simulation.this,xmloc,ymloc);
	}
    }
    public void setMenuItems()
    {
	if (istart == 1) {
	    button.setSelected(true);
	    items[1].setEnabled(true);
	    items[1].setText(colors[1]);
	    items[0].setDisabledIcon(createImageIcon("rbs.gif",""));
	    items[0].setText(inicios);
	    items[0].setEnabled(false);
	    items[2].setDisabledIcon(createImageIcon("rbp.gif",""));
	    items[2].setEnabled(false);
	}
	
	if (ipause == 1) {
	    items[0].setEnabled(true);
	    items[0].setText(colors[0]);
	    items[1].setEnabled(false);
	    items[1].setText(parado);
	    if (icrho == 0 && icstate == 0 ) {
		items[2].setEnabled(true);
	    }
	    else {
		items[2].setEnabled(false);
	    }
	}
    }

    public class PopMenu{
	private ButtonGroup colorGroup;
	private final Color colorValues[] = 
	{ Color.BLUE, Color.YELLOW, Color.RED };
	PopMenu () {
	    
	    // set up popup menu and its items
	    colors[0] = DiMol2D.messages.getString("inicio");
	    colors[1] = DiMol2D.messages.getString("pausa");
	    colors[2] = DiMol2D.messages.getString("sigue");
	    colors[3] = DiMol2D.messages.getString("fin");
	    inicios = DiMol2D.messages.getString("inicios");
	    parado = DiMol2D.messages.getString("parado");
	    colorGroup = new ButtonGroup();
	    popupMenu = new JPopupMenu();
	    items = new JRadioButton[ 4 ];
	    ItemHandler handler = new ItemHandler();
	    // construct each menu item and add to popup menu; also
	    // enable event handling for each menu item
	    JMenuItem titulo = new JMenuItem( DiMol2D.messages.getString("control"));
	    titulo.setBackground(new Color(0,136,185));
	    titulo.setForeground(new Color(251,238,0));
	    popupMenu.add(titulo);
	    //
	    // Empty radio button to allow for deselection of all buttons
	    //
	    button = new JRadioButton();
	    colorGroup.add(button);
	    String descripcion = "button group";
	    popupMenu.addSeparator();
	    for ( int count = 0; count < items.length; count++ ) {
		items[ count ] = new JRadioButton(colors[count]);
		items[count].setIcon(createImageIcon("rb.gif", descripcion)); 
		items[count].setPressedIcon(createImageIcon("rbp.gif",descripcion));
		items[count].setRolloverIcon(createImageIcon("rbr.gif",descripcion));
		items[count].setSelectedIcon(createImageIcon("rbs.gif",descripcion));
		items[count].setRolloverSelectedIcon(createImageIcon("rbrs.gif",descripcion));
		items[count].setDisabledIcon(createImageIcon("rbp.gif",descripcion));
		//		items[count].setBackground(colorM);
		popupMenu.add( items[ count ] );
		colorGroup.add( items[ count ] );
		items[ count ].addActionListener(handler );
	    }
	    items[0].setEnabled(true);
	    items[1].setEnabled(false);
	    items[2].setEnabled(false);
	    items[3].setEnabled(true);
	    addMouseListener(new MouseAdapter() {  // anonymous inner class
		    // handle mouse press event
		    public void mousePressed( MouseEvent event )
		    { 
			checkForTriggerEvent( event ); 
		    } 
		    // handle mouse release event
		    public void mouseReleased( MouseEvent event )
		    { 
			checkForTriggerEvent( event ); 
		    } 
		    // determine whether event should trigger popup menu
		    private void checkForTriggerEvent( MouseEvent event )
		    {
			if ( event.isPopupTrigger() ){
			    setMenuItems();
			    xmloc = event.getX();
			    ymloc = event.getY();
			    popupMenu.show(event.getComponent(), xmloc, ymloc );  
			}
		    }
		    
		} // end anonymous inner clas
			 
			     ); // end call to addMouseListener
	
	}
   // private inner class to handle menu item events
	private class ItemHandler implements ActionListener {
	    
	    // process menu item selections
	    public void actionPerformed( ActionEvent event )
	    {
		// determine which menu item was selected
		if ( event.getActionCommand() ==  DiMol2D.messages.getString("inicio") ){
		    if (istart == 0 || ipause == 1 ) {
			if (icn == 1) {
			    icn = 0;
			    //
			    // Properly reset particle numbers and mole fractions 
			    NMOL = NMOLN;
			    if (itype == 1) natcm[1] = NMOL ;
			    if (itype > 1)  setNparta(xmola) ;
			    if (itype == 3)  setNpartb(xmolb) ;
			}
			if ( itset != 0 && irhoset != 0 ) {
			    DiMol2D.natoms.setEnabled(false);
			    DiMol2D.print.setEnabled(false);
			    DiMol2D.rbWall.setEnabled(false);
			    DiMol2D.rbWally.setEnabled(false);
			    DiMol2D.cbDumb.setEnabled(false);
			    DiMol2D.elnb.setEnabled(false);
			    DiMol2D.cbHet.setEnabled(false);
			    DiMol2D.rbGrav.setEnabled(false);
			    DiMol2D.inter.setEnabled(false);
			    DiMol2D.compos.setEnabled(false);
			    DiMol2D.rbMenuItem.setEnabled(false);
			    DiMol2D.rbMenuItem1.setEnabled(false);
			    DiMol2D.rbMenuItem2.setEnabled(false);
			    DiMol2D.rhoSlider.setEnabled(false);
			    if (DiMol2D.icompos == 1) DiMol2D.compw.disiSlider();
			    if (DiMol2D.inpp == 1) DiMol2D.npp.disnpslider();
			    if (DiMol2D.iinter == 1) DiMol2D.app2.disiSlider();
			    if (DiMol2D.iinterw == 1) DiMol2D.app2w.disiSlider();
			    if (DiMol2D.ielon == 1) DiMol2D.elonw.disiSlider();
			    inic = 0;
			    ib = 0;
			    rstate[istate] = densidad;
			    tstate[istate] = temperatura;
			    istate = istate + 1;
			    if (istate > 0 && ippd != 0) DiMol2D.ppd.repintaPD();
			    rsize = Math.sqrt(densidad/(double)NMOL);
			    SIZE = (int)((double)HSIZE*rsize+0.501);
			    rimin = rmin*rsize*rmin*rsize;
			    rimax = rmax*rsize*rmax*rsize;
			    ngrm = Math.min((int)(1.0/(2.0*rsize*drg)+0.51),NGRMAX-1);
			    ngrmy = Math.min((int)(1.0/(rsize*drg)+0.51),NGRMAXY-1);
			    drgx = (1.0/rsize)/(double)ngrm;
			    drgy = (1.0/rsize)/(double)ngrm;
			}
		    }
		    inicp = 1;
		    startProgram();
		    resetPopup();
		}
		else if  ( event.getActionCommand() == DiMol2D.messages.getString("pausa") ){
		    
		    if ( ipause == 1 ) {
			JOptionPane.showMessageDialog(Simulation.this,DiMol2D.messages.getString("progstopped"));
			button.setSelected(true);
			items[1].setSelected(true);
		    }
		    else if ( istart == 0 ) {
			JOptionPane.showMessageDialog(Simulation.this,DiMol2D.messages.getString("prognotinit"));
			button.setSelected(true);
		    }
		    else {
			ipause = 1;
			molThread = null ;
			idum_old = idum;
			itype_old = itype;
			natcm_old[0] = natcm[0];
			natcm_old[1] = natcm[1];
			natcm_old[2] = natcm[2];
			natcm_old[3] = natcm[3];
			DiMol2D.rhoSlider.setEnabled(true);
			DiMol2D.natoms.setEnabled(true);
			DiMol2D.print.setEnabled(true);
			DiMol2D.rbWally.setEnabled(true);
			DiMol2D.rbGrav.setEnabled(true);
			if (idum == 2) 	DiMol2D.elnb.setEnabled(true);
			DiMol2D.rbWall.setEnabled(true);
			DiMol2D.cbDumb.setEnabled(true);
			DiMol2D.cbHet.setEnabled(true);
			DiMol2D.inter.setEnabled(true);
			if (itype > 1) DiMol2D.compos.setEnabled(true);
			DiMol2D.rbMenuItem.setEnabled(true);
			DiMol2D.rbMenuItem1.setEnabled(true);
			DiMol2D.rbMenuItem2.setEnabled(true);
			if (DiMol2D.icompos == 1) DiMol2D.compw.eniSlider();
			if (DiMol2D.iinter == 1) DiMol2D.app2.eniSlider();
			if (DiMol2D.iinterw == 1) DiMol2D.app2w.eniSlider();
			if (DiMol2D.inpp == 1) DiMol2D.npp.ennpslider();
			if (DiMol2D.ielon == 1) DiMol2D.elonw.eniSlider();

			densidad_o = densidad;
			Interaccion.setPause(ipause);
			resetPopup();
		    }
		}
	    
		else if  ( event.getActionCommand() == DiMol2D.messages.getString("sigue") ){
		    if ( istart == 0 ) {
			JOptionPane.showMessageDialog(Simulation.this,DiMol2D.messages.getString("prognotinit"));
		    }
		    else {
		    DiMol2D.natoms.setEnabled(false);
		    DiMol2D.print.setEnabled(false);
		    DiMol2D.rbWall.setEnabled(false);
		    DiMol2D.rbGrav.setEnabled(false);
		    DiMol2D.rbWally.setEnabled(false);
		    DiMol2D.cbDumb.setEnabled(false);
		    DiMol2D.elnb.setEnabled(false);
		    DiMol2D.cbHet.setEnabled(false);
		    DiMol2D.inter.setEnabled(false);
		    DiMol2D.rbMenuItem.setEnabled(false);
		    DiMol2D.rbMenuItem1.setEnabled(false);
		    DiMol2D.rbMenuItem2.setEnabled(false);
		    DiMol2D.rhoSlider.setEnabled(false);
		    DiMol2D.compos.setEnabled(false);
		    if (DiMol2D.iinter == 1) DiMol2D.app2.disiSlider();
		    if (DiMol2D.iinterw == 1) DiMol2D.app2w.disiSlider();
		    if (DiMol2D.icompos == 1) DiMol2D.compw.disiSlider();
		    if (DiMol2D.inpp == 1) DiMol2D.npp.disnpslider();
		    if (DiMol2D.ielon == 1) DiMol2D.elonw.disiSlider();
		    densidad = densidad_o;
		    IRHOINIT = (int)(100.0*densidad+0.501);
		    DiMol2D.rhoSlider.setValue(IRHOINIT);
		    DiMol2D.prhoT.setRT(temperatura,(double)irho/100.0);
		    if (icn == 1) {
			DiMol2D.npp.setnpslider(NMOL);
			icn = 0;
		    }
		    if (inicp == 0 ) {
			inic = 0;
		    }
		    else {
			inic = 1;
		    }
		    inicp = 1;
		    startProgram();
		    resetPopup();
		    }
		}
		else if  ( event.getActionCommand() == DiMol2D.messages.getString("fin") ){
		    molThread = null;
		    int ipauseold = ipause ;
		    ipause = 1;
		    int ans = JOptionPane.showConfirmDialog(Simulation.this,DiMol2D.messages.getString("confterm"),DiMol2D.messages.getString("saliendo"),JOptionPane.YES_NO_OPTION);
		    if ( ans == 0 ) {

			System.exit(0);
		    }
		    else {
			button.setSelected(true);
		    }
			if (istart != 0) {
			if ( ipauseold == 0 )startProgram();
			ipause = ipauseold;
		    }
		}
	    }
	    
	    
	} // end private inner class ItemHandler
    }


    static {
     System.loadLibrary("Simulation");
    }


}



