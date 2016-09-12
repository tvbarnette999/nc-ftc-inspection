package nc.ftc.inspection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.FileNotFoundException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


public class Main extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2371487851745548963L;


	/**
	 * Mapping of all NC team numbers to names.
	 */
	public static HashMap<Integer,String> teamData=new HashMap<Integer,String>();


	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat ("[hh:mm:ss] ");

	public static final boolean NIMBUS = true;

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
	 *
	 *TODO could have rules column of forms direct you to that rule in the manual? (Super Long-term goal) but itd be really cool
	 *
	 *TODO help pages are probably an important thing
	 *
	 *TODO if web page cant send POST due to disconnect, have a button at bottom of page to send all data from page for reconnect?
	 *-or keep a vector in js or something?
	 *
	 *TODO have server respond with notes and signatures to confirm.?
	 *
	 *TODO capability to run headless. just in case
	 *
	 *TODO save which to track? save in server.config in root?
	 *
	 *TODO Some GUI easy way to select root save dir.
	 *
	 *
	 *
	 *
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
		for(String s:args){
			if(s.startsWith("root=")){
				//TODO set root
			}
			if(s.startsWith("headless")){
				//TODO headless
			}
		}

		me = new Main();
		loadFiles();
		me.initGUI();

		try {
			Server.theServer.startServer(80);
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "Error loading server:\n\t" + e.getMessage(), "Server Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			System.exit(0);
		}
		autoSaveThread = new Thread("AutoSave"){
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
						me.error(e.getLocalizedMessage());
					}
				}
			}
		};
		autoSaveThread.setDaemon(true);
		autoSaveThread.start();

	}
	private ImageIcon ftcIcon;
	private JTabbedPane tabbedPane = new JTabbedPane();
	private JPanel eventSettingsPanel = new JPanel();
	private JPanel serverSettingsPanel = new JPanel();
	private JPanel leftPanel = new JPanel();
	private JPanel pwPanel = new JPanel();
	private JPanel pwSub1 = new JPanel();
	private JPanel pwSub2 = new JPanel();
	private JPanel pwSub3 = new JPanel();
	private JPasswordField pw1 = new JPasswordField(15);
	private JPasswordField pw2 = new JPasswordField(15);
	private JLabel pw1Label = new JLabel("Please enter a password");
	private JLabel pw2Label = new JLabel("Re-enter the password");
	private JLabel pwStatus = new JLabel("");
	private JButton pwEnter = new JButton("Set Password");
	private JPanel statusPanel = new JPanel();
	private JPanel topStatusPanel = new JPanel();
	private JPanel trackingPanel = new JPanel();
	private JPanel eventPanel = new JPanel();
	private JPanel teamPanel = new JPanel();
	private JPanel teamButtons = new JPanel();
	private JButton addTeam = new JButton("Add Team");
	private JButton removeTeam = new JButton("Remove Team");
	private JButton editTeam = new JButton("Edit Team");
	private JList<Team> teamList = new JList<Team>();
	private static final String COOKIE_LABEL_STRING = "Cookies Issued: ";
	
	
	private JCheckBox trackCheckIn = new JCheckBox("Check In",true);
	private JCheckBox trackCube = new JCheckBox("Sizing Cube",true);
	private JCheckBox separateCube = new JCheckBox("Separate Cube from HW",true);
	private JCheckBox trackHardware = new JCheckBox("Hardware",true);
	private JCheckBox fullHardware = new JCheckBox("Full Hardware",true);
	private JCheckBox trackSoftware = new JCheckBox("Software",true);
	private JCheckBox fullSoftware = new JCheckBox("Full Software",true);
	private JCheckBox trackField = new JCheckBox("Field",true);
	private JCheckBox fullField = new JCheckBox("Full Field",true);
	private static final int INDENT = 50;
	private ActionListener trackListener = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			Server.trackCheckIn = trackCheckIn.isSelected();
			Server.trackHardware = trackHardware.isSelected();
			if(!Server.trackHardware){
				fullHardware.setSelected(false);
				fullHardware.setEnabled(false);
			}
			else{
				fullHardware.setEnabled(true);
			}
			Server.trackField = trackField.isSelected();
			if(!Server.trackField){
				fullField.setSelected(false);
				fullField.setEnabled(false);
			}
			else{
				fullField.setEnabled(true);
			}
			Server.trackSoftware = trackSoftware.isSelected();
			if(!Server.trackSoftware){
				fullSoftware.setSelected(false);
				fullSoftware.setEnabled(false);
			}
			else{
				fullSoftware.setEnabled(true);
			}
			
			
			Server.fullHardware = fullHardware.isSelected();
			Server.fullSoftware = fullSoftware.isSelected();
			Server.fullField = fullField.isSelected();
			Server.trackCube  = trackCube.isSelected();
			
			
			if(!Server.trackCube){
				separateCube.setSelected(false);
				separateCube.setEnabled(false);
			}
			//if cube but not full hw, must be separate
			else if(Server.trackCube && !Server.fullHardware){
				separateCube.setSelected(true);
				separateCube.setEnabled(false);
			}
			else{
				separateCube.setEnabled(true);
			}
			Server.separateCube = separateCube.isSelected();			
			Server.theServer.saveConfig();
		}
	};
	private ActionListener teamEditListener= new ActionListener(){
		public void actionPerformed(ActionEvent e){
			Object src = e.getSource();
			if(src == editTeam){
				//popup to edit
				Team t = teamList.getSelectedValue();
				
				JTextField field1 = new JTextField(""+t.number);
				JTextField field2 = new JTextField(t.name);
				Object[] message = {
				    "Team Number:", field1,
				    "Team Name:", field2,
				};
				boolean ok = false;
				while(!ok){
					int option = JOptionPane.showConfirmDialog(Main.this, message, "Enter Team Info", JOptionPane.OK_CANCEL_OPTION);
					if (option == JOptionPane.OK_OPTION){
					    try{
					    	int num = Integer.parseInt(field1.getText());
					    	if(num != t.number){
						    	if(teamData.containsKey(num)){
							    	JOptionPane.showMessageDialog(Main.this, "Team already exists: "+num, null, JOptionPane.ERROR_MESSAGE);	
							    	continue;
						    	}
						    	teamData.remove(t.number);
						    	t.number = num;
					    	}
					    	ok = true;
					    } catch(NumberFormatException e1){
					    	JOptionPane.showMessageDialog(Main.this, "Invalid team number: "+field1.getText(), "", JOptionPane.ERROR_MESSAGE);	
					    	continue;
					    }
					    t.name = field2.getText();
					    teamData.put(t.number, t.name);
					    Server.save();
					    Resources.saveTeamList();
					    ok = true;
					} else{
						break;
					}
				}
				Server.save();
				
			} else if(src == addTeam){
				//popup with master list
			} else if(src == removeTeam){
				//popop to confirm
				int c = JOptionPane.showConfirmDialog(Main.this, "Remove team "+teamList.getSelectedValue().toString()+"?");
				if(c == JOptionPane.OK_OPTION){
					Server.theServer.teams.remove(teamList.getSelectedValue());
					Server.save();
				}
			}
		}		
	};

	private static final String SERVER_SETTINGS = "Server Settings";
	private static final String EVENT_SETTINGS = "Event Settings";
	private JLabel cookieLabel = new JLabel(COOKIE_LABEL_STRING);
	private String trafficString = "Traffic (15s bin): ";
	private JLabel trafficLabel = new JLabel(trafficString);
	private JPanel trafficPanel = new JPanel() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6310389226347369367L;

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
	FTCEditorPane consoleTextArea = new FTCEditorPane();
	public class FTCEditorPane extends JEditorPane {
		/**
		 * 
		 */
		private static final long serialVersionUID = 602436360740296867L;
		String text = "";
		public FTCEditorPane() {
			setContentType("text/html");
			setEditable(false);
			setBackground(Color.black);
			setForeground(Color.white);
			setMaximumSize(new Dimension(10000, 400));
		}
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
			pack();
		}
		public void clear() {
			text = "";
		}

	}
	private JScrollPane consoleScrollPane = new JScrollPane(consoleTextArea);
	private JTextField consoleField = new JTextField();
	private JPanel consoleInputPanel = new JPanel();
	private JLabel consoleInputLabel = new JLabel("Console: ");


	private Thread graphics;
	private int[] traffic = new int[50];
	private ArrayList<String> commands = new ArrayList<String>();
	private int command;
	private void initGUI() {
		setCheckBoxes();
		if (NIMBUS) {
			try {
//				System.out.println(UIManager.getInstalledLookAndFeels().length);
				for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
					System.out.println(info.toString());
					if ("Nimbus".equalsIgnoreCase(info.getName())) {
						UIManager.setLookAndFeel(info.getClassName());
						break;
					}
				}
			} catch (Exception e) {
				// If Nimbus is not available, you can set the GUI to another look and feel.
			}
		}
		try {
			ftcIcon = new ImageIcon(ImageIO.read(Resources.getInputStream("firstfavicon.png")));
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}//getClass().getResource("Resources/firstfavicon.png"));
		setIconImage(ftcIcon.getImage());
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				int answer = JOptionPane.showConfirmDialog(null, "This will close the server, are you sure?");
				//				System.out.println("ANSWER: " + answer);
				if (answer == JOptionPane.YES_OPTION) {
					Server.stopServer();
					setVisible(false);
					dispose();
					System.exit(0);
				} else {
					setVisible(true);
					System.err.println("Not closing");
				}
			}
		});
		//		GridBagConstraints c = new GridBagConstraints();
		serverSettingsPanel.setLayout(new BorderLayout());
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
		pwPanel.setLayout(new BorderLayout());
		pwPanel.setBorder(new TitledBorder("Set Password"));
		pwSub1.setLayout(new BorderLayout());
		pwSub2.setLayout(new BorderLayout());
		pwSub3.setLayout(new BorderLayout());
		pwSub1.add(pw1Label, BorderLayout.NORTH);
		pwSub1.add(pw2Label, BorderLayout.SOUTH);
		pwSub2.add(pw1, BorderLayout.NORTH);
		pwSub2.add(pw2, BorderLayout.SOUTH);
		pwSub3.add(pwStatus, BorderLayout.NORTH);
		pwSub3.add(pwEnter, BorderLayout.SOUTH);
		pwPanel.add(pwSub1, BorderLayout.WEST);
		pwPanel.add(pwSub2, BorderLayout.CENTER);
		pwPanel.add(pwSub3, BorderLayout.EAST);
		leftPanel.setLayout(new BorderLayout());
		leftPanel.add(pwPanel, BorderLayout.NORTH);
		//		c.gridx = 0;
		//		c.gridy = 0;
		//		c.gridheight = 2;
		//		this.getContentPane().add(pwPanel, c);

		//		c.gridheight = 1;
		statusPanel.setLayout(new BorderLayout());
		topStatusPanel.setLayout(new BorderLayout());
		statusPanel.setBorder(new TitledBorder("Server Status"));
		trafficLabel.setHorizontalAlignment(JLabel.CENTER);
		topStatusPanel.add(trafficLabel, BorderLayout.WEST);
		cookieLabel.setHorizontalAlignment(JLabel.CENTER);
		topStatusPanel.add(cookieLabel, BorderLayout.EAST);
		statusPanel.add(topStatusPanel, BorderLayout.NORTH);
		trafficPanel.setPreferredSize(new Dimension(300, 100));
		statusPanel.add(trafficPanel, BorderLayout.CENTER);

		consolePanel.setBorder(new TitledBorder("Server Console"));
		consolePanel.setLayout(new BorderLayout());

		consolePanel.add(consoleScrollPane, BorderLayout.CENTER);

		consoleInputPanel.setLayout(new BorderLayout());
		consoleInputPanel.add(consoleInputLabel, BorderLayout.WEST);
		consoleInputPanel.add(consoleField, BorderLayout.CENTER);
		consolePanel.add(consoleInputPanel, BorderLayout.SOUTH);
		leftPanel.add(statusPanel, BorderLayout.CENTER);
		serverSettingsPanel.add(leftPanel, BorderLayout.WEST);
		serverSettingsPanel.add(consolePanel, BorderLayout.CENTER);
		//		consolePanel.setPreferredSize(getPreferredSize());
		//		leftPanel.setPreferredSize(getPreferredSize());
		//		this.pack();
		//		setSize(850, 400);
		consoleTextArea.setPreferredSize(new Dimension(550, 375));
		setMinimumSize(new Dimension(850, 400));
		
		
		

		tabbedPane.addTab(SERVER_SETTINGS, UIManager.getIcon("FileChooser.hardDriveIcon"), serverSettingsPanel, SERVER_SETTINGS);
		tabbedPane.addTab(EVENT_SETTINGS, ftcIcon, eventSettingsPanel, EVENT_SETTINGS);
		trackingPanel.setOpaque(true);
		trackingPanel.setBorder(new TitledBorder("Tracking Option"));
		trackingPanel.setPreferredSize(new Dimension(260, 300));
		trackingPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		trackingPanel.add(trackCheckIn);
		trackingPanel.add(Box.createHorizontalStrut(150));
		trackingPanel.add(trackCube);

		trackingPanel.add(Box.createHorizontalStrut(100));
