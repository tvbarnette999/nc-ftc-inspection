package nc.ftc.inspection;

import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import static nc.ftc.inspection.Resources.FD_FORM_FILE;
import static nc.ftc.inspection.Resources.HW_FORM_FILE;
import static nc.ftc.inspection.Resources.SW_FORM_FILE;

public class Main extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2371487851745548963L;




	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat ("hh:mm:ss");
	
	public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MM_DD_YY_hh-mm-ssa");
	

	public static final boolean NIMBUS = true;

	/*
	 * TODO List:
	 * 
	 * Handle inability to load event
	 * Handle incorrect inspection form (form vs team file mismatch)
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
	 *
	 *TODO have server respond with notes and signatures to confirm.?
	 *
	 *TODO capability to run headless. just in case
	 *
	 *
	 *TODO Some GUI easy way to select root save dir.
	 *
	 *TODO static import constants where used multiple times (ie Server.HARDWARE)
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
		try {
			setUpLogFiles();
		} catch (IOException e1) {
			System.err.println("Unable to redirect syserr or sysout");
			e1.printStackTrace();
		}
		
		//TODO popup for non existant root directory?
//		if(!Resources.rootExists()){
//			
//			JTextField field = new JTextField(new File(Resources.root).getAbsolutePath());
//			JButton browse = new JButton("Browse...");
//			browse.addActionListener(new ActionListener(){
//				public void actionPerformed(ActionEvent e){
//					JFileChooser fc = new JFileChooser(Resources.root);
//					fc.showOpenDialog(null);
//					field.setText(fc.getCurrentDirectory().getAbsolutePath());
//				}
//			});
//			JPanel p = new JPanel();
//			p.setPreferredSize(new Dimension(300,23));
//			p.setLayout(new BorderLayout());
//			p.add(browse, BorderLayout.EAST);
//			p.add(field, BorderLayout.CENTER);
//			int choice = JOptionPane.showConfirmDialog(null, new Object[]{"Select Save Directory",p}, "Save", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, UIManager.getIcon("FileChooser.newFolderIcon"));
//			if(choice == JOptionPane.OK_OPTION){
//				Resources.root = field.getText();
//			} else{
//				return; //terminate
//			}
//			
//		}
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
						Resources.backup();
						System.out.println("AutoSave");
					}catch(InterruptedException e){
						return;
					}
					catch(Exception e){
						e.printStackTrace();
						me.error(e.getLocalizedMessage(), null);
					}
				}
			}
		};
		autoSaveThread.setDaemon(true);
		autoSaveThread.start();

	}
	
	private static void setUpLogFiles() throws IOException {
		System.setErr(new RedirectingPrintStream(System.err, "/log/" + DATE_TIME_FORMAT.format(new Date()) + "_ERROR.log"));
		System.setOut(new RedirectingPrintStream(System.out, "/log/" + DATE_TIME_FORMAT.format(new Date()) + "_OUT.log"));
	}
	private static Color BACKGROUND = Color.decode("#EEEEEE");
	
	private ImageIcon ftcIcon;
	private JTabbedPane tabbedPane = new JTabbedPane();
	private JPanel eventSettingsPanel = new JPanel();
	private JPanel serverSettingsPanel = new JPanel();
	private JPanel resourceManagerPanel = new JPanel();
	private JPanel leftPanel = new JPanel();
	private JPanel pwPanel = new JPanel();
	private JPanel pwSub1 = new JPanel();
	private JPanel pwSub2 = new JPanel();
	private JPanel pwSub3 = new JPanel();
	private JPasswordField pw1 = new JPasswordField(15);
	private JPasswordField pw2 = new JPasswordField(15);
	private JLabel pw1Label = new JLabel("Please enter a password");
	private JLabel pw2Label = new JLabel("Re-enter the password");
	private static final String DEFAULT_PW_TEXT = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
			+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
			+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
			+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</html>";
	private JLabel pwStatus = new JLabel(DEFAULT_PW_TEXT);
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
	private JTextField searchTeam = new JTextField();
	private JList<Team> teamList = new JList<Team>();
	private JScrollPane teamScrollPane = new JScrollPane(teamList);
	
	private JPanel eventInfoPanel = new JPanel();
	private JLabel eventNameLabel1 = new JLabel("Event Name:");
	private JLabel eventNameLabel2 = new JLabel();
	private JLabel eventCodeLabel1 = new JLabel("Event Code:");
	private JLabel eventCodeLabel2 = new JLabel();
	private JButton editEvent = new JButton();
	private JButton changeEvent = new JButton();
	
	private JPanel inspectionPanel = new JPanel();
	private JPanel referencePanel = new JPanel();
	private JPanel formEditPanel = new JPanel();
	private FormEditor formEdit = new FormEditor();
	private JScrollPane formScrollPane = new JScrollPane(formEdit);
	private JPanel hardwarePanel = new JPanel();
	private JPanel softwarePanel = new JPanel();
	private JPanel fieldPanel = new JPanel();
	JPanel formBottomPanel = new JPanel();
	JTextField delimiter = new JTextField(){
		private static final long serialVersionUID = -3943675988999287631L;

		public Dimension getPreferredSize(){
			Dimension d = super.getPreferredSize();
			if(d.width < 50) d.width = 50;
			return d;
		}
	};
	JLabel delimiterLabel = new JLabel("Delimiter:");
	JButton saveForm = new JButton("Save");
	JButton resetForm = new JButton("Revert Changes");
	JButton cancelForm = new JButton("Cancel");

	JLabel hardware1 = new JLabel("Hardware:");
	JLabel software1 = new JLabel("Software:");
	JLabel field1 = new JLabel("Field:");
	private JLabel hardwareLabel = new JLabel(Resources.DEFAULT);
	private JLabel softwareLabel = new JLabel(Resources.DEFAULT);
	private JLabel fieldLabel    = new JLabel(Resources.DEFAULT);
	private JButton hardwareEdit = new JButton("Edit");
	private JButton softwareEdit = new JButton("Edit");
	private JButton fieldEdit = new JButton("Edit");
	private JButton hardwareRestore = new JButton("Restore Default");
	private JButton softwareRestore = new JButton("Restore Default");
	private JButton fieldRestore = new JButton("Restore Default");
	private JButton hardwareSelect = new JButton("Select File");
	private JButton softwareSelect = new JButton("Select File");
	private JButton fieldSelect = new JButton("Select File");
	
	
	
	
	
	private JDialog dialog = new JDialog(this, "Add team to event");
	private JButton addSelectedTeam = new JButton("Add Selected Team");
	private JButton newTeam = new JButton("New Team");
	private JPanel dialogBottom = new JPanel();
	private JList<Team> masterList = new JList<Team>();
	private JScrollPane masterScrollPane = new JScrollPane(masterList);
	
	private static final String COOKIE_LABEL_STRING = "Cookies Issued: ";
	
	
	private JCheckBox trackCheckIn = new JCheckBox("Check In",true);
	private JCheckBox trackCube = new JCheckBox("Sizing Cube",true);
	private JCheckBox separateCube = new JCheckBox("Separate Cube from HW",true);
	private JCheckBox trackHardware = new JCheckBox("Hardware",true);
	private JCheckBox fullHardware = new JCheckBox("Full Hardware",true);
	private JCheckBox multiHardware = new JCheckBox("Multi-Team", false);
	private JCheckBox trackSoftware = new JCheckBox("Software",true);
	private JCheckBox fullSoftware = new JCheckBox("Full Software",true);
	private JCheckBox multiSoftware = new JCheckBox("Multi-Team", false);
	private JCheckBox trackField = new JCheckBox("Field",true);
	private JCheckBox fullField = new JCheckBox("Full Field",true);
	private JCheckBox multiField = new JCheckBox("Multi-Team", true);
	private static final int INDENT = 50;
	
	private void editForm(InspectionForm f){
		formEdit.setForm(f);
		delimiter.setText(f.delimiter);
		delimiter.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void changedUpdate(DocumentEvent arg0) {
			}
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				formEdit.setDelimiter(delimiter.getText());
			}
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				formEdit.setDelimiter(delimiter.getText());
			}
		});
		this.formScrollPane.repaint();
	}
	
	private void restoreDefault(String file, InspectionForm form){
		String status = Resources.getFileStatus(file);
		String backup = Resources.getBackup(file);
		
		if(status == Resources.CUSTOM){
			int choice = JOptionPane.showConfirmDialog(Main.this, "This will move the current file to " + backup, "Restore Default Form", JOptionPane.OK_CANCEL_OPTION);
			if(choice != JOptionPane.OK_OPTION) return;
			try {
				Resources.renameResource(file, backup);
			} catch (IOException e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(Main.this, "Failed to move old file! Aborting operation.");
				return;
			}
			try{
				Main.loadInspectionForm(file, form);
			}catch(Exception e1){
				System.err.println("Failed to load default file!");
				e1.printStackTrace();
			}
			
		}
	}
	private ActionListener formListener = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			JButton source = (JButton) e.getSource();
			if(source == hardwareEdit){
				editForm(Server.hardwareForm);
			} else if(source == softwareEdit){
				editForm(Server.softwareForm);
			} else if(source == fieldEdit){
				editForm(Server.fieldForm);
			} else if(source == hardwareRestore){
				//TODO- Move custom one to backup, reload default file
				restoreDefault(HW_FORM_FILE, Server.hardwareForm);
				
			} else if(source == softwareRestore){
				restoreDefault(SW_FORM_FILE, Server.softwareForm);
			} else if(source == fieldRestore){
				restoreDefault(FD_FORM_FILE, Server.fieldForm);
			} else if(source == hardwareSelect){
				//TODO, load file chooser
			} else if(source == softwareSelect){
				
			} else if(source == fieldSelect){
				
			} else if(source == saveForm){
				
				String file = "";
				switch(formEdit.form.type){
					case Server.HARDWARE: file = HW_FORM_FILE; break;
					case Server.SOFTWARE: file = SW_FORM_FILE; break;
					case Server.FIELD:    file = FD_FORM_FILE; break;
				}
				String status = Resources.getFileStatus(file);
				String backup = Resources.getBackup(file);
				
				if(status == Resources.CUSTOM){
					int choice = JOptionPane.showConfirmDialog(Main.this, "This will save as " + file + ",\n moving the old file to " + backup, "Save Custom Form", JOptionPane.OK_CANCEL_OPTION);
					if(choice != JOptionPane.OK_OPTION) return;
					try {
						Resources.renameResource(file, backup);
					} catch (IOException e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(Main.this, "Failed to move old file! Aborting operation.");
						return;
					}
				}
				try {
					Resources.saveForm(formEdit);
				} catch (IOException e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(Main.this, "Failed to save form! Aborting operation.");
					return;
				}
				
				try {
					Main.loadInspectionForm(file, formEdit.form);
				} catch (FileNotFoundException e1) {
				
					e1.printStackTrace();
				}
				refreshFormStatus();
				formEdit.form = null;
				formEdit.list.clear();
				formEdit.removeAll();
				formEdit.revalidate();
				formEdit.repaint();
			} else if(source == resetForm){
				editForm(formEdit.form);
			} else if(source == cancelForm){
				formEdit.form = null;
				formEdit.list.clear();
				formEdit.removeAll();
				formEdit.revalidate();
				formEdit.repaint();
			}
		}
	};
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
			
			if(!Server.fullHardware){
				multiHardware.setSelected(false);
				multiHardware.setEnabled(false);
			}
			if(!Server.fullField){
				multiField.setSelected(false);
				multiField.setEnabled(false);
			}
			if(!Server.fullSoftware){
				multiSoftware.setSelected(false);
				multiSoftware.setEnabled(false);
			}
			
			Server.multiField = multiField.isSelected();
			Server.multiSoftware = multiSoftware.isSelected();
			Server.multiHardware = multiHardware.isSelected();
			
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
			Team t = teamList.getSelectedValue();
			if (t == null && (src == editTeam || src == removeTeam)) {
				JOptionPane.showMessageDialog(Main.this, "Please select a team");
				return;
			}
			if(src == editTeam){
				//popup to edit
				
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
						    	if(Team.doesTeamExist(num)){
							    	JOptionPane.showMessageDialog(Main.this, "Team already exists: "+num+" "+ Team.getTeam(num).name, null, JOptionPane.ERROR_MESSAGE);	
							    	continue;
						    	}
						    	Team.changeNumber(t, num);
					    	}
					    	ok = true;
					    } catch(NumberFormatException e1){
					    	JOptionPane.showMessageDialog(Main.this, "Invalid team number: "+field1.getText(), "", JOptionPane.ERROR_MESSAGE);	
					    	continue;
					    }
					    t.name = field2.getText();
					    Server.save();
					    Resources.saveTeamList();
					    refreshTeamList();
					    ok = true;
					} else{
						break;
					}
				}
				Server.save();
				
			} else if(src == addTeam){
				//popup with master list
				
				refreshMasterList();
				dialog.setLocationRelativeTo(Main.this);
				dialog.setVisible(true);
				teamList.setListData(Server.theServer.teams);
				
			} else if(src == removeTeam){
				//popop to confirm
				int c = JOptionPane.showConfirmDialog(Main.this, "Remove team "+teamList.getSelectedValue().toString()+"?");
				if(c == JOptionPane.OK_OPTION){
					Server.save(); //save the team's data just in case 
					Server.theServer.teams.remove(teamList.getSelectedValue());
					Server.save();
					refreshTeamList();
				}
			} else if(src == addSelectedTeam){
				
				if(masterList.getSelectedValue() == null){
					JOptionPane.showMessageDialog(Main.this, "Please select a team");
					return;
				} 
				Server.addTeam(masterList.getSelectedValue());
				refreshTeamList();
				dialog.setVisible(false);
				
				
			} else if(src == newTeam){
				//popup to enter new team info
				JTextField field1 = new JTextField();
				JTextField field2 = new JTextField();
				Object[] message = {
				    "Team Number:", field1,
				    "Team Name:", field2,
				};
				boolean ok = false;
				while(!ok){
					int num = 0;
					String name = "";
					int option = JOptionPane.showConfirmDialog(Main.this, message, "Enter Team Info", JOptionPane.OK_CANCEL_OPTION);
					if (option == JOptionPane.OK_OPTION){
					    try{
					    	num = Integer.parseInt(field1.getText());
					    	if(Team.doesTeamExist(num)){
						    	JOptionPane.showMessageDialog(Main.this, "Team already exists: "+num+" "+ Team.getTeam(num).name, null, JOptionPane.ERROR_MESSAGE);	
						    	continue;
					    	}					
					    	ok = true;
					    } catch(NumberFormatException e1){
					    	JOptionPane.showMessageDialog(Main.this, "Invalid team number: " + field1.getText(), "", JOptionPane.ERROR_MESSAGE);	
					    	continue;
					    }
					    name = field2.getText();
					    Team.registerTeam(num, name);
					    Resources.saveTeamList();
					    refreshMasterList();
					    masterList.setSelectedValue(Team.getTeam(num), true);
					    ok = true;
					} else{
						break;
					}
				}
			}
		}		
	};

	private static final String SERVER_SETTINGS = "Server Settings";
	private static final String EVENT_SETTINGS = "Event Settings";
	private static final String RESOURCE_MANAGER = "Resource Manager";
	private JLabel cookieLabel = new JLabel(COOKIE_LABEL_STRING);
	private String trafficString = "Traffic (15s bin): ";
	private JLabel trafficLabel = new JLabel(trafficString);
	/**
	 * This is the panel for displaying the traffic graph in the server screen
	 */
	private JPanel trafficPanel = new JPanel() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6310389226347369367L;

		/**
		 * This draws the bar graph of the traffic
		 * @param g The graphics object to draw
		 */
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
	public FTCEditorPane consoleTextArea = new FTCEditorPane(this);

	private JScrollPane consoleScrollPane = new JScrollPane(consoleTextArea);
	private JTextField consoleField = new JTextField();
	private JPanel consoleInputPanel = new JPanel();
	private JLabel consoleInputLabel = new JLabel("Console: ");


	private Thread graphics;
	private int[] traffic = new int[50];
	private ArrayList<String> commands = new ArrayList<String>();
	private int command;
	
	/*TODO GUI Features:
	 * To right of tracking panel:
	 * Current event name & code
	 * button to edit event info
	 * button to changes event
	 * 
	 * current root directory (save)
	 * button to change root directory
	 * 
	 * button to compress event data?
	 * button to view inspection forms/print?
	 * ^^instead, button to generate html folder of all inspection forms.
	 * 
	 * update resource button: manual, forum.
	 */
	
	/**
	 * This quietly closes the server and then stops execution.
	 * NOTE: this is needed to free the port that we are using for comm.
	 */
	public void kill() {
		Server.stopServer();
		setVisible(false);
		dispose();
		System.exit(0);
	}
	/*
	 * TODO divide this method into more moethods? Move each tab init to each own method? Move annonymous definitions outside method?
	 * Basically this method is waaaay to long.
	 */
	/**
	 * This does all of the work to init the GUI. 
	 * NOTE: I think this ONLY does graphics stuff, but we have not guaranteed that it only does this.
	 * If we move to headless ability, we need to check this method. Also, there may be other GUI init outside of this method.
	 */
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
					kill();
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
					pwStatus.setText("Passwords match       ");
				}
				if (one.length == 0) {
					enabled = false;
					pwStatus.setText(DEFAULT_PW_TEXT);
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
				append("Password updated", null);
				pw1.setText("");
				pw2.setText("");
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
		tabbedPane.addTab(RESOURCE_MANAGER, ftcIcon, resourceManagerPanel, RESOURCE_MANAGER);
		
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
		trackingPanel.add(Box.createHorizontalStrut(100));
		trackingPanel.add(multiHardware);
//		trackingPanel.add(Box.createHorizontalStrut(50));
		trackingPanel.add(trackSoftware);
		trackingPanel.add(Box.createHorizontalStrut(120));
		trackingPanel.add(Box.createHorizontalStrut(INDENT));
		trackingPanel.add(fullSoftware);
		trackingPanel.add(Box.createHorizontalStrut(100));
		trackingPanel.add(multiSoftware);
//		trackingPanel.add(Box.createHorizontalStrut(50));
		trackingPanel.add(trackField);

		trackingPanel.add(Box.createHorizontalStrut(150));
		trackingPanel.add(Box.createHorizontalStrut(INDENT));
		trackingPanel.add(fullField);

		trackingPanel.add(Box.createHorizontalStrut(100));
		trackingPanel.add(multiField);
		
		trackCheckIn.addActionListener(trackListener);
		trackCube.addActionListener(trackListener);
		trackHardware.addActionListener(trackListener);
		trackSoftware.addActionListener(trackListener);
		trackField.addActionListener(trackListener);
		separateCube.addActionListener(trackListener);
		fullHardware.addActionListener(trackListener);
		fullSoftware.addActionListener(trackListener);
		fullField.addActionListener(trackListener);
		multiHardware.addActionListener(trackListener);
		multiSoftware.addActionListener(trackListener);
		multiField.addActionListener(trackListener);
		
		
		eventPanel.setOpaque(true);
		eventPanel.setBorder(new TitledBorder("Current Event"));
		eventPanel.setPreferredSize(new Dimension(300, 100));
		
		
		eventInfoPanel.setOpaque(true);
		//event
		
		
		
		
		
		
		
		teamPanel.setOpaque(true);
		teamPanel.setBorder(new TitledBorder("Team Information"));
		//teamPanel.setPreferredSize(new Dimension(300, 300));
		teamList.setListData(Server.theServer.teams);
		teamList.setBackground(BACKGROUND);//"#F2F2F2"));
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
		teamPanel.add(teamScrollPane, BorderLayout.CENTER);
		
		eventSettingsPanel.setLayout(new BorderLayout());
		eventSettingsPanel.add(trackingPanel, BorderLayout.WEST);
		eventSettingsPanel.add(teamPanel, BorderLayout.EAST);
		eventSettingsPanel.add(eventPanel,BorderLayout.CENTER);
		
		//Resource Manager Tab
		
		hardwareEdit.addActionListener(formListener);
		hardwareRestore.addActionListener(formListener);
		hardwareSelect.addActionListener(formListener);
		
		FlowLayout flow = new FlowLayout(FlowLayout.LEFT);
		hardware1.setPreferredSize(new Dimension(70,20));
		hardwarePanel.setLayout(flow);		
		hardwareLabel.setPreferredSize(new Dimension(70,20));
		hardwarePanel.add(hardware1);
		hardwarePanel.add(hardwareLabel);
		hardwarePanel.add(hardwareEdit);
		hardwarePanel.add(hardwareRestore);
		hardwarePanel.add(hardwareSelect);
		
		softwareEdit.addActionListener(formListener);
		softwareRestore.addActionListener(formListener);
		softwareSelect.addActionListener(formListener);
		
		FlowLayout flow2 = new FlowLayout(FlowLayout.LEFT);
		software1.setPreferredSize(new Dimension(70,20));
		softwarePanel.setLayout(flow2);
		softwareLabel.setPreferredSize(new Dimension(70,20));
		softwarePanel.add(software1);
		softwarePanel.add(softwareLabel);
		softwarePanel.add(softwareEdit);
		softwarePanel.add(softwareRestore);
		softwarePanel.add(softwareSelect);
		
		fieldEdit.addActionListener(formListener);
		fieldRestore.addActionListener(formListener);
		fieldSelect.addActionListener(formListener);
		
		FlowLayout flow3 = new FlowLayout(FlowLayout.LEFT);
		field1.setPreferredSize(new Dimension(70,20));
		fieldPanel.setLayout(flow3);
		fieldLabel.setPreferredSize(new Dimension(70,20));
		fieldPanel.add(field1);
		fieldPanel.add(fieldLabel);
		fieldPanel.add(fieldEdit);
		fieldPanel.add(fieldRestore);
		fieldPanel.add(fieldSelect);
		
		
		
		inspectionPanel.setPreferredSize(new Dimension(450,200));
		inspectionPanel.setBorder(new TitledBorder("Inspection Forms"));
		inspectionPanel.add(hardwarePanel);
		inspectionPanel.add(softwarePanel);
		inspectionPanel.add(fieldPanel);

		refreshFormStatus();
		
//		formEditTable.set
		
		formEditPanel.setBorder(new TitledBorder("Form Edit"));
		formEditPanel.setLayout(new BorderLayout());
		
		
		
		formBottomPanel.add(delimiterLabel);
		formBottomPanel.add(delimiter);
		formBottomPanel.add(saveForm);
		formBottomPanel.add(resetForm);
		formBottomPanel.add(cancelForm);
		formEditPanel.add(formBottomPanel, BorderLayout.SOUTH);
		formEditPanel.add(formScrollPane, BorderLayout.CENTER);
		
		saveForm.addActionListener(formListener);
		resetForm.addActionListener(formListener);
		cancelForm.addActionListener(formListener);
		
		
		
		resourceManagerPanel.setLayout(new BorderLayout());
		resourceManagerPanel.add(inspectionPanel, BorderLayout.WEST);
		resourceManagerPanel.add(formEditPanel, BorderLayout.CENTER);
		
		
		
		
		
				
		//listener to update graphics when tab changed
		tabbedPane.addChangeListener(new ChangeListener(){

			@Override
			public void stateChanged(ChangeEvent arg0) {
				if(tabbedPane.getSelectedIndex() == 0){
					updateServerGraphics();
				}
				if(tabbedPane.getSelectedIndex() == 1){
					refreshTeamList();
				}
				
			}
			
		});
		this.getContentPane().add(tabbedPane);
		pack();
		this.setVisible(true);
		this.setLocationRelativeTo(null);
		
		
		refreshMasterList();
		
		masterList.setBackground(BACKGROUND);
		
		dialog.setLayout(new BorderLayout());
		dialogBottom.setPreferredSize(new Dimension(350, 50));
		
		addSelectedTeam.addActionListener(teamEditListener);
		newTeam.addActionListener(teamEditListener);
		
		dialogBottom.add(addSelectedTeam);
		dialogBottom.add(newTeam);
		
		dialog.add(dialogBottom, BorderLayout.SOUTH);
		dialog.add(searchTeam, BorderLayout.NORTH);
		dialog.add(masterScrollPane, BorderLayout.CENTER);
		dialog.setSize(350, this.getHeight());
		
		//filter list as you type
		searchTeam.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void changedUpdate(DocumentEvent arg0) {
			}
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				refreshMasterList();
			}
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				refreshMasterList();
			}
		});
		
		
		pwStatus.setPreferredSize(pwStatus.getSize());
		consoleField.addKeyListener(new KeyAdapter(){
			public void keyReleased(KeyEvent e){
				switch (e.getKeyCode()) {
				case KeyEvent.VK_ENTER:
					handleCommand(consoleField.getText(), null);
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
	private void refreshTeamList(){
		teamList.setListData(Server.theServer.teams);
	}
	
	/**
	 * This uses Java 1.8 code to sort. NOTE: this could be converted to 1.6 if we decide we need to for portability.
	 */
	private void refreshMasterList(){
		Vector<Team> v = new Vector<Team>(Team.masterList.values());
		v.removeAll(Server.theServer.teams);
		String filter = searchTeam.getText();
		if(!filter.isEmpty()){
			while(!filter.isEmpty() && Character.isWhitespace(filter.charAt(0))) filter = filter.substring(1);
			if(Character.isDigit(filter.charAt(0))){
				v.removeIf(a -> !Integer.toString(a.number).startsWith(searchTeam.getText()));
			} else{
				v.removeIf(a -> !a.name.toLowerCase().startsWith(searchTeam.getText().toLowerCase()));
			}
		}
		Collections.sort(v);
		masterList.setListData(v);
	}
	
	private void refreshFormStatus(){
		this.hardwareLabel.setText(Resources.getFileStatus(HW_FORM_FILE));
		this.softwareLabel.setText(Resources.getFileStatus(SW_FORM_FILE));
		this.fieldLabel.setText(Resources.getFileStatus(FD_FORM_FILE));
	}
	/**
	 * This updates the area at the bottom right of the console page that shows the traffic states and the cookies issued
	 */
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
		
		//TODO need to do something if any of these throw an exception?
		try {
			loadInspectionForm(HW_FORM_FILE, Server.hardwareForm);
		} catch (FileNotFoundException e1) {
			
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, "Unable to load Hardware Inspection form!");
		}
		try {
			loadInspectionForm(SW_FORM_FILE, Server.softwareForm);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, "Unable to load Software Inspection form!");
		}

		try {
			loadInspectionForm(FD_FORM_FILE, Server.fieldForm);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, "Unable to load Field Inspection form!");
		}

		try {
			Resources.loadTeamList();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		try {
			scan = Resources.getScanner("events.dat");
			while(scan.hasNextLine()){
				events.add(scan.nextLine());
			}
			Server.theServer.loadConfig();
			//FIXME address this
//			if(!events.contains(Server.event)){
//				if(events.size() > 0) Server.event = events.get(0);
//				else Server.event=null;
//			}
			System.out.println(Server.event);
			Server.theServer.loadEvent(Server.event);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if(scan != null) scan.close();

	}
	
	public static void loadInspectionForm(String srcFile, InspectionForm target) throws FileNotFoundException{
		target.cbTotal = 0;
		target.rows.clear();
		target.widestRow = 0;
		Scanner scan = Resources.getScanner(srcFile);	//The first line is the delimiter
		String delimiter = scan.nextLine();
		target.setDelimiter(delimiter);
		while(scan.hasNextLine()){
			try{
				String line=scan.nextLine();
				line=line.replaceAll("<","&lt;");
				line=line.replaceAll(">","&gt;");
				target.addRow(line);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		scan.close();

	}
	
	/**
	 * This handles the command that was given by the String specified by who.
	 * Who should be null only for use by the system console.
	 * @param command The command to process
	 * @param who The person who sent the command (or null for the System)
	 */
	public void handleCommand(String command, String who){
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
		 *KILL/STOP
		 * 
		 * 
		 */
		append(command, who);
		boolean success=false;//return to not show success
		String[] args=command.split(" ");
		if(args.length>0){
			args[0]=args[0].toUpperCase();

			if(args[0].equals("LIST")){
				if(args.length>1){
					args[1]=args[1].toUpperCase();
					if(args[1].equals("EVENTS")){
						for(String e:events){
							append(e, who);
						}
						return;
					}
					else if(args[1].equals("TEAMS")){
						if(args.length>2 && args[2].toUpperCase().equals("-A")){
							//list all nc teams in order
							Integer[] copy=Team.masterList.keySet().toArray(new Integer[1]);
							Arrays.sort(copy);
							for(Integer num:copy){
								append(Team.getSaveString(num), who);
							}
							return;
						}
						else{
							for(Team t:Server.theServer.teams){
								append(t.number+": "+t.name, who);
							}
							return;
						}
					}
					else if(args[1].equals("STATUS")){						

						try{
							int num=Integer.parseInt(args[2]);
							Team t=Server.theServer.getTeam(num);
							if(t==null){
								if(Team.masterList.containsKey(num))append(num+" "+Team.masterList.get(num)+" is not in this event.", who);
								else append("Unrecognized team #: "+num, who);
								return;
							}
							append(t.number+": "+t.name, who);
							append((t.checkedIn?"":"NOT ")+"Checked In", who);
							append((t.cube==Server.PASS?"":"NOT")+"passed sizing cube", who);
							append((t.hardware==Server.PASS?"":"NOT")+"passed hardware", who);
							append((t.software==Server.PASS?"":"NOT")+"passed software", who);
							append((t.field==Server.PASS?"":"NOT")+"passed field", who);
							return;
						}
						catch(Exception e){
							append("USAGE: LIST STATUS &lt;number&gt;", who);
							return;
						}
					}
					else{
						append("USAGE: LIST [EVENTS | TEAMS | STATUS]", who);
						return;
					}
				}else{
					append("USAGE: LIST [EVENTS | TEAMS | STATUS] ", who);
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
							append("USAGE: CHANGE [EVENT] &lt;code&gt;", who);
							return;
						}
					}
				}
				else{
					append("USAGE: CHANGE [EVENT] &lt;code&gt;", who);
					return;
				}
			}
			else if(args[0].equals("ADD")){
				if(args.length>1){
					if(args[1].toUpperCase().equals("TEAM")){
						try{
							int num=Integer.parseInt(args[2]);
							if(Server.theServer.getTeam(num)!=null){
								append("Team "+num+" already in event", who);
								return;
							}
							
							if(!Team.doesTeamExist(num)){
								Team.registerTeam(num, null);
								append("Use \"SET TEAMNAME "+num +" <NAME>\" to set team name.", who);
								Resources.saveTeamList();
							}
							success = Server.theServer.teams.add(Team.getTeam(num));							
							Collections.sort(Server.theServer.teams);
							success &= Resources.saveEventFile();
							Server.save();
						}catch(Exception e){
							append("FAILED: USAGE: ADD TEAM &lt;number&gt;", who);
							return;
						}
					}
					if(args[1].toUpperCase().equals("EVENT")){ 
						if(args.length>3){
							String name="";
							if(args.length>3)name=args[3];
							for(int i = 4; i<args.length; i++){
								name += " " + args[i];
							}
							if(Resources.createEventFile(args[2],name)){ 
								events.add(args[2]);
								success=Resources.saveEventsList();
							}
						}else{
							append("USAGE: ADD [EVENT] &lt;code&gt; &lt;name&gt;", who);
							return;
						}
					}
				}else{
					append("USAGE: ADD [TEAM | EVENT] &lt;number | code> &lt;name&gt;", who);//also least team #s for event
					return;
				}
			}
			else if(args[0].equals("REMOVE")){
				if(args.length>2 && args[1].toUpperCase().equals("TEAM")){
					try{
						Team t=Server.theServer.getTeam(Integer.parseInt(args[2]));
						System.out.println(t);
						if(t==null){
							append("Team was not in event", who);
							return;
						}else{
							success = Server.theServer.teams.remove(t);
							success &= Resources.saveEventFile();//save removal
						}
					}catch(Exception e){
						append("USAGE: REMOVE TEAM &lt;number&gt;", who);
						return;
					}
				}
				else{
					append("USAGE: REMOVE TEAM &lt;number&gt;", who);
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
								Server.theServer.getTeam(num).setStatus(type, stat);
								success=true;
							}catch(Exception e){
								//not numbe status
								try {
									int stat=Server.class.getDeclaredField(args[4]).getInt(null);
									Server.theServer.getTeam(num).setStatus(type, stat);
									success=true;
								} catch (Exception e1) {
									e1.printStackTrace();
									success=false;
								} 
							}
						}
						else{
							append("USAGE: SET STATUS &lt;number&gt; &lt;type&gt; &lt;status&gt;", who);
							append("&lt;type= CI | SC | HW | SW | FD&gt;", who);
							append("&lt;status= 0 | 1 | 2 | 3 | NO_DATA | FAIL | PROGRESS | PASS&gt;", who);
							return;
						}
					}
					else if(args[1].equals("AUTOSAVE")){
						try{
							autoSave=Long.parseLong(args[2])*1000;
							success=true;
						}catch(Exception e){
							append("USAGE: SET AUTOSAVE &lt;time (s)&gt;", who);
							return;
						}
					}
					else if(args[1].equals("TEAMNAME")){
						try{
							int number=Integer.parseInt(args[2]);
							Team t=Server.theServer.getTeam(number);
							if(t==null){
								append("Team "+number+" not found.", who);
								return;
							}
							String name = args[3];
							t.setName(name);
							Team.setTeamName(number, name);
							Resources.saveTeamList();
						}catch(Exception e){
							append("USAGE: SET TEAMNAME &lt;number&gt; &lt;name&gt;", who);
							return;
						}
					}
				}
				else{
					append("USAGE: SET [STATUS | PASSWORD | ROOT | AUTOSAVE | TEAMNAME | EVENT [NAME | CODE] ] &lt;value&gt;", who);
					return;
				}
			}
			else if(args[0].equals("CLEAR")){
				//TODO warning
				if(args.length>1){
					args[1]=args[1].toUpperCase();
					if(args[1].equals("DATA")){
						success=Server.clearData();
					} else
					if(args[1].equals("CONSOLE")){
						consoleTextArea.clear();
						return;
					} else
					if (args[1].equals("LOGS")) {
						Resources.deleteDirectory("log", "log");
						append("Old logs cleared, current session logs untouched", who);
						return;
					} else
					if (args[1].equals("COOKIES")) {
						Server.theServer.refreshPassword();
						append("Cookies cleared, all users unauthenticated", who);
						return;
					}
					else{
						append("USAGE: CLEAR [CONSOLE | COOKIES | DATA | LOGS]", who);
						return;
					}
				}else{
					append("USAGE: CLEAR [CONSOLE | DATA]", who);
					return;
				}
			}
			else if(args[0].equals("SAVE")){
				success=Server.save();
			}
			else if(args[0].equals("IP")){
				try {
					append("Server IP: " + InetAddress.getLocalHost().getHostAddress(), who);
				} catch (UnknownHostException e) {
					
				}
				return;
			}
			else if(args[0].equals("HELP")){
				append("Available commands: (Attempt use for more help)", who);
				append("\tLIST [EVENTS | TEAMS | STATUS]", who);
				append("\tADD [TEAM | EVENT]", who);
				append("\tREMOVE TEAM", who);
				append("\tSET [...]", who);
				append("\tCHANGE EVENT", who);
				append("\tSELECT EVENT", who);
				append("\tIP", who);
				append("\tSAVE", who);
				return;
			}
			else if (args[0].equals("KILL") || args[0].equals("STOP")) {
				kill(); 
			}
			else if(args[0].equals("UNLOAD")){
				Server.theServer.unloadEvent();
			}
			else{
				error("UNKNOW COMMAND: "+args[0], who);
				return;
			}
			append((success?"SUCCESS":"FAILED"), who);
		}
	}

	private String[] colors = new String[] {"#FF7F00", "#9400D3", "#00FF00", "#FF00FF", "#0000FF", "33A8FF"};
	public static final String ERROR_STRING = "ERROR";
	public static final String SERVER_STRING = "Server";
	/**
	 * Used to append info to the console
	 * This formats the string that is given, adding a timestamp and stylizing the who sent it option.
	 * If who is null then it sends it as the server, which will be gray, otherwise each person is assigned a color based on the name.
	 * If the name starts with ERROR then the ERROR will be removed from the name and it will be stylized red. 
	 * This is so that the system can display error responses to any user.
	 * @param s The string to append to the console
	 * @param who The user that posted this string (or the user this is a response to)
	 */
	public void append(String s, String who) {
		s = fixHTML(s);
		String color = who == null ? "#888888" : (who.startsWith(ERROR_STRING) ? "#FF0000" : colors[Math.abs(who.hashCode() - 1) % colors.length]);
		String timeWho = "<font color=\"" + color + "\" face=\"lucida console\">[" + TIME_FORMAT.format(Calendar.getInstance().getTime()) + " - " + (who == null ? SERVER_STRING : who.replaceAll(ERROR_STRING, "")) + "] </font>";
		consoleTextArea.append(timeWho + "<font color=\"#ffffff\" face=\"lucida console\">" + s + "</font><br>");
	}
	/**
	 * This is the same as append, but it stylizes it as an ERROR. See append for more info
	 * @param s The string to append to the console as an error
	 * @param who The user this error is a response to
	 */
	public void error(String s, String who) {
		append(s, ERROR_STRING + (who == null ? SERVER_STRING : who));
//		s = fixHTML(s);
//		consoleTextArea.append("<font color=\"#ff0000\">" + DATE_FORMAT.format(Calendar.getInstance().getTime()) + "</font><font color=\"#ff0000\" face=\"lucida console\">" + s + "</font><br>"); 
	}
	/**
	 * This replaces the line breaks and tabs in text with html versions
	 * @param s The string to fix
	 * @return The fixed string
	 */
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
		multiHardware.setSelected(Server.multiHardware);
		multiSoftware.setSelected(Server.multiSoftware);
		multiField.setSelected(Server.multiField);
	}
	
	public boolean changeEvent(String event){
		boolean b=Server.changeEvent(event);
		setCheckBoxes();
		return b;
	}
	
	

}
