package nc.ftc.inspection.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.JComboBox;
import javax.swing.JTextArea;

import nc.ftc.inspection.FormEditor;
import nc.ftc.inspection.Main;
import nc.ftc.inspection.Server;
import nc.ftc.inspection.Team;
import nc.ftc.inspection.FormEditor.RowEdit;
import nc.ftc.inspection.InspectionForm.CB_LEVEL;

/**A class with static methods for accessing Resources. The root directory is specified at runtime or in configuration, and 
 * is where the data is saved. It defaults to "NC Inspection" adjacent to .jar. It is the first place checked for any 
 * resource needed. This means if a "built in" resource needs to be updated locally, it can be by putting it there.
 * The second place we check for a resource is within the Resources folder inside the jar. If we cannot find it at 
 * that point, a FileNotFoundException is thrown. Most of these methods
 * return a Stream or Scanner or PrintWriter to the respective resource, since
 * it is possible the resource is in a jar. See the Resources manifest file for details on each file: resource-manifest.html
 * <br>
 * Resources Directories:
 * <br>
 * <pre>
 * 1. root/	
 *          .config              
 *          events.dat            
 *          [any modified default file]
 *          [eventName].event    
 *          [eventName]/         
 *                [eventName].status 
 *                hw/  			 
 *                      [team#].ins  
 *                sw/ 			 
 *                      [team#].ins  
 *                fd/ 			 
 *                      [team#].ins  
 * 		
 * 2.<.jar>/Resources
 * 			pdfs, html, php, js
 * 			default event data
 *</pre>
 * 
 * 
 */



public class Resources {
	public static final String DEFAULT = "Default";
	public static final String CUSTOM = "Custom";
	public static final String UNKNOWN = "UNKNOWN";
	
	public static final String HW_FORM_FILE = "hwform.dat";
	public static final String SW_FORM_FILE = "swform.dat";
	public static final String FD_FORM_FILE = "fdform.dat";
	
	static HashMap<String, String> fileStatus = new HashMap<String, String>();
	/**The root save directory that is checked first. Default value: "NC Inspection" */
	public static String root="NC Inspection";
	public static String backup = null;//"F://";//"backup"; //TODO Selection of backup drive (preferable USB)
	/**
	 * Returns a Scanner object for the given resource.
	 * @param name the Resource to access
	 * @return the Scanner for the resource
	 * @throws FileNotFoundException if the file is not found
	 */
	public static Scanner getScanner(String name) throws FileNotFoundException{
		return new Scanner(getInputStream(name), "UTF-8");
	}
	public static boolean exists(String name) {
		if (name == null || name.length() == 0 || name.endsWith(".dat") || name.endsWith(".event")) {
			return false;
		}
		try {
			if (getInputStream(name) != null) {
				return true;
			}
		} catch (FileNotFoundException e) {
			//we will return false bc it isnt available
		}
		return false;
	}
	