//		trackingPanel.add(new J);

		trackingPanel.add(Box.createHorizontalStrut(INDENT));
		trackingPanel.add(separateCube);
		
		trackingPanel.add(trackHardware);

		trackingPanel.add(Box.createHorizontalStrut(120));
		trackingPanel.add(Box.createHorizontalStrut(INDENT));
		trackingPanel.add(fullHardware);
		trackingPanel.add(Box.createHorizontalStrut(50));
		trackingPanel.add(trackSoftware);
		trackingPanel.add(Box.createHorizontalStrut(120));
		trackingPanel.add(Box.createHorizontalStrut(INDENT));
		trackingPanel.add(fullSoftware);
		trackingPanel.add(Box.createHorizontalStrut(50));
		trackingPanel.add(trackField);

		trackingPanel.add(Box.createHorizontalStrut(150));
		trackingPanel.add(Box.createHorizontalStrut(INDENT));
		trackingPanel.add(fullField);
		
		trackCheckIn.addActionListener(trackListener);
		trackCube.addActionListener(trackListener);
		trackHardware.addActionListener(trackListener);
		trackSoftware.addActionListener(trackListener);
		trackField.addActionListener(trackListener);
		separateCube.addActionListener(trackListener);
		fullHardware.addActionListener(trackListener);
		fullSoftware.addActionListener(trackListener);
		fullField.addActionListener(trackListener);
		
		/*
		 * To right of tracking panel:
		 * Current event name & code
		 * button to edit event info
		 * button to changes event
		 * 
		 * current root directory (save)
		 * button to change root directory
		 * 
		 * team list
		 * use JList:
		 * button to edit team info
		 * button to add new team
		 * 	-brings up list of all teams, which has an option to add new team
		 * button to remove team
		 * 
		 * update resource button: manual, forum.
		 */
		eventPanel.setOpaque(true);
		eventPanel.setBorder(new TitledBorder("Current Event"));
		eventPanel.setPreferredSize(new Dimension(300, 100));
		teamPanel.setOpaque(true);
		teamPanel.setBorder(new TitledBorder("Team Information"));
		//teamPanel.setPreferredSize(new Dimension(300, 300));
		teamList = new JList<Team>(Server.theServer.teams);
		teamList.setBackground(Color.decode("#EEEEEE"));//"#F2F2F2"));
		teamList.setOpaque(true);
	//	teamList.setBorder(BorderFactory.createSoftBevelBorder(BevelBorder.RAISED));
		editTeam.addActionListener(teamEditListener);
		addTeam.addActionListener(teamEditListener);
		removeTeam.addActionListener(teamEditListener);
		teamButtons.setPreferredSize(new Dimension(350,50));
		teamButtons.add(editTeam);
		teamButtons.add(removeTeam);
		teamButtons.add(addTeam);
		teamPanel.setLayout(new BorderLayout());
		teamPanel.add(teamButtons, BorderLayout.SOUTH);
		teamPanel.add(teamList, BorderLayout.CENTER);
		
		eventSettingsPanel.setLayout(new BorderLayout());
		eventSettingsPanel.add(trackingPanel, BorderLayout.WEST);
		eventSettingsPanel.add(teamPanel, BorderLayout.EAST);
		eventSettingsPanel.add(eventPanel,BorderLayout.CENTER);
				
		//listener to update graphics when tab changed
		tabbedPane.addChangeListener(new ChangeListener(){

			@Override
			public void stateChanged(ChangeEvent arg0) {
				if(tabbedPane.getSelectedIndex() == 0){
					updateServerGraphics();
				}
				
			}
			
		});
		this.getContentPane().add(tabbedPane);
		pack();
		this.setVisible(true);
		this.setLocationRelativeTo(null);
		consoleField.addKeyListener(new KeyAdapter(){
			public void keyReleased(KeyEvent e){
				switch (e.getKeyCode()) {
				case KeyEvent.VK_ENTER:
					handleCommand(consoleField.getText());
					commands.add(consoleField.getText());
					command = commands.size();
					consoleField.setText("");	
					break;
				case KeyEvent.VK_UP:
					command-=2;
				case KeyEvent.VK_DOWN:
					command++;
					command = Math.min(Math.max(0, command), commands.size());
					if (command < commands.size()) {
						consoleField.setText(commands.get(command));
					} else {
						consoleField.setText("");
					}
				}
			}
		});
		trafficPanel.setOpaque(true);
		
		graphics = new Thread("Graphics Thread") {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				while (true) {
					//dont do anything if tab is hidden.
					if(tabbedPane.getSelectedIndex() == 0)updateServerGraphics();
					try {
						Thread.sleep(15000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		graphics.setDaemon(true);
		graphics.start();

//		setDefaultCloseOperation(EXIT_ON_CLOSE);
		//		this.pack();

	}
	private void updateServerGraphics(){
		cookieLabel.setText(COOKIE_LABEL_STRING + Server.theServer.getCookieCount() + "");
		System.arraycopy(traffic, 1, traffic, 0, traffic.length - 1); // shift array 1 left
		//					System.out.println(Arrays.toString(traffic));
		traffic[traffic.length - 1] = Server.theServer.getTraffic();
		trafficLabel.setText(trafficString + traffic[traffic.length - 1]);
		//					trafficPanel.invalidate();
		trafficPanel.paint(trafficPanel.getGraphics());
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
			JOptionPane.showMessageDialog(null, "Unable to load Hardware Inspection form!");
		}
		try {
			loadInspectionForm("swform.dat",Server.SWForm);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, "Unable to load Software Inspection form!");
		}

		try {
			loadInspectionForm("fdform.dat",Server.FDForm);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, "Unable to load Field Inspection form!");
		}

		try {
			scan=Resources.getScanner("events.dat");
			while(scan.hasNextLine()){
				events.add(scan.nextLine());
			}
			Server.theServer.loadConfig();
			if(!events.contains(Server.event)){
				if(events.size()>0)Server.event=events.get(0);
				else Server.event=null;
			}
			Server.theServer.loadEvent(Server.event);
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
		 *CLEAR ALL LOCAL DATA? (resets to factory settings and events -deletes save dir)
		 * 
		 * IP - show ip.
		 * 
		 *SAVE*
		 * 
		 * 
		 */
		append(command);
		boolean success=false;//return to not show success
		String[] args=command.split(" ");
		if(args.length>0){
			args[0]=args[0].toUpperCase();

			if(args[0].equals("LIST")){
				if(args.length>1){
					args[1]=args[1].toUpperCase();
					if(args[1].equals("EVENTS")){
						for(String e:events){
							append(e);
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
							append("USAGE: LIST STATUS &lt;number&gt;");
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
								success=changeEvent(args[2]);
							}
						}else{
							append("USAGE: CHANGE [EVENT] &lt;code&gt;");
							return;
						}
					}
				}
				else{
					append("USAGE: CHANGE [EVENT] &lt;code&gt;");
					return;
				}
			}
			else if(args[0].equals("ADD")){
				if(args.length>1){
					if(args[1].toUpperCase().equals("TEAM")){
						try{
							int num=Integer.parseInt(args[2]);
							if(Server.theServer.getTeam(num)!=null){
								append("Team "+num+" already in event");
								return;
							}
							success=Server.theServer.teams.add(new Team(num));
							if(!teamData.containsKey(num)){
								teamData.put(num, null);
							}
							Resources.saveTeamList();
							Collections.sort(Server.theServer.teams);
							success &= Resources.saveEventFile();
							Server.save();
						}catch(Exception e){
							append("FAILED: USAGE: ADD TEAM &lt;number&gt;");
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
							append("USAGE: ADD [EVENT] &lt;code&gt; &lt;name&gt;");
							return;
						}
					}
				}else{
					append("USAGE: ADD [TEAM | EVENT] &lt;number | code> &lt;name&gt;");//also least team #s for event
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
						append("USAGE: REMOVE TEAM &lt;number&gt;");
						return;
					}
				}
				else{
					append("USAGE: REMOVE TEAM &lt;number&gt;");
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
							append("USAGE: SET STATUS &lt;number&gt; &lt;type&gt; &lt;status&gt;");
							append("&lt;type= CI | SC | HW | SW | FD&gt;");
							append("&lt;status= 0 | 1 | 2 | 3 | NO_DATA | FAIL | PROGRESS | PASS&gt;");
							return;
						}
					}
					else if(args[1].equals("AUTOSAVE")){
						try{
							autoSave=Long.parseLong(args[2])*1000;
							success=true;
						}catch(Exception e){
							append("USAGE: SET AUTOSAVE &lt;time (s)&gt;");
							return;
						}
					}
					else if(args[1].equals("TEAMNAME")){
						try{
							int number=Integer.parseInt(args[2]);
							Team t=Server.theServer.getTeam(number);
							if(t==null){
								append("Team "+number+" not found.");
								return;
							}
							String name = args[3];
							t.setName(name);
							teamData.put(number, name);
							Resources.saveTeamList();
						}catch(Exception e){
							append("USAGE: SET TEAMNAME &lt;number&gt; &lt;name&gt;");
							return;
						}
					}
				}
				else{
					append("USAGE: SET [STATUS | PASSWORD | ROOT | AUTOSAVE | TEAMNAME | EVENT [NAME | CODE] ] &lt;value&gt;");
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
						consoleTextArea.clear();
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
			else if(args[0].equals("IP")){
				try {
					append("Server IP: "+InetAddress.getLocalHost().getHostAddress());
				} catch (UnknownHostException e) {
					
				}
				return;
			}
			else if(args[0].equals("HELP")){
				append("Available commands: (Attempt use for more help)");
				append("\tLIST [EVENTS | TEAMS | STATUS]");
				append("\tADD [TEAM | EVENT]");
				append("\tREMOVE TEAM");
				append("\tSET [...]");
				append("\tCHANGE EVENT");
				append("\tSELECT EVENT");
				append("\tIP");
				append("\tSAVE");
				return;
			}
			else{
				error("UNKNOW COMMAND: "+args[0]);
				return;
			}
			append((success?"SUCCESS":"FAILED"));
		}
	}


	public void append(String s) {
		s = fixHTML(s);
		consoleTextArea.append("<font color=\"#888888\">" + DATE_FORMAT.format(Calendar.getInstance().getTime()) + "</font><font color=\"#ffffff\" face=\"lucida console\">" + s + "</font><br>");
	}
	public void error(String s) {
		s = fixHTML(s);
		consoleTextArea.append("<font color=\"#ff0000\">" + DATE_FORMAT.format(Calendar.getInstance().getTime()) + "</font><font color=\"#ff0000\" face=\"lucida console\">" + s + "</font><br>"); 
	}
	public static String fixHTML(String s) {
		return s.replaceAll("\n", "<br>").replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
	}
	
	public void setCheckBoxes(){
		trackCheckIn.setSelected(Server.trackCheckIn);
		trackCube.setSelected(Server.trackCube);
		separateCube.setSelected(Server.separateCube);
		trackHardware.setSelected(Server.trackHardware);
		fullHardware.setSelected(Server.fullHardware);
		trackSoftware.setSelected(Server.trackSoftware);
		fullSoftware.setSelected(Server.fullSoftware);
		trackField.setSelected(Server.trackField);
		fullField.setSelected(Server.fullField);
	}
	
	public boolean changeEvent(String event){
		boolean b=Server.changeEvent(event);
		setCheckBoxes();
		return b;
	}

}
