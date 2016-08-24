package nc.ftc.inspection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

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
	/**The root save directory that is checked first. Default value: "NC Inspection" */
	public static String root="NC Inspection";
	/**
	 * Returns a Scanner object for the given resource.
	 * @param name the Resource to access
	 * @return the Scanner for the resource
	 * @throws FileNotFoundException if the file is not found
	 */
	public static Scanner getScanner(String name) throws FileNotFoundException{
		return new Scanner(getInputStream(name));
	}
	
	/**
	 * Returns an InputStream for the given resource.
	 * @param name the name of the resource
	 * @return the InputStream
	 * @throws FileNotFoundException if file cannot be found
	 */
	@SuppressWarnings("resource")
	public static InputStream getInputStream(String name) throws FileNotFoundException{
		InputStream in;
		/*try root save directory first- if we need to change a file on the fly it loads that one first 
		(also any mod like add team- rewrite new event data file there and its saved)
		*/
		try{
			in=new FileInputStream(root+"/"+name);
		}catch(Exception e){
			try{		
				in=Resources.class.getResourceAsStream("/Resources/"+name);
				if(in==null)throw new FileNotFoundException("Resource " + name + " not in save root");
			}catch(FileNotFoundException e1){
				System.err.println("Unable to load Resource: "+name);
				throw new FileNotFoundException("Could not find resource: "+name);
			}
		}
		return in;
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
			pw.print(s.substring(0,s.length()-1));//remove last comma
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
			PrintWriter pw=new PrintWriter(f);
			ArrayList<Integer> nums=new ArrayList<Integer>(Main.teamData.keySet());
			Collections.sort(nums);
			for(int num:nums){
				pw.println(num+":"+Main.teamData.get(num));
			}
			pw.flush();
			pw.close();
			return true;
		}catch(IOException e){
			e.printStackTrace();
		}	
		
		return false;
	}
}
