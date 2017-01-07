package nc.ftc.inspection;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JEditorPane;

/**
 * This is for the cmd line interface. The system uses HTML code to render the commands, and this
 * class hides the need for the the interfacing code to care about some of the details of appending to html code.
 * @author Trey
 *
 */
public class FTCEditorPane extends JEditorPane {
	/**
	 * 
	 */
	private static final long serialVersionUID = 602436360740296867L;
	String text = "";
	Main me;
	/**
	 * Creates a new empty command line display. This sets the background to black and the foreground to white.
	 */
	public FTCEditorPane(Main me) {
		setContentType("text/html");
		setEditable(false);
		setBackground(Color.black);
		setForeground(Color.white);
		setMaximumSize(new Dimension(10000, 400));
		this.me = me;
	}
	/**
	 * This appends the given string to the command line and refreshes the GUI
	 * @param t The String to append
	 */
	public void append(String t) {
		text += t;
		setText("<html><body>" + text + "</body></html>");
		//System.out.println(getText());
		//			Document d = getDocument();
		//			try {
		//				d.insertString(d.getLength(), t, null);
		//			} catch (BadLocationException e) {
		//			}
		//			System.out.println(getText());
		me.pack();
	}
	
	/**
	 * Returns the text without the leading and trailing html and body tags.
	 * NOTE: this is still HTML stylized, and currently there is no helper methods to convert back.
	 * This is currently used for serving the /admin page which ports the cmd line to the web
	 * @return
	 */
	public String getPlainText() {
		return text;
	}
	/**
	 * Clears the text of the command line
	 */
	public void clear() {
		text = "";
	}

}