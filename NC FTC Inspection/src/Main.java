import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;

import java.util.HashMap;
import java.util.Scanner;
import java.util.Timer;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.IconUIResource;

public class Main extends JFrame {

	public static HashMap<Integer,String> teamData=new HashMap<Integer,String>();


	
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
	 * 
	 *TODO Handle Sizing Cube tracking done by index 3 on team.hw;
	 *TODO Handle Signatures.
	 *
	 *TODO could have rules column of forms direct you to that rule in the manual? (Super Long-term goal) but itd be really cool
	 *
	 *TODO help pages are probably an important thing
	 *
	 *TODO if web page cant send POST due to disconnect, have a button at bottom of page to send all data from page for reconnect?
	 *
	 *TODO save status data every minute or so
	 *
	 *TODO capability to run headless. just in case
	 *
	 *TODO checkboxes for which stages of inspection to track
	 *TODO save which to track? save in event or .config?
	 */

	public Main() {
		super("NC FTC Inspection Server");
	}
	//public static File rootSaveDir=new File("");//root dir for all saved data
	public static long autoSave=60000;//default every minute
	public static Main me;
	public static Vector<String> events=new Vector<String>();
	public static Thread autoSaveThread;
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
		autoSaveThread=new Thread("AutoSave"){
			public void run(){
				System.out.println("Started Autosave thread.");
				while(true){
					try{
						Thread.sleep(autoSave);
						Server.save();
						System.out.println("AutoSave");
					}catch(InterruptedException e){
						return;
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		};
		autoSaveThread.setDaemon(true);
		autoSaveThread.start();
		
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
	private JPanel trafficPanel = new JPanel() {
		@Override
		public void paint(Graphics g) {
			int max = 5;
			for (int i : traffic)
				max = Math.max(max, i);
//			Graphics g = trafficPanel.getGraphics();
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
			int top = 4;
			g.setColor(Color.white);
			for (int i = 0; i < top - 1; i++) {
				g.drawString("" + max * (top - i) / top, 0, trafficPanel.getHeight()  * i / top + g.getFontMetrics().getHeight());
			}
		}
	};
	/*
	 * TODO add a scrollpane to show the elements of Server.statusLog
	 * Make Server.addLogEntry fire ChangeEvent or something to trigger update for it 
	 */
	
	private JPanel consolePanel = new JPanel();
	JTextArea consoleTextArea = new JTextArea();
	private JScrollPane consoleScrollPane = new JScrollPane(consoleTextArea);
	private JTextField consoleField = new JTextField();
	private JPanel consoleInputPanel = new JPanel();
	private JLabel consoleInputLabel = new JLabel("Console: ");
	
	
	private Thread graphics;
	private int[] traffic = new int[50];
	private void initGUI() {
		setIconImage(new ImageIcon(getClass().getResource("firstfavicon.png")).getImage());
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			
			public void windowClosing(WindowEvent e) {
				int answer = JOptionPane.showConfirmDialog(null, "This will close the server, are you sure?");
//				System.out.println("ANSWER: " + answer);
				if (answer == JOptionPane.YES_OPTION) {
					Server.stopServer();
					//TODO a thread keeps running. we need to find it and make it interruptible or daemon
					setVisible(false);
					dispose();
				} else {
					setVisible(true);
					System.err.println("Opening");
				}
			}
		});
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
		c.gridheight = 2;
		this.getContentPane().add(pwPanel, c);
		
		c.gridheight = 1;
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
		
		consolePanel.setBorder(new TitledBorder("Server Console"));
		consolePanel.setLayout(new GridBagLayout());
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 8;
		c.gridwidth = 2;
		consolePanel.add(consoleScrollPane, c);
		
		consolePanel.setPreferredSize(getPreferredSize());
		
		consoleInputPanel.setLayout(new GridBagLayout());
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		consoleInputPanel.add(consoleInputLabel, c);
		c.gridx = 1;
		c.gridwidth = 5;
		consoleInputPanel.add(consoleField, c);
		
		c.gridheight = 1;
		c.gridy = 8;
		consolePanel.add(consoleInputPanel, c);
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 4;
		this.getContentPane().add(consolePanel, c);
		
		c.gridx = 0;
		c.gridy = 2;
		c.gridheight = 2;
		c.gridwidth = 1;
		this.getContentPane().add(statusPanel, c);
		this.pack();
		this.setVisible(true);
		this.setLocationRelativeTo(null);
//		consoleField.setPreferredSize(new Dimension((int) getPreferredSize().getWidth(), 25));
//		trafficPanel.setPreferredSize(getPreferredSize());
		consoleField.addKeyListener(new KeyAdapter(){
			public void keyReleased(KeyEvent e){
				if(e.getKeyCode()==KeyEvent.VK_ENTER){
					handleCommand(consoleField.getText());
					consoleField.setText("");
				}
			}
		});
		trafficPanel.setOpaque(true);
		boolean running=true;
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
//					System.out.println(Arrays.toString(traffic));
					traffic[traffic.length - 1] = Server.theServer.getTraffic();
					trafficLabel.setText(trafficString + traffic[traffic.length - 1]);
					trafficPanel.invalidate();
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
		//load team data- numbers and names for all teams in NC
		Scanner scan = null;
		try {
			scan =Resources.getScanner("teamdata.dat");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		while(scan.hasNextLine()){
			try{
				String line=scan.nextLine();
				//System.out.println(line);
				int num=Integer.parseInt(line.substring(0, line.indexOf(":")));
				String name=line.substring(line.indexOf(":")+1);
				teamData.put(num, name);
			}catch(Exception e){
				e.printStackTrace();
			}
		}	
		if(scan!=null)scan.close();

		//TODO need to do something if any of these throw an exception?
		try {
			loadInspectionForm("hwform.dat",Server.HWForm);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		try {
			loadInspectionForm("swform.dat",Server.SWForm);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		try {
			loadInspectionForm("fdform.dat",Server.FDForm);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		try {
			scan=Resources.getScanner("events.dat");
			while(scan.hasNextLine()){
				events.add(scan.nextLine());
			}
			Server.event=events.get(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if(scan!=null)scan.close();

	}
	
	
	public static void loadInspectionForm(String srcFile, Vector<String> target) throws FileNotFoundException{
		
		Scanner scan=Resources.getScanner(srcFile);			
		while(scan.hasNextLine()){
			try{
				String line=scan.nextLine();
				line=line.replaceAll("<","&lt;");
				line=line.replaceAll(">","&gt;");
				line=line.replaceAll(":", "</td><td>");
				target.add(line);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public void handleCommand(String command){
		//TODO implement commands
		
		/*   this does not include arguments
		 * 
		 * LIST events
		 * 		teams (-a for all nc teams)
		 *      STATUS (for a team)
		 *
		 *ADD    team*
		 *       event*
		 *REMOVE team*
		 *SET   status
		 *      password
		 *      root**
		 *      autosave
		 *      TEAM [#]* <name> (sets name- # would be add)
		 *      EVENT
		 *           NAME*
		 *           CODE* - nasty- gotta move files 
		 *      
		 *CHANGE event
		 *SELECT event-same
		 *
		 *CLEAR DATA*
		 *DLEAR ALL LOCAL DATA? (resets to factory settings and events -deletes save dir)
		 * 
		 *SAVE*
		 * 
		 * 
		 */
		consoleTextArea.append(command+"\n");
		boolean success=false;//return to not show success
		String[] args=command.split(" ");
		if(args.length>0){
			args[0]=args[0].toUpperCase();
		
			if(args[0].equals("LIST")){
				if(args.length>1){
					args[1]=args[1].toUpperCase();
					if(args[1].equals("EVENTS")){
						for(String e:events){
							consoleTextArea.append(e+"\n");
						}
						return;
					}
					else if(args[1].equals("TEAMS")){
						if(args.length>2 && args[2].toUpperCase().equals("-A")){
							//list all nc teams in order
							Integer[] copy=teamData.keySet().toArray(new Integer[1]);
							Arrays.sort(copy);
							for(Integer num:copy){
								append(num+": "+teamData.get(num));
							}
							return;
						}
						else{
							for(Team t:Server.theServer.teams){
								append(t.number+": "+t.name);
							}
							return;
						}
					}
					else if(args[1].equals("STATUS")){						
						
						try{
							int num=Integer.parseInt(args[2]);
							Team t=Server.theServer.getTeam(num);
							if(t==null){
								if(teamData.containsKey(num))append(num+" "+teamData.get(num)+" is not in this event.");
								else append("Unrecognized team #: "+num);
								return;
							}
							append(t.number+": "+t.name);
							append((t.checkedIn?"":"NOT ")+"Checked In");
							append((t.cube==Server.PASS?"":"NOT")+"passed sizing cube");
							append((t.hardware==Server.PASS?"":"NOT")+"passed hardware");
							append((t.software==Server.PASS?"":"NOT")+"passed software");
							append((t.field==Server.PASS?"":"NOT")+"passed field");
							return;
						}
						catch(Exception e){
							append("USAGE: LIST STATUS <number>");
							return;
						}
					}
					else{
						append("USAGE: LIST [EVENTS | TEAMS | STATUS]");
						return;
					}
				}else{
					append("USAGE: LIST [EVENTS | TEAMS | STATUS] ");
					return;
				}
			}
			else if(args[0].equals("CHANGE") || args[0].equals("SELECT")){
				if(args.length>1){
					if(args[1].toUpperCase().equals("EVENT")){
						if(args.length>2){
							if(events.contains(args[2])){
								success=Server.changeEvent(args[2]);
							}
						}else{
							append("USAGE: CHANGE [EVENT] <code>");
							return;
						}
					}
				}
				else{
					append("USAGE: CHANGE [EVENT] <code>");
					return;
				}
			}
			else if(args[0].equals("ADD")){
				if(args.length>1){
					if(args[1].toUpperCase().equals("TEAM")){
						try{
							int num=Integer.parseInt(args[2]);
							success=Server.theServer.teams.add(new Team(num));
							Collections.sort(Server.theServer.teams);
							success&=Resources.saveEventFile();
						}catch(Exception e){
							append("FAILED: USAGE: ADD TEAM <number>");
							return;
						}
					}
					if(args[1].toUpperCase().equals("EVENT")){ 
						if(args.length>3){
							String name="";
							if(args.length>3)name=args[3];
							for(int i=4;i<args.length;i++){
								name+=" "+args[i];
							}
							if(Resources.createEventFile(args[2],name)){ 
								events.add(args[2]);
								success=Resources.saveEventsList();
							}
						}else{
							append("USAGE: ADD [EVENT] <code> <name>");
							return;
						}
					}
				}else{
					append("USAGE: ADD [TEAM | EVENT] <number | code> <name>");//also least team #s for event
					return;
				}
			}
			else if(args[0].equals("REMOVE")){
				if(args.length>2 && args[1].toUpperCase().equals("TEAM")){
					try{
						Team t=Server.theServer.getTeam(Integer.parseInt(args[2]));
						System.out.println(t);
						if(t==null){
							append("Team was not in event");
							return;
						}else{
							success = Server.theServer.teams.remove(t);
							success &= Resources.saveEventFile();//save removal
						}
					}catch(Exception e){
						append("USAGE: REMOVE TEAM <number>");
						return;
					}
				}
				else{
					append("USAGE: REMOVE TEAM <number>");
					return;
				}
				
			}
			else if(args[0].equals("SET")){
				if(args.length>1){
					args[1]=args[1].toUpperCase();
					if(args[1].equals("STATUS")){
						if(args.length>4){
							int num=Integer.parseInt(args[2]);
							String type=args[3];
							try{
								int stat=Integer.parseInt(args[4]);
								Server.theServer.getTeam(num).set(type, stat);
								success=true;
							}catch(Exception e){
								//not numbe status
								try {
									int stat=Server.class.getDeclaredField(args[4]).getInt(null);
									Server.theServer.getTeam(num).set(type, stat);
									success=true;
								} catch (Exception e1) {
									e1.printStackTrace();
									success=false;
								} 
							}
						}
						else{
							append("USAGE: SET STATUS <number> <type> <status>");
							append("<type= CI | SC | HW | SW | FD>");
							append("<status= 0 | 1 | 2 | 3 | NO_DATA | FAIL | PROGRESS | PASS>");
							return;
						}
					}
				}
				else{
					append("USAGE: SET [STATUS | PASSWORD | ROOT | AUTOSAVE | TEAM | EVENT [NAME | CODE] ] <value>");
					return;
				}
			}
			else if(args[0].equals("CLEAR")){
				//TODO warning
				if(args.length>1){
					args[1]=args[1].toUpperCase();
					if(args[1].equals("DATA")){
						success=Server.clearData();
					}
					if(args[1].equals("CONSOLE")){
						consoleTextArea.setText("");
						return;
					}
					else{
						append("USAGE: CLEAR [CONSOLE | DATA]");
						return;
					}
				}else{
					append("USAGE: CLEAR [CONSOLE | DATA]");
					return;
				}
			}
			else if(args[0].equals("SAVE")){
				success=Server.save();
			}
			else{
				append("UNKNOW COMMAND:"+args[0]);
				return;
			}
			consoleTextArea.append((success?"SUCCESS":"FAILED")+"\n");
		}
	}
	
	
	public void append(String s){
		consoleTextArea.append(s+"\n");
		//TODO scroll down
	}
	
	
}
