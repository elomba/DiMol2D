import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.net.*;

public class Identf extends JFrame
{
    private Container cont; 
    private PlotST miPanel;
    private int HS=420, VS=200;
    public static Color backg, foreg1;
    Identf (int dimX, Image dm2ds){
	super( DiMol2D.messages.getString("acerca"));
	JLabel icon;
	backg = new Color(0,136,185);
	cont = getContentPane();
	URL  url = getClass().getResource("Icon/csic_trans.png");
	Icon csic = new ImageIcon(url);
	url = getClass().getResource("Icon/qft_t100.png");
	Icon qft = new ImageIcon(url);
	icon = new JLabel();
	icon.setIcon( csic);
	icon.setHorizontalTextPosition( SwingConstants.CENTER );
	icon.setVerticalTextPosition( SwingConstants.BOTTOM );
	JLabel qfticon = new JLabel();
	qfticon.setIcon(qft);
	qfticon.setHorizontalTextPosition( SwingConstants.CENTER );
	qfticon.setVerticalTextPosition( SwingConstants.BOTTOM );

	cont.setLayout( new BorderLayout(1,1));
	miPanel = new PlotST(DiMol2D.messages.getString("design"),dm2ds,HS,VS);
	//	cont.add(nombres,BorderLayout.CENTER);
	cont.add(miPanel,BorderLayout.CENTER);
	cont.add(icon,BorderLayout.WEST);
	cont.add(qfticon,BorderLayout.EAST);
	//	cont.setBackground(new Color(5,114,182)); 
	cont.setBackground(backg); 
	setSize(HS+150,VS);
	setVisible(true);
    }
    public void repintanos(  )
    {
	cont.repaint();
    }

}
class PlotST extends JPanel {
    private String text;
    private int H, V,yor;
    private Image dm2d;
    PlotST(String texto, Image DM, int hs, int vs){
	text = texto;
	H = hs;
	V = vs;
	dm2d = DM;
	yor = V/3+10;
    }
    public void paintComponent( Graphics g ) {
	super.paintComponent( g );
	Graphics2D g2d = (Graphics2D)g;
	g2d.setColor(Identf.backg);
	g2d.fillRect(0,0,H,V);
	g2d.setColor(new Color(234,255,0));
	g2d.drawImage(dm2d,H/5,-35,this);
	g2d.setFont( new Font("SansSerif", Font.BOLD,14));	
	g2d.drawString(text+":",6,yor);
	g2d.drawString("E. Lomba, N.G. Almarza, C Martin. ",6,yor+20);
	g2d.setFont( new Font("SansSerif", Font.BOLD,10));	
	g2d.drawString("IQFR, CSIC, Madrid (2004-2008).",100,yor+60);
	g2d.drawString(DiMol2D.messages.getString("bug"),100,yor+75);
	g2d.drawString("http://www.qft.iqfr.csic.es/DiMol2D",100,yor+90);
    }
   public Dimension getPreferredSize() {
       return new Dimension( H, V );
   }


   // used by layout manager to determine minimum size
   public Dimension getMinimumSize() {
       return getPreferredSize();
   }
   public Dimension getMaximumSize() {
       return getPreferredSize();
   }
}

