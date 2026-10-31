import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.text.DecimalFormat;
import java.net.*;
//
// DiMol2D is a program that visualizes a running Molecular Dinamics simulation
// in 2D, calculates correlation functions, allows for walls and gravity to be
// added and some other extras. The simulation is run in a native Fortan 90 
// code which transfers data through the C routine jsimul usin Java Native 
// Interface (JNI). 
//           Designed by E.Lomba, N.G. Almarza and C. Martin.
//           IQFR CSIC, Madrid 2004-2004
//           
//           Version 0.9  09.2004
//           Verson  1.0  10.2005 
//           Version 1.1  05.2008
//
//
public class DiMol2D  implements CommonVars
{
    static JCheckBoxMenuItem rbMol, rbBon, rbWall, rbWally, rbGrav, cbDumb, cbHet;
    static JMenuBar menuBar;
    static JMenu menu, submenu;
    public static JRadioButtonMenuItem rbMenuItem, rbMenuItem1, rbMenuItem2,rbMenuItem3,rbMenuItem4;
    public static JMenuItem  disten, natoms, veloc, print, pgr, pdpx, pdpy, phd, interat, interw, compos, elnb;
    public static JMenu inter;
    private  ItemHandler itemHandler; 
    public static GrFrame grf;
    public static Color cmenu;
    public static PdxFrame pdfx;
    public static PdyFrame pdfy;
    public static PhD ppd;
    MetalworksHelp aps;   
    private  GridBagLayout mdLayout;
    private GridBagConstraints mdConstr;
    public static Point pos;
    public static SetNpart npp;
    public static SetVeloc nveloc;
    public static Interaccion app2;
    public static InteraccionW app2w;
    public static Elongacion elonw;
    public static Composicion compw;
    public static Identf ap1;
    public static int iinter=0, iinterw=0, inpp=0, invel=0, icompos=0, ielon=0;
    DecimalFormat dosDigitos;
    public static JLabel gLabel,  trLabel, rLabel;
    private static int ITMPMIN=0, ITMPMAX=200,ITMPINI=0, ITMPSP=50;
    public static JSlider tempSlider, rhoSlider;
    private double riimin, riimax,rmin,rmax;
    public static Container todo;
    public static Manometer manoM ;
    public static PintaRT prhoT;
    public Simulation mdSim;
    private Icon qft, trho, iconT, iconRho;
    public static String language;
    public static String country;
    public static Image dm2ds;
    Locale currentLocale;
    public static ResourceBundle messages;
    public DiMol2D () {
	//
	// Define Locale to set the proper language for the messages
	//
	cmenu = new Color(247,255,212);
	currentLocale = Locale.getDefault();
	messages =
	    ResourceBundle.getBundle("MessagesBundle",currentLocale);
	//
	// Load images into memory
	//
	URL url = getClass().getResource("Icon/qft_t100.png");
	qft = new ImageIcon(url);
	url = getClass().getResource("Image/DM2D1-1s.png");
	dm2ds = Toolkit.getDefaultToolkit().getImage(url);
	url = getClass().getResource("Icon/T.png");
	iconT = new ImageIcon(url);
	url = getClass().getResource("Icon/Rho.png");
	iconRho = new ImageIcon(url);
	gLabel = new JLabel();
	gLabel.setIcon(qft);
	trLabel = new JLabel();
	trLabel.setIcon(iconT);
	rLabel = new JLabel();
	rLabel.setIcon(iconRho);
	//
	// Main window
	//
	JFrame f = new JFrame(messages.getString("titulo"));
	f.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
	//
	// Get screee size
	//
	Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	//
	// Initialize point for window location and format for number output
	//
	pos = new Point(0,0);
	dosDigitos = new DecimalFormat("0.00");
	//
	// Initialize simulation window and set dimensions
	//
        mdSim = new Simulation();
	mdSim.dimY = dim.height;
	mdSim.dimX = dim.width;
	mdSim.RHSIZE=(85*dim.height)/100 ;
	mdSim.RVSIZE=mdSim.RHSIZE;
	mdSim.HSIZE = (80*mdSim.RHSIZE)/100;
	mdSim.VSIZE = mdSim.HSIZE;
	manoM = new Manometer(0.0,15,(int)(mdSim.RVSIZE/2.9));
	prhoT = new PintaRT(0.0,0.0,120,60);
	//
	//  Construct main menu
	//
	menuBar = new JMenuBar();
	//
	// Build simulation type menu
	//
	menu = new JMenu(messages.getString("tipo"));
	menu.setBackground(new Color(0,136,185));
	menu.setForeground(new Color(251,238,0));
	menuBar.setBackground(new Color(0,136,185));
	menuBar.setForeground(new Color(237,225,0));
	menuBar.add(menu);
	menu.addSeparator();
	ButtonGroup group = new ButtonGroup();
	rbMenuItem = new JRadioButtonMenuItem(messages.getString("sustanciapura"));
	rbMenuItem.setBackground(cmenu);
	rbMenuItem.setSelected(true);
	group.add(rbMenuItem);
	menu.add(rbMenuItem);
	rbMenuItem1 = new JRadioButtonMenuItem(messages.getString("mezclabinaria"));
	rbMenuItem1.setBackground(cmenu);
	group.add(rbMenuItem1);
	menu.add(rbMenuItem1);
	rbMenuItem2 = new JRadioButtonMenuItem(messages.getString("mezclaternaria"));
	rbMenuItem2.setBackground(cmenu);
	group.add(rbMenuItem2);
	menu.add(rbMenuItem2);
        cbDumb =  new JCheckBoxMenuItem(messages.getString("dumbell"));
	cbDumb.setBackground(cmenu);
	menu.add(cbDumb);
        cbHet =  new JCheckBoxMenuItem(messages.getString("het"));
	cbHet.setBackground(cmenu);
	menu.add(cbHet);
	menu.addSeparator();
	//
	// Build System Control & Properties menu
	//
	rbWall = new JCheckBoxMenuItem(messages.getString("paredeslaterales"));
	rbWall.setBackground(cmenu);
	rbWally = new JCheckBoxMenuItem(messages.getString("paredeshorizontales"));
	rbWally.setBackground(cmenu);
	rbGrav = new JCheckBoxMenuItem(messages.getString("gravitacion"));
	rbGrav.setBackground(cmenu);
	JMenu menu1 = new JMenu(messages.getString("sistema"));
	inter = new JMenu(messages.getString("interacciones"));
	inter.setBackground(cmenu);
	interat = new JMenuItem(messages.getString("atomoatomo"));
	interat.setBackground(cmenu);
	interw = new JMenuItem(messages.getString("atomopared"));
	interw.setBackground(cmenu);
        compos = new JMenuItem(messages.getString("composition"));
	compos.setBackground(cmenu);
        elnb = new JMenuItem(messages.getString("elong"));
	elnb.setBackground(cmenu);

	rbMol = new JCheckBoxMenuItem(messages.getString("pintamoleculas"));
	rbMol.setBackground(cmenu);
	rbBon = new JCheckBoxMenuItem(messages.getString("pintaenlaces"));
	rbBon.setBackground(cmenu);
	disten = new JMenuItem(messages.getString("distanciaenlace"));
	disten.setBackground(cmenu);
	natoms = new JMenuItem(messages.getString("numeroparticulas"));
	natoms.setBackground(cmenu);
	veloc = new JMenuItem(messages.getString("sincro"));
	veloc.setBackground(cmenu);
	print = new JMenuItem(messages.getString("imprimir"));
	print.setBackground(cmenu);
	pgr = new JMenuItem(messages.getString("gr"));
	pgr.setBackground(cmenu);
	pdpx = new JMenuItem(messages.getString("perfdenshor"));
	pdpx.setBackground(cmenu);
	pdpy = new JMenuItem(messages.getString("perfdensver"));
	pdpy.setBackground(cmenu);
	phd = new JMenuItem(messages.getString("diagramafases"));
	phd.setBackground(cmenu);
	menu1.add(inter);
	inter.add(interat);
	inter.add(interw);
        menu1.add(compos);
        menu1.add(elnb);
	DiMol2D.compos.setEnabled(false);
	DiMol2D.elnb.setEnabled(false);
	menu1.add(disten);
	menu1.add(natoms);
	menu1.add(veloc);
	menu1.add(print);
	menu1.addSeparator();
	menu1.add(pgr);
	menu1.add(pdpx);
	menu1.add(pdpy);
	menu1.add(phd);
	menu1.addSeparator();
	menu1.add(rbMol);
	rbMol.setSelected(true);
	menu1.add(rbBon);
	menu1.setBackground(new Color(0,136,185));
	menu1.setForeground(new Color(251,238,0));
	menuBar.add(menu1);
	interw.setEnabled(false);
	pdpx.setEnabled(false);
	pdpy.setEnabled(false);
	//
	// Build Help Menu
	//
	JMenu menu2 = new JMenu(messages.getString("ayuda"));
	JMenuItem help = new JMenuItem(messages.getString("ayudabasica"));
	help.setBackground(cmenu);
	JMenuItem ident = new JMenuItem(messages.getString("acerca"));
	ident.setBackground(cmenu);
	menu2.add(help);
	menu2.addSeparator();
	menu2.add(ident);
	
	menu2.setBackground(new Color(0,136,185));
	menu2.setForeground(new Color(251,238,0));
	menuBar.add(Box.createHorizontalGlue());
	menuBar.add(menu2);
	//
	// Add item handlers to Menu items
	//
	itemHandler  = new ItemHandler();
	rbMenuItem.addActionListener(itemHandler);
	rbMenuItem1.addActionListener(itemHandler);
	rbMenuItem2.addActionListener(itemHandler);
	help.addActionListener(itemHandler);
	ident.addActionListener(itemHandler);
	interat.addActionListener(itemHandler);
	interw.addActionListener(itemHandler);
	compos.addActionListener(itemHandler);
	elnb.addActionListener(itemHandler);
	disten.addActionListener(itemHandler);
	natoms.addActionListener(itemHandler);
	veloc.addActionListener(itemHandler);
	pgr.addActionListener(itemHandler);
	pdpx.addActionListener(itemHandler);
	pdpy.addActionListener(itemHandler);
	phd.addActionListener(itemHandler);
	print.addActionListener(itemHandler);
	rbMol.addActionListener(itemHandler);
	rbBon.addActionListener(itemHandler);
	rbWall.addActionListener(itemHandler);
	cbDumb.addActionListener(itemHandler);
	cbHet.addActionListener(itemHandler);
	rbWally.addActionListener(itemHandler);
	rbGrav.addActionListener(itemHandler);
	menu.add(rbWall);
	menu.add(rbWally);
	menu.add(rbGrav);

	//
	// End of menu construction
	//

	// 
	// Construct T and rho sliders
	//
	 tempSlider = 
		new JSlider( SwingConstants.VERTICAL, ITMPMIN, ITMPMAX, ITMPINI );
	 tempSlider.setMajorTickSpacing( ITMPSP );
	 tempSlider.setMinorTickSpacing( ITMPSP/2 );
	 tempSlider.setPaintTicks( true );
	 tempSlider.setBackground(colorFnd);
	 tempSlider.setForeground(new Color(1,1,1));
	 tempSlider.setLabelTable(mdSim.makeLabels(ITMPMIN,ITMPMAX,ITMPSP,ITMPSP/10));
	 tempSlider.setPaintLabels( true );
	 mdSim.temperatura = 0.0;
	 prhoT.setRT(mdSim.temperatura,mdSim.densidad);
      tempSlider.addChangeListener(new ChangeListener() {public void stateChanged( ChangeEvent e )
	      {
		  if (!tempSlider.getValueIsAdjusting()) {
		      ITMPINI = tempSlider.getValue();
		      mdSim.temperatura = (double)ITMPINI/100.0;
		      mdSim.tstate[mdSim.istate] = mdSim.temperatura;
		      mdSim.rstate[mdSim.istate] = mdSim.densidad;
		      if (mdSim.istate != 0 && mdSim.ippd != 0 ) {
			  if ( mdSim.istart > 0) {
			      ppd.repintaPD();
			  }
			  else {
			    ppd.ptransient(mdSim.temperatura,mdSim.densidad);  
			  }
		      }
		      mdSim.istate = mdSim.istate + 1;
		      prhoT.setRT(mdSim.temperatura,mdSim.densidad);
		      mdSim.itset = 1;
		  }
		  else
		      if (mdSim.istate > 0 && mdSim.ippd != 0 ) {
			  double ttemp = (double)(tempSlider.getValue())/100.0;
			  DiMol2D.ppd.ptransient(ttemp,mdSim.densidad);
		      }
	      }
         } // end anonymous inner class
				  ); // end call to addChangeListener

      rhoSlider = 
	  new JSlider( SwingConstants.VERTICAL, 0, 100, mdSim.IRHOINIT );
      rhoSlider.setBackground(colorFnd);
      rhoSlider.setForeground(new Color(1,1,1));
      rhoSlider.setMajorTickSpacing( 20 );
      rhoSlider.setMinorTickSpacing( 10 );
      rhoSlider.setPaintTicks( true );
      rhoSlider.setLabelTable(mdSim.makeLabels(0,100,20,2));
      rhoSlider.setPaintLabels( true );
      rhoSlider.addChangeListener(new ChangeListener() {public void stateChanged( ChangeEvent e )
	      {
		  if ( mdSim.ipause != 1 && mdSim.istart != 0 ) {
				mdSim.IRHOINIT = (int)(100.0*mdSim.densidad);
				rhoSlider.setValue(mdSim.IRHOINIT);
		  }
		  else {
		      mdSim.icrho = 0;
		      if (!rhoSlider.getValueIsAdjusting()) {
			  mdSim.densidad_o = mdSim.densidad;
			  mdSim.irho = rhoSlider.getValue();
			  if (mdSim.irho < 1 ) mdSim.irho = 1;
			  mdSim.densidad = mdSim.irho/100.0;
			  prhoT.setRT(mdSim.temperatura,(double)mdSim.irho/100.0);

			  mdSim.irhoset = 1;
			  mdSim.icrho = 1;
		      }
 else
		      if (mdSim.istate > 0 && mdSim.ippd != 0 ) {
			  double trho = (double)(rhoSlider.getValue())/100.0;
			  DiMol2D.ppd.ptransient(mdSim.temperatura,trho);
		      }
		  }
	      }
         } // end anonymous inner class
      ); // end call to addChangeListener
      //
      // Main window layout
      //

	todo = f.getContentPane();
	mdLayout =  new GridBagLayout();
	mdConstr = new GridBagConstraints();
	//	todo.setBackground(new Color(147,185,179));
	todo.setBackground(colorFnd);
	todo.setLayout( mdLayout );
        mdConstr.anchor=GridBagConstraints.CENTER;
	mdConstr.fill = GridBagConstraints.BOTH;
	mdConstr.weightx= 1000;
	mdConstr.weighty= 20000;
	addComponent(mdSim,1,0,1,6);
	mdConstr.weightx= 1;
	mdConstr.weighty= 1000;
	addComponent(menuBar,0,0,3,1);
	mdConstr.weightx= 0;
	mdConstr.weighty= 10;
        mdConstr.anchor=GridBagConstraints.NORTH;
	mdConstr.fill = GridBagConstraints.BOTH;
	addComponent(gLabel,1,1,2,1);
	mdConstr.weightx= 0;
	mdConstr.weighty= 1;
	mdConstr.fill = GridBagConstraints.VERTICAL;

	addComponent(trLabel,2,1,1,1);
	addComponent(rLabel,2,2,1,1);
	mdConstr.fill = GridBagConstraints.NONE;
	mdConstr.weightx= 0;
	mdConstr.weighty= 0;
	int trSize = (int)(mdSim.RVSIZE/3.8);
	if (mdSim.RVSIZE > 700 ) trSize = (int)(mdSim.RVSIZE/3.4);
	if (mdSim.RVSIZE > 740 ) trSize = (int)(mdSim.RVSIZE/2.9);
	tempSlider.setPreferredSize(new Dimension (60, trSize));
	rhoSlider.setPreferredSize(new Dimension (60, trSize));
	tempSlider.setMinimumSize(new Dimension (60, trSize));
	rhoSlider.setMinimumSize(new Dimension (60, trSize));
	tempSlider.setMaximumSize(new Dimension (60, trSize));
	rhoSlider.setMaximumSize(new Dimension (60, trSize));
	addComponent(tempSlider,3,1,1,1);
	addComponent(rhoSlider,3,2,1,1);
	mdConstr.weightx= 0;
	mdConstr.weighty= 0;
 	mdConstr.anchor=GridBagConstraints.WEST;
 	mdConstr.fill = GridBagConstraints.NONE;
	addComponent(prhoT,5,1,2,1);
	mdConstr.anchor=GridBagConstraints.CENTER;
	mdConstr.fill = GridBagConstraints.BOTH;
	mdConstr.weightx= 10;
	mdConstr.weighty= 200;
	addComponent(manoM,6,1,2,1);
	f.pack();
	f.setResizable(false);
	f.setSize (mdSim.RHSIZE+155, mdSim.RVSIZE+55);
        f.setVisible(true);
    }
    //
    //
    //  Here goes the main class 
    //
    public static void main(String[] args) {
	//
	// Instantiate class DiMol2D 
	//
	DiMol2D app = new DiMol2D();
    }

