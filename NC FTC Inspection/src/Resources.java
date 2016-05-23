import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

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

	public static boolean createEventFile(String code, String name, String teams) {
		File f=new File(root);
		if(!f.exists() || ! f.isDirectory())f.mkdirs();
		f=new File(root+"/"+code+".event");
		try {
			f.createNewFile();
			PrintWriter pw=new PrintWriter(f);
			pw.println(name);
			pw.print(teams);
			pw.flush();
			pw.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
	}

	public static boolean saveEventsList() {
		File f=new File(root);
		if(!f.exists() || ! f.isDirectory())f.mkdirs();
		f=new File(root+"/events.dat");
		if(!f.exists()){
			try {
				f.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
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

}
