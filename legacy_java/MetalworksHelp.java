/*
 * Copyright (c) 2002 Sun Microsystems, Inc. All  Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * -Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduct the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT
 * BE LIABLE FOR ANY DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT
 * OF OR RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN
 * IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that Software is not designed, licensed or intended for
 * use in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

/*
 * @(#)MetalworksHelp.java	1.9 02/06/13
 */

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.event.*;
/*
 * @version 1.9 06/13/02
 * @author Steve Wilson
 */
public class MetalworksHelp extends JFrame {
    public static Container chtml;
    public MetalworksHelp(int hsize, int vsize, int xpos, int ypos, String title, String fichero) {
	super(title);

	HtmlPane html = new HtmlPane(fichero);
	setContentPane(html);
	chtml = getContentPane();
	setSize(hsize,vsize);

	setVisible(true);
    }



}


class HtmlPane extends JScrollPane implements HyperlinkListener {
    JEditorPane html;
    private JMenuItem items[];  
    private ButtonGroup colorGroup;
    private JPopupMenu popupMenu;
    String colors[] = { "Inicio", "Atras"};
    private URL url, inicial, viejo,old;

    public HtmlPane(String fichero) {
	popupMenu = new JPopupMenu();
	items = new JMenuItem[ 2 ];
	ItemHandler handler = new ItemHandler();
	// construct each menu item and add to popup menu; also
	// enable event handling for each menu item
	colors[0] = DiMol2D.messages.getString("home");
	colors[1] = DiMol2D.messages.getString("atras");
	for ( int count = 0; count < items.length; count++ ) {
	    items[ count ] = new JMenuItem( colors[ count ] );
	    popupMenu.add( items[ count ] );
	    items[ count ].addActionListener(handler );
	}


	try {
	    url = getClass().getResource(fichero);
	    String s = url.toString();
	    viejo = url;
	    old = url;
	    inicial = url;
	    html = new JEditorPane(s);
	    html.setEditable(false);
	    html.addHyperlinkListener(this);

	    JViewport vp = getViewport();
	    html.addMouseListener(new MouseAdapter() {  // anonymous inner class
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
			    popupMenu.show(event.getComponent(), event.getX(), event.getY() );  
			}
		    }
		    
		} // end anonymous inner clas
			 
			     ); // end call to addMouseListener
	    vp.add(html);

	} catch (MalformedURLException e) {
	    System.out.println("Malformed URL: " + e);
	} catch (IOException e) {
	    System.out.println("IOException: " + e);
	}
	
    }

    private class ItemHandler implements ActionListener {
	
	// process menu item selections
	public void actionPerformed( ActionEvent event ) {
	    // determine which menu item was selected
	    Cursor c = html.getCursor();
	    Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
	    html.setCursor(waitCursor);
	    
	    if (event.getActionCommand() == DiMol2D.messages.getString("home")) {
		SwingUtilities.invokeLater(new PageLoader(inicial, c));
	    }
	    else if (event.getActionCommand() == DiMol2D.messages.getString("atras")) {
		SwingUtilities.invokeLater(new PageLoader(old, c));
	    }
	    
	    
	} // end private inner class ItemHandler
    }
    /**
     * Notification of a change relative to a 
     * hyperlink.
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
	if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	    url = e.getURL();
	    linkActivated(url);
	}
    }
    
    /**
     * Follows the reference in an
     * link.  The given url is the requested reference.
     * By default this calls <a href="#setPage">setPage</a>,
     * and if an exception is thrown the original previous
     * document is restored and a beep sounded.  If an 
     * attempt was made to follow a link, but it represented
     * a malformed url, this method will be called with a
     * null argument.
     *
     * @param u the URL to follow
     */
    protected void linkActivated(URL u) {
	Cursor c = html.getCursor();
	Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
	html.setCursor(waitCursor);
	SwingUtilities.invokeLater(new PageLoader(u, c));
    }

    /**
     * temporary class that loads synchronously (although
     * later than the request so that a cursor change
     * can be done).
     */
    class PageLoader implements Runnable {
	
	PageLoader(URL u, Cursor c) {
	    old = viejo;
	    url = u;
	    viejo = url;
	    cursor = c;
	}

        public void run() {
	    if (url == null) {
		// restore the original cursor
		html.setCursor(cursor);
	// PENDING(prinz) remove this hack when 
		// automatic validation is activated.
		Container parent = html.getParent();
		parent.repaint();
	    } else {
		Document doc = html.getDocument();
		try {
		    html.setPage(url);
		} catch (IOException ioe) {
		    html.setDocument(doc);
		    getToolkit().beep();
		} finally {
		    // schedule the cursor to revert after
		    // the paint has happended.
		    url = null;
		    SwingUtilities.invokeLater(this);
		}
	    }
	}

	URL url;
	Cursor cursor;
    }



}
