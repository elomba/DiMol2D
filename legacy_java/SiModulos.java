import java.awt.*;
import java.awt.event.*;
import java.io.*;
// import javax.swing.*;
// import javax.swing.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.swing.colorchooser.*;
import javax.swing.filechooser.*;

import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.text.DecimalFormat;
import java.net.*;
import java.net.URL;

public class SiModulos extends JPanel implements CommonVars
{ 

    public ImageIcon createImageIcon(String filename, String description) {
	    String path = "/Icon/" + filename;
	    return new ImageIcon(getClass().getResource(path), description); 
    }
    public SiModulos () {

    }
}
    
