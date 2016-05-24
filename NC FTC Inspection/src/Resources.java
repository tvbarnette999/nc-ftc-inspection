import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

/*
 * Resources Directories:
 * 
 * 1st Check:
 * 		<.jar>/Resources
 * 			pdfs, html, php, js
 * 			default event data
 * 2nd Check:
 * 		root/
 * 			events.dat (if modified)
 * 			<event>.event (if modified or created)
 * 			/<event> (nothing in this dir is ever in jar)
 * 				<event>.status 
 * 				/hw
 * 				/sw
 * 				/fd
 * 					<team#>.ins
 * 3rd check:
 * 		/Resources (should only work in eclipse or if exported with Resources in folder beside jar)
 * 					(save data will not go here unless root is set to "/Resources")* 
 * 
 * root defaults to NC Inspection folder beside jar
 * 
 * 
 */


public class Resources {
	public static String root="NC Inspection";
	/**
	 * Returns a Scanner object for the given resource.
	 * @param name
	 * @return
	 * @throws FileNotFoundException 
	 */
	public static Scanner getScanner(String name) throws FileNotFoundException{
		return new Scanner(getInputStream(name));
	}
	
	public static InputStream getInputStream(String name) throws FileNotFoundException{
		InputStream in;
		try{//try root save directory first- if we need to change a file on the fly it loads that one first (also any mod like add team- rewrite new event data file there and its saved)
			in=new FileInputStream(root+"/"+name);
		}catch(Exception e){
			try{				
				in=Resources.class.getResourceAsStream("Resources/"+name);
				if(in==null)throw new Exception("First failed");
			}catch(Exception e1){
				try{
					in=new FileInputStream("Resources/"+name);//this should not work outside eclipse for anything.
				}catch(Exception e2){
					System.err.println("Unable to load Resource: "+name);
					throw new FileNotFoundException("Could not find resource: "+name);
					//TODO check other exceptions to see if something worse happened?
				}
			}
		}
		return in;
	}

	public static boolean createEventFile(String code, String name) {
		checkRoot();
		File f=new File(root+"/"+code+".event");
		try {
			f.createNewFile();
			PrintWriter pw=new PrintWriter(f);
			pw.println(name);
			pw.print("");//no teams
			pw.flush();
			pw.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	

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
	private static void checkRoot(){
		File f=new File(root);
		if(!f.exists() || ! f.isDirectory())f.mkdirs();
	}
	public static boolean saveEventFile() {
		checkRoot();
		File f=new File(root+"/"+Server.event+".event");
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
			String s="";
			for(Team t:Server.theServer.teams){
				s+=t.number+",";
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
}
