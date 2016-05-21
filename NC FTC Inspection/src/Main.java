import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Main extends JFrame {

	public static HashMap<Integer,String> teamData=new HashMap<Integer,String>();


	//TODO Monitoring GUI- allow editing what teams are there
	/*
	 * TODO Decide how events are structured:
	 * 
	 * Premake team list for each event 
	 * or select teams during setup?
	 * 
	 * teamdata.dat will have team # and name for each NC team (can make from data on FIRST's website- maybe make a script for that)
	 * add team location?
	 * 
	 * 
	 * Also, lets avoid any items above java 1.6 incase this ends up running on linux (a Pi for example)
	 * 
	 * May want to rename class to make it more legit.
	 * 
	 *TODO Handle Sizing Cube tracking done by index 3 on team.hw;
	 *TODO Handle Signatures.
	 *
	 *TODO could have rules column of forms direct you to that rule in the manual? (Super Long-term goal) but itd be really cool
	 *
	 *TODO help pages are probably an important thing
	 */

	public Main() {
		super("NC FTC Inspection Server");
	}
	public static Main me;
	public static void main(String[] args) {
		loadFiles();
		me = new Main();
		me.initGUI();
		try {
			Server.theServer.startServer(80);
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "Error loading server:\n\t" + e.getMessage(), "Server Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			System.exit(0);
		}
	}
	private JPanel pwPanel = new JPanel();
	private JPasswordField pw1 = new JPasswordField(15);
	private JPasswordField pw2 = new JPasswordField(15);
	private JLabel pw1Label = new JLabel("Please enter a password");
	private JLabel pw2Label = new JLabel("Re-enter the password");
	private JLabel pwStatus = new JLabel("");
	private JButton pwEnter = new JButton("Set Password");
	
	private JPanel statusPanel = new JPanel();
	private JLabel cookieLabel = new JLabel("Cookies Issued: ");
	private JLabel cookieCount = new JLabel("0");
	private String trafficString = "Traffic (15s bin): ";
	private JLabel trafficLabel = new JLabel(trafficString);
	private JPanel trafficPanel = new JPanel();
	private Thread graphics;
	private int[] traffic = new int[50];
	private void initGUI() {
		GridBagConstraints c = new GridBagConstraints();
		this.getContentPane().setLayout(new GridBagLayout());
		pwPanel.setLayout(new GridBagLayout());
		pwPanel.setBorder(new TitledBorder("Set Password"));
		pw1.setEchoChar(((char)8226)); // dot
		pw2.setEchoChar(((char)8226)); // dot
		DocumentListener pwListener = new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				checkFields();
			}
			
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				checkFields();
			}
			
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				checkFields();
			}
			
			public void checkFields() {
				char[] one = pw1.getPassword();
				char[] two = pw2.getPassword();
				boolean enabled = false;
				d:if (one.length == two.length) {
					for (int i = 0; i < one.length; i++)
						if (one[i] != two[i]) {
							enabled = false;
							break d;
						} 
					enabled = true;
				}
				if (!enabled) {
					pwStatus.setText("Passwords do not match");
				} else {
					pwStatus.setText("Passwords match");
				}
				if (one.length == 0) {
					enabled = false;
					pwStatus.setText("");
				}
				pwEnter.setEnabled(enabled);
				pack();
			}
		};
		pw1.getDocument().addDocumentListener(pwListener);
		pw2.getDocument().addDocumentListener(pwListener);
		pwEnter.setEnabled(false);
		pwEnter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Server.theServer.setPassword(new String(pw1.getPassword()));
			}
		});
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		pwPanel.add(pw1Label, c);
		c.gridy = 1;
		pwPanel.add(pw2Label, c);
		c.gridx = 1;
		c.gridy = 0;
		pwPanel.add(pw1, c);
		c.gridy = 1;
		pwPanel.add(pw2, c);
		c.gridy = 0;
		c.gridx = 2;
		pwPanel.add(pwStatus, c);
		c.gridy = 1;
		pwPanel.add(pwEnter, c);
		
		c.gridx = 0;
		c.gridy = 0;
		this.getContentPane().add(pwPanel, c);
		
		statusPanel.setLayout(new GridBagLayout());
		statusPanel.setBorder(new TitledBorder("Server Status"));
		statusPanel.add(trafficLabel, c);
		c.gridx = 1;
		statusPanel.add(cookieLabel, c);
		c.gridx = 2;
		statusPanel.add(cookieCount, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		c.gridheight = 2;
		
		trafficPanel.setPreferredSize(new Dimension(300, 100));
		statusPanel.add(trafficPanel, c);
		
		c.gridheight = 1;
		c.gridwidth = 1;
		this.getContentPane().add(statusPanel, c);
		this.pack();
		this.setVisible(true);
		this.setLocationRelativeTo(null);
//		trafficPanel.setPreferredSize(getPreferredSize());
		trafficPanel.setOpaque(true);
		graphics = new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				while (true) {
					cookieCount.setText(Server.theServer.getCookieCount() + "");
					System.arraycopy(traffic, 1, traffic, 0, traffic.length - 1); // shift array 1 left
					System.out.println(Arrays.toString(traffic));
					traffic[traffic.length - 1] = Server.theServer.getTraffic();
					trafficLabel.setText(trafficString + traffic[traffic.length - 1]);
					int max = 5;
					for (int i : traffic)
						max = Math.max(max, i);
					Graphics g = trafficPanel.getGraphics();
					int x, y, width, height;
					g.setColor(Color.black);
					g.fillRect(-1, -1, trafficPanel.getWidth() + 2, trafficPanel.getHeight() + 2);
					for (int i = 0; i < traffic.length; i++) {
						g.setColor(Color.getHSBColor((float) (.333 - .333 * ((double) traffic[i] / max)), 1, 1));
						x = i * trafficPanel.getWidth() / traffic.length;
						y = trafficPanel.getHeight() - traffic[i] * trafficPanel.getHeight() / max;
						
						width = trafficPanel.getWidth() / traffic.length;
						height =  traffic[i] * trafficPanel.getHeight() / max;
						
						g.fillRect(x, y, width, height);
					}
					try {
						Thread.sleep(15000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		graphics.start();
//		this.pack();
	}


	private static void loadFiles() {
		Scanner scan = null;
		try {
			scan = new Scanner(new File("Resources/teamdata.dat"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while(scan.hasNextLine()){
			try{
				String line=scan.nextLine();
				System.out.println(line);
				int num=Integer.parseInt(line.substring(0, line.indexOf(":")));
				String name=line.substring(line.indexOf(":")+1);
				teamData.put(num, name);
			}catch(Exception e){
				e.printStackTrace();
			}
		}	
		scan.close();

		try {
			scan=new Scanner(new File("Resources/hwform.dat"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while(scan.hasNextLine()){
			try{
				String line=scan.nextLine();
				line=line.replaceAll("<","&lt;");
				line=line.replaceAll(">","&gt;");
				line=line.replaceAll(":", "</td><td>");
				System.out.println(line);
				Server.HWForm.add(line);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		scan.close();

		try {
			scan=new Scanner(new File("Resources/swform.dat"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while(scan.hasNextLine()){
			try{
				String line=scan.nextLine();
				line=line.replaceAll(":", "</td><td>");
				Server.SWForm.add(line);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		scan.close();

		try {
			scan=new Scanner(new File("Resources/fdform.dat"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while(scan.hasNextLine()){
			try{
				String line=scan.nextLine();
				line=line.replaceAll(":", "</td><td>");
				Server.FDForm.add(line);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		// TODO Auto-generated method stub

	}

}