    //
    // Build the main Item Handler
    //
    private class ItemHandler extends JFrame implements ActionListener {
	public void actionPerformed ( ActionEvent event )
	
	{
	    if (event.getActionCommand() == messages.getString("ayudabasica")){
		    String ayudab = messages.getString("ayudabasica");
		    String filename = messages.getString("fichero");
		    aps = new MetalworksHelp(390,2*mdSim.dimY/3,mdSim.dimX-390,0,ayudab,filename);
		    aps.setResizable(false);
		    aps.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
		    aps.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent evt) {
				aps.dispose();
			    }
			} );
	    }
	    else if (event.getActionCommand() == messages.getString("acerca")){
		if ( mdSim.isobr ==0 ) {
		    ap1 = new Identf(mdSim.dimX,dm2ds);
		    ap1.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
		    mdSim.isobr = 1;
		    ap1.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent evt) {
				mdSim.isobr = 0;
				ap1.dispose();
			    }
			} );
		}
	    }
	    else if (event.getActionCommand() == messages.getString("pintamoleculas")){
		if (rbMol.getState()){
		     mdSim.ipmol = 1;
		}
		else {
		     mdSim.ipmol = 0;
		}

	    }
	    else if (event.getActionCommand() == messages.getString("pintaenlaces")){
		if (rbBon.getState()){
		     mdSim.ipbon = 1;
		}
		else {
		     mdSim.ipbon = 0;
		}
	    }
	    else if (event.getActionCommand() == messages.getString("dumbell")){
		if (cbDumb.getState()){
		    mdSim.idum = 2;
		    DiMol2D.elnb.setEnabled(true);
		     if ( mdSim.itype == 2) {
			 mdSim.natcm[1] = Simulation.NMOL/2;
			 if ( mdSim.natcm[1]%mdSim.idum != 0) mdSim.natcm[1] = mdSim.idum*( mdSim.natcm[1]/mdSim.idum);
		     }
		     else if  ( mdSim.itype == 3) {
			 mdSim.natcm[1] = Simulation.NMOL/3;
			  if ( mdSim.natcm[1]%mdSim.idum != 0) mdSim.natcm[1] = mdSim.idum*( mdSim.natcm[1]/mdSim.idum);
		     }

		}
		else {
		    if (mdSim.iheter == 1) {
			mdSim.idum = 2;
			cbDumb.setState(true);
		    }
		    else {
			mdSim.idum = 1;
			DiMol2D.elnb.setEnabled(false);

		    }

		}
		mdSim.icstate = 1;
		mdSim.inicp = 0;
	    }
	    else if (event.getActionCommand() == messages.getString("het")){
		if (cbHet.getState()){
		    mdSim.idum = 2;
		    mdSim.iheter = 1;
		    DiMol2D.elnb.setEnabled(true);
		    cbDumb.setState(true);
		    cbHet.setState(true);
		     if ( mdSim.itype == 2) {
			 mdSim.natcm[1] = Simulation.NMOL/2;
			 if ( mdSim.natcm[1]%mdSim.idum != 0) mdSim.natcm[1] = mdSim.idum*( mdSim.natcm[1]/mdSim.idum);
		     }
		     else if  ( mdSim.itype == 3) {
			 mdSim.natcm[1] = Simulation.NMOL/3;
			  if ( mdSim.natcm[1]%mdSim.idum != 0) mdSim.natcm[1] = mdSim.idum*( mdSim.natcm[1]/mdSim.idum);
		     }

		}
		else {
		     mdSim.iheter = 0;

		}
		mdSim.icstate = 1;
		mdSim.inicp = 0;
	    }
	    else if (event.getActionCommand() == messages.getString("paredeslaterales")){
		if (rbWall.getState()){
		     mdSim.iwall = 1;
		     DiMol2D.interw.setEnabled(true);
		     DiMol2D.pdpx.setEnabled(true);
		}
		else {
		     mdSim.iwall = 0;
		     DiMol2D.pdpx.setEnabled(false);
		     DiMol2D.interw.setEnabled(false);

		}
		mdSim.icstate = 1;
		mdSim.inicp = 0;
	    }
	    else if (event.getActionCommand() == messages.getString("paredeshorizontales")){
		if (rbWally.getState()){
		    DiMol2D.pdpy.setEnabled(true);
		    mdSim.iwally = 1;
		}
		else {
		    mdSim.iwally = 0;
		    mdSim.igrav = 0;
		    DiMol2D.pdpy.setEnabled(false);
		    rbGrav.setState(false);
		}
		mdSim.icstate = 1;
		mdSim.inicp = 0;
	    }
	    else if (event.getActionCommand() == messages.getString("gravitacion")){
		if (rbGrav.getState()){
		    if (!rbWally.getState()){
			mdSim.iwally = 1;
			DiMol2D.pdpy.setEnabled(true);
			rbWally.setState(true);
		    }
		    mdSim.igrav = 1;
		}
		else {
		    mdSim.igrav = 0;
		}
		mdSim.inicp = 0;
		mdSim.icstate = 1;
	    }


	    else if (event.getActionCommand() == messages.getString("gr")){
		if ( mdSim.ipgr == 0 &&  mdSim.istart != 0 ){
		    grf = new GrFrame(mdSim.ngrm,mdSim.drg);
		    grf.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
		    mdSim.ipgr = 1;
		    grf.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent evt) {
				mdSim.ipgr = 0;
				grf.dispose();
			    }
			} );
		}
	    }
	    else if (event.getActionCommand() == messages.getString("perfdenshor")){
		if ( mdSim.ipdx == 0 &&  mdSim.istart != 0 ){
		    pdfx = new PdxFrame(mdSim.ngrm,mdSim.drgx);
		    pdfx.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
		    mdSim.ipdx = 1;
		    pdfx.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent evt) {
				mdSim.ipdx = 0;
				pdfx.dispose();
			    }
			} );
		}
	    }
	    else if (event.getActionCommand() == messages.getString("perfdensver")){
		if ( mdSim.ipdy == 0 &&  mdSim.istart != 0 ){
		    pdfy = new PdyFrame(mdSim.ngrmy,mdSim.drgy);
		    pdfy.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
		    mdSim.ipdy = 1;
		    pdfy.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent evt) {
				mdSim.ipdy = 0;
				pdfy.dispose();
			    }
			} );
		}
	    }
	    else if (event.getActionCommand() == messages.getString("diagramafases")){
		if (mdSim.ippd == 0 && mdSim.istate > 0 && mdSim.itype != 11 ){
		    createPD(0,242);
		    mdSim.ippd = 1;
		    ppd.ptransient(mdSim.temperatura,mdSim.densidad);
		}
	    }

	    else if (event.getActionCommand() == messages.getString("distanciaenlace")){
		rmax = mdSim.rmax;
		rmin = mdSim.rmin;
		mdSim.paraPrograma();
		Object srmax = JOptionPane.showInputDialog(this,messages.getString("distmax"),Double.toString(rmax));
		Object srmin = JOptionPane.showInputDialog(this,messages.getString("distmin"),Double.toString(rmin));
		mdSim.startProgram();
		if (srmin != null ) {
		    riimin = Double.parseDouble(srmin.toString());
		    if ( riimin < 0.0 ) riimin = 0.0 ;
		    mdSim.rmin = riimin;
		}
		if (srmax != null ) {
		    riimax = Double.parseDouble(srmax.toString());
		    if ( riimax <= mdSim.rmin ) riimax = rmin+0.1 ;
		    mdSim.rmax = riimax;
		}
		mdSim.rimin = mdSim.rmin*mdSim.rsize*mdSim.rmin*mdSim.rsize;
		mdSim.rimax = mdSim.rmax*mdSim.rsize*mdSim.rmax*mdSim.rsize;
	    }




	    else if (event.getActionCommand() == messages.getString("sincro")){
		if ( invel == 0) {
		    nveloc = new SetVeloc();
		    invel = 1;
		    nveloc.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
		    nveloc.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent evt) {
				invel = 0;
				nveloc.dispose();
			    }
			} );
		}
		
	    }
	    
	    else {

		if ( mdSim.istart == 0 || mdSim.ipause == 1) {
		    if (event.getActionCommand() == messages.getString("sustanciapura")){
			if ( mdSim.itype != 1 ) {
			    mdSim.inicp = 0;
			    mdSim.icstate = 1;
			}
			mdSim.itype = 1;
			mdSim.natcm[1] = Simulation.NMOL;
			phd.setEnabled(true);
			DiMol2D.inter.setEnabled(true);
                        DiMol2D.compos.setEnabled(false);
                        DiMol2D.elnb.setEnabled(false);
			rbWall.setEnabled(true);
			rbGrav.setEnabled(true);
			rbWally.setEnabled(true);
                        cbDumb.setEnabled(true);
                        cbHet.setEnabled(true);
			if (mdSim.iinter == 1) {
			    pos = app2.getLocation();
			    app2.dispose();
			    createInteractionWindow(pos);
			}
			if (iinterw == 1) {
			    pos = app2w.getLocation();
			    app2w.dispose();
			    createInteractionWallWindow(pos);
			}
			if (icompos == 1) {
			    pos = compw.getLocation();
			    compw.dispose();
			}
			if (ielon == 1) {
			    pos = elonw.getLocation();
			    elonw.dispose();
			}
		    }
		    else if (event.getActionCommand() == messages.getString("mezclabinaria")){
			if ( mdSim.itype != 2 ) {
			    mdSim.inicp = 0;
			    mdSim.icstate = 1;
			}
			mdSim.itype = 2;
			mdSim.natcm[1] = Simulation.NMOL/2;
			if (mdSim.idum > 1) {
			    if ( mdSim.natcm[1]%mdSim.idum != 0) mdSim.natcm[1] = mdSim.idum*( mdSim.natcm[1]/mdSim.idum);
			}
			mdSim.natcm[2] = Simulation.NMOL;
			mdSim.xmola = 0.5;
			mdSim.xmolb = 0.5;
			DiMol2D.compos.setEnabled(true);
			if (mdSim.idum > 1) DiMol2D.elnb.setEnabled(true);
			rbWall.setEnabled(true);
			rbGrav.setEnabled(true);
			rbWally.setEnabled(true);
			cbDumb.setEnabled(true);
			cbHet.setEnabled(true);
			phd.setEnabled(true);
			DiMol2D.inter.setEnabled(true);
			if (iinter == 1) {
			    pos = app2.getLocation();
			    app2.dispose();
			    createInteractionWindow(pos);
			}
			if (iinterw == 1) {
			    pos = app2w.getLocation();
			    app2w.dispose();
			    createInteractionWallWindow(pos);
			}

		     	if (icompos == 1) {
			    pos = compw.getLocation();
			    compw.dispose();
			    createCompositionWindow(pos);
			}
		     	if (ielon == 1) {
			    pos = elonw.getLocation();
			    elonw.dispose();
			    createElongationWindow(pos);
			}
		    }
						 
		    else if (event.getActionCommand() == messages.getString("mezclaternaria")){
			if ( mdSim.itype != 3 ) {
			    mdSim.inicp = 0;
			    mdSim.icstate = 1;
			}
			mdSim.itype = 3;
			mdSim.natcm[1] = Simulation.NMOL/3;
			if (mdSim.idum > 1) {
			    if ( mdSim.natcm[1]%mdSim.idum != 0) mdSim.natcm[1] = mdSim.idum*( mdSim.natcm[1]/mdSim.idum);
			}
			mdSim.natcm[2] = 2*Simulation.NMOL/3;
			mdSim.natcm[3] = Simulation.NMOL;
			mdSim.xmola = 0.3333333;
			mdSim.xmolb = 0.3333333;
			mdSim.xmolc = 0.3333333;
			DiMol2D.compos.setEnabled(true);
			if(mdSim.idum > 1)DiMol2D.elnb.setEnabled(true);
			rbWall.setEnabled(true);
			rbWally.setEnabled(true);
			rbGrav.setEnabled(true);
			cbDumb.setEnabled(true);
			cbHet.setEnabled(true);
			DiMol2D.inter.setEnabled(true);
			phd.setEnabled(true);
			if (iinter == 1) {
			    pos = app2.getLocation();
			    app2.dispose();
			    createInteractionWindow(pos);
			}
			if (iinterw == 1) {
			    pos = app2w.getLocation();
			    app2w.dispose();
			    createInteractionWallWindow(pos);
			}
			if (icompos == 1) {
			    pos = compw.getLocation();
			    compw.dispose();
			    createCompositionWindow(pos);
			}
			if (ielon == 1) {
			    pos = elonw.getLocation();
			    elonw.dispose();
			    createElongationWindow(pos);
			}

		    }
		    if (event.getActionCommand() == messages.getString("atomoatomo")){
			if (iinter == 0 ){
			    iinter = 1;
			    createInteractionWindow(pos);
			}
		    }
		    if (event.getActionCommand() == messages.getString("atomopared")){
			if (iinterw == 0 ){
			    iinterw = 1;
			    createInteractionWallWindow(pos);
			}
		    }


		    if (event.getActionCommand() == messages.getString("numeroparticulas")){
			if ( inpp == 0) {
			    npp = new SetNpart();
			    inpp = 1;
			    npp.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
			    npp.addWindowListener(new WindowAdapter() {
				    public void windowClosing(WindowEvent evt) {
					inpp = 0;
					npp.dispose();
				    }
				} );
			}

		    }

		    if (event.getActionCommand() == messages.getString("composition")){
			if ( icompos == 0) {
			    icompos = 1;
			    createCompositionWindow(pos);
			}
		    }

		    if (event.getActionCommand() == messages.getString("elong")){
			if ( ielon == 0) {
			    ielon = 1;
			    createElongationWindow(pos);
			}
		    }


		    if (event.getActionCommand() == messages.getString("imprimir")){
			Print2D nprin = new Print2D();
		    }
		}
		else {
		    JOptionPane.showMessageDialog(this," Para acceder a este menu hay que detener la aplicacion");
		}
	    }
		


	//
	}
    }





    private void addComponent( Component component,
      int row, int column, int width, int height )
	//
	// Function to add components to a gridbag layout
	//
   {
      // set gridx and gridy 
      mdConstr.gridx = column;
      mdConstr.gridy = row;

      // set gridwidth and gridheight
      mdConstr.gridwidth = width;   
      mdConstr.gridheight = height;

      // set constraints and add component
      mdLayout.setConstraints( component, mdConstr );  
      todo.add( component );      
   }

    private  void createInteractionWindow(Point pos) {
	//
	// Create interaction windows
	//
	app2 = new Interaccion(mdSim.rcaa,mdSim.rcab,mdSim.itype,mdSim.ipause,mdSim.istart,pos);
	app2.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
	app2.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent evt) {
		    iinter = 0;
		    DiMol2D.pos = app2.getLocation();
		    app2.dispose();
		}
	    } );
    }

    private  void createCompositionWindow(Point pos) {
	compw = new Composicion(mdSim.natcm,mdSim.itype,mdSim.ipause,mdSim.istart,pos);
	compw.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
	compw.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent evt) {
		    icompos = 0;
		    Simulation.pos = compw.getLocation();
		    compw.dispose();
		}
	    } );
    }
    private  void createElongationWindow(Point pos) {
	elonw = new Elongacion(mdSim.elon,mdSim.ipause,mdSim.istart,pos);
	elonw.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
	elonw.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent evt) {
		    ielon = 0;
		    Simulation.pos = elonw.getLocation();
		    elonw.dispose();
		}
	    } );

    }

    private  void createInteractionWallWindow(Point pos) {
	//
	// Create particle-wall interaction window
	//
	app2w = new InteraccionW(mdSim.rw1,mdSim.rw2,mdSim.rw3,mdSim.epsw,mdSim.itype,mdSim.ipause,mdSim.istart,pos);
	app2w.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
	app2w.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent evt) {
		    iinterw = 0;
		    DiMol2D.pos = app2w.getLocation();
		    app2w.dispose();
		}
	    } );
    }

    public  void createPD(int px, int py) {
	//
	// Create Phase Diagram window
	//
	ppd = new PhD(px,py);
	//	ppd.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
 	ppd.addWindowListener(new WindowAdapter() {
 		public void windowClosing(WindowEvent evt) {
 		    mdSim.ippd = 0;
 		}
 	    } );

    }


}