	private static void copyDirectory(File src, File dest, CopyOption opt) throws IOException{
		for(File f : src.listFiles()){
			if(f.isDirectory()){
				File target = new File(dest + "/" + f.getName());
				if(!target.exists() || !target.isDirectory())target.mkdir();
				copyDirectory(f, target, opt);
			} else{
				Files.copy(f.toPath(), dest.toPath().resolve(f.getName()), opt);
			}
		}
	}
	public static void backup(){
//		backup = "F://";
		if(backup == null)return;
		try {
			File back = new File(backup + "/NC Inspection");
			if(!back.exists() || !back.isDirectory()){
				back.mkdirs();
			}
			copyDirectory(new File(root), back, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("SUCCES in backing up to "+back.getAbsolutePath());
		} catch (IOException e) {
			Server.addErrorEntry("BACKUP FAILED");
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns an InputStream for the given resource.
	 * @param name the name of the resource
	 * @param forceDefault Throw exception if not the default (in .jar) one
	 * @return the InputStream
	 * @throws FileNotFoundException if file cannot be found
	 */
	@SuppressWarnings("resource")
	public static InputStream getInputStream(String name, boolean forceDefault) throws FileNotFoundException{
		InputStream in;
		/*try root save directory first- if we need to change a file on the fly it loads that one first 
		(also any mod like add team- rewrite new event data file there and its saved)
		*/
		try{
			if(forceDefault)throw new Exception("Forcing Default");
			in = new FileInputStream(root + "/" + name);
			fileStatus.put(name, CUSTOM);
		}catch(Exception e){
			try{		
				in = Resources.class.getResourceAsStream("/Resources/" + name);
				if(in == null){
					if(forceDefault) throw new FileNotFoundException("Unable to load default!");
					throw new FileNotFoundException("Resource " + name + " not in save root");
				}
				fileStatus.put(name, DEFAULT);
			}catch(FileNotFoundException e1){
				System.err.println("Unable to load Resource: " + name);
				throw new FileNotFoundException("Could not find resource: " + name);
			}
		}
		return in;
	}
	
	/**
	 * Returns an InputStream for the given resource.
	 * @param name the name of the resource
	 * @return the InputStream
	 * @throws FileNotFoundException if file cannot be found
	 */
	public static InputStream getInputStream(String name) throws FileNotFoundException{
		return getInputStream(name, false);
	}
	
	
	public static PrintWriter getWriter(String file) throws IOException{
		File f = new File(root + "/" + file);
		if(!f.exists()){
			f.getParentFile().mkdirs();
			f.createNewFile();
		}
		PrintWriter pw = new PrintWriter(f, "UTF-8");
		return pw;
	}
	
	public static PrintStream getPrintStream(String file) throws IOException{
		File f = new File(root + "/" + file);
		if(!f.exists()){
			f.getParentFile().mkdirs();
			f.createNewFile();
		}
		PrintStream ps = new PrintStream(f, "UTF-8");
		return ps;
	}

	/**
	 * Creates a data file for the given event name. It is located in the root directory. Use this to create a new event.
	 * @param code The event code/abbreviation
	 * @param name The full event name.
	 * @return true if successful
	 */
	public static boolean createEventFile(String code, String fullName) {
		checkRoot();
		File f=new File(root+"/"+code+".event");
		try {
			f.createNewFile();
			PrintWriter pw=new PrintWriter(f);
			pw.println(fullName);
			pw.print("");//no teams
			pw.flush();
			pw.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	/**
	 * This deletes all files in a given directory matching a given file extension. This is currently only used for deleting the log files
	 * @param dir
	 * @param ext
	 */
	public static void deleteDirectory(String dir, String ext) {
		File fDir = new File(root + "/" + dir);
		for (File f:fDir.listFiles()) {
			if (f.getName().endsWith(ext)) {
				f.delete();
			}
		}
	}
	
	/**
	 * Saves the current list of events.
	 * @return true is successful
	 */
	public static boolean saveEventsList() {
		checkRoot();
		File f=new File(root+"/events.dat");
		if(!f.exists()){
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		try{
			PrintWriter pw=new PrintWriter(f);
			for(String e:Main.events){
				pw.println(e);
			}
			pw.flush();
			pw.close();
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * Returns true if the root directory exists
	 * @return
	 */
	public static boolean rootExists(){
		File f = new File(root);
		return f.exists() && f.isDirectory();
	}
	
	/**
	 * Checks that the root directory exists.
	 */
	private static void checkRoot(){
		File f=new File(root);
		if(!f.exists() || !f.isDirectory())f.mkdirs();
	}
	
	/**
	 * Saves the event data to the event data file in the root directory. [eventName].event
	 * @return true if successful
	 */
	public static boolean saveEventFile() {
		checkRoot();
		File f = new File(root+"/"+Server.event+".event");
		if(!f.exists()){
			try{
				f.createNewFile();
			}catch(Exception e){
				e.printStackTrace();
				return false;
			}
		}
		try{
			PrintWriter pw=new PrintWriter(f);
			pw.println(Server.fullEventName);
			String s = "";
			for(Team t:Server.theServer.teams){
				s += t.number+",";
			}
			if(s.length() > 0)s = s.substring(0,s.length()-1);
			pw.print(s);//remove last comma
			pw.flush();
			pw.close();
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}	
		
		return false;
	}
	
	/**
	 * Returns a PrintWriter for the file for status data of the current event
	 * @return PrintWriter for status file.
	 */
	public static PrintWriter getStatusWriter() {
		File f=new File(root+"/"+Server.event);
		if(!f.exists() || !f.isDirectory())f.mkdirs();
		
		f=new File(root+"/"+Server.event+"/"+Server.event+".status");
		if(!f.exists()){
			try{
				f.createNewFile();				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		try{
			PrintWriter pw=new PrintWriter(f);
			return pw;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns a Scanner to read the current event status file.
	 * @return Scanner for status file.
	 */
	public static Scanner getStatusScanner(){
		File f=new File(root+"/"+Server.event+"/"+Server.event+".status");
		if(!f.exists())return null;
		try{
			return new Scanner(f);
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a PrintWriter for the hardware inspection file for the given team.
	 * @param team the team number
	 * @return PrintWriter to write inspection status.
	 */
	public static PrintWriter getHardwareWriter(int team) {
		File f=new File(root+"/"+Server.event+"/HW");
		if(!f.exists() || !f.isDirectory())f.mkdirs();
		
		f=new File(f.getPath()+"/"+team+".ins");
		if(!f.exists()){
			try{
				f.createNewFile();
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		try{
			PrintWriter pw=new PrintWriter(f);
			return pw;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns a Scanner to read hardware inspection file for the given team.
	 * @param team the team number
	 * @return Scanner to read hardware status.
	 */
	public static Scanner getHardwareScanner(int team){
		File f=new File(root+"/"+Server.event+"/HW/"+team+".ins");
		if(!f.exists())return null;
		try{
			return new Scanner(f);
		}
		catch(Exception e){
			return null;
		}
	}

	/**
	 * Returns a PrintWriter for the software inspection file for the given team.
	 * @param team the team number
	 * @return PrintWriter to write inspection status.
	 */
	public static PrintWriter getSoftwareWriter(int team) {
		File f=new File(root+"/"+Server.event+"/SW");
		if(!f.exists() || !f.isDirectory())f.mkdirs();
		
		f=new File(f.getPath()+"/"+team+".ins");
		if(!f.exists()){
			try{
				f.createNewFile();
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		try{
			PrintWriter pw=new PrintWriter(f);
			return pw;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns a Scanner to read software inspection file for the given team.
	 * @param team the team number
	 * @return Scanner to read software status.
	 */
	public static Scanner getSoftwareScanner(int team){
		File f=new File(root+"/"+Server.event+"/SW/"+team+".ins");
		if(!f.exists())return null;
		try{
			return new Scanner(f);
		}
		catch(Exception e){
			return null;
		}
	}
	
	/**
	 * Returns a PrintWriter for the field inspection file for the given team.
	 * @param team the team number
	 * @return PrintWriter to write inspection status.
	 */
	public static PrintWriter getFieldWriter(int team) {
		File f=new File(root+"/"+Server.event+"/FD");
		if(!f.exists() || !f.isDirectory())f.mkdirs();
		
		f=new File(f.getPath()+"/"+team+".ins");
		if(!f.exists()){
			try{
				f.createNewFile();
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		try{
			PrintWriter pw=new PrintWriter(f);
			return pw;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns a Scanner to read field inspection file for the given team.
	 * @param team the team number
	 * @return Scanner to read field status.
	 */
	public static Scanner getFieldScanner(int team){
		File f=new File(root+"/"+Server.event+"/FD/"+team+".ins");
		if(!f.exists())return null;
		try{
			return new Scanner(f);
		}
		catch(Exception e){
			return null;
		}
	}
	
	/**
	 * Returns a PrintWriter to write to the Server Configuration File.
	 * @return the PrintWriter
	 */
	public static PrintWriter getConfigWriter(){
		File f=new File(root);
		if(!f.exists() || !f.isDirectory())f.mkdirs();
		
		f=new File(f.getPath()+"/server.config");
		if(!f.exists()){
			try{
				f.createNewFile();				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		try{
			PrintWriter pw=new PrintWriter(f);
			return pw;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns a Scanner to read the Server COnfiguration file.
	 * @return the Scanner
	 */
	public static Scanner getConfigScanner(){
		File f=new File(root+"/"+"server.config");
		if(!f.exists())return null;
		try{
			return new Scanner(f);
		}
		catch(Exception e){
			return null;
		}
	}
	
	public static boolean saveTeamList(){
		checkRoot();
		File f = new File(root+"/teamdata.dat");
		if(!f.exists()){
			try{
				f.createNewFile();
			}catch(Exception e){
				e.printStackTrace();
				return false;
			}
		}
		try{
			PrintWriter pw = new PrintWriter(f);
			ArrayList<Integer> nums = new ArrayList<Integer>(Team.masterList.keySet());
			Collections.sort(nums);
			for(int num : nums){
				pw.println(Team.getSaveString(num));
			}
			pw.flush();
			pw.close();
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}	
		
		return false;
	}
	
	public static void loadTeamList() throws IOException{
		
		checkRoot();
		Scanner scan = getScanner("teamdata.dat");
	
		while(scan.hasNextLine()){
			try{
				String line=scan.nextLine();
				//System.out.println(line);
				int num=Integer.parseInt(line.substring(0, line.indexOf(":")));
				String name=line.substring(line.indexOf(":")+1);
				Team.registerTeam(num, name);
			}catch(Exception e){
				e.printStackTrace();
			}
		}	
		
		if(scan!=null)scan.close();
	}
	
	public static String getFileStatus(String file){
		String res = fileStatus.get(file);
		if(res == null) res = UNKNOWN;
		return res;
	}
	
	/**
	 * Adds _1 to the end of the given filename, and increments until that file does not exist
	 * @param orig
	 * @return
	 */
	public static String getBackup(String file){
		String name = file.substring(0, file.lastIndexOf('.'));
		String ext = file.substring(file.lastIndexOf('.'));
		int i = 1;
		System.out.println(root + "/" + name + "_" + i + ext);
		while(new File(root + "/" + name + "_" + i + ext).exists())i++;
		return name + "_" + i + ext;
	}

	/**
	 * Saves the form stored in the GUI of the given FormEditor
	 * @param form
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static void saveForm(FormEditor form) throws IOException {
		String file = "";
		switch(form.form.type){
			case Server.HARDWARE: file = "hwform.dat"; break;
			case Server.SOFTWARE: file = "swform.dat"; break;
			case Server.FIELD:    file = "fdform.dat"; break;
		}
		PrintWriter pw = getWriter(file);
		pw.println(form.newDelimiter);
		pw.println("I::color=" + form.form.color);
		if(form.form.header != null) pw.println("I::header=" + form.form.header);
		if(form.form.cubeIndex > -1)pw.println("I::cube_index=" + form.form.cubeIndex);
		System.out.println(form.list.size());
		for(RowEdit edit : form.list){
			if(edit.header){
				pw.print("H" + form.newDelimiter);
				pw.print((edit.left.getComponentCount() - 1) + form.newDelimiter);
				for(int i = 0; i < edit.left.getComponentCount() - 1; i ++){
					pw.print(((JTextArea)edit.left.getComponent(i)).getText().replaceAll("\n", "<br>") + form.newDelimiter);
				}
			} else{
				pw.print((edit.left.getComponentCount() - 1) + form.newDelimiter);
				for(int i = 0; i < edit.left.getComponentCount() - 1; i++){
					pw.print(((CB_LEVEL)((JComboBox<CB_LEVEL>)edit.left.getComponent(i)).getSelectedItem()).value + form.newDelimiter);
				}
			}

			pw.print(edit.explain.getText().replaceAll("\n", "<br>") + form.newDelimiter);
			pw.println(edit.rule.getText().replaceAll("\n", "<br>"));
		}

		pw.flush();
		pw.close();	
		
		//TODO test this: Should update size of team's bool [] when form changes
		switch(form.form.type){
			case Server.HARDWARE:
				for(Team t : Team.masterList.values()){
					t.hwData = Arrays.copyOf(t.hwData, form.form.cbTotal);
				} 
				break;
			case Server.SOFTWARE: 
				for(Team t : Team.masterList.values()){
					t.swData = Arrays.copyOf(t.swData, form.form.cbTotal);
				} 
				break;
			case Server.FIELD:
				for(Team t : Team.masterList.values()){
					t.fdData = Arrays.copyOf(t.fdData, form.form.cbTotal);
				} 
				break;
		}
		
	}

	/**
	 * Renames a local resource (not stored in the jar)
	 * @param file
	 * @param backup
	 * @throws IOException
	 */
	public static void renameResource(String file, String backup) throws IOException {
		Files.move(new File(root + "/" + file).toPath(), new File(root + "/" + backup).toPath(), StandardCopyOption.REPLACE_EXISTING);		
	}

	public static boolean backupExists() {
		if(backup == null)return false;
		return new File(backup).exists() && new File(backup).isDirectory();
	}
}
