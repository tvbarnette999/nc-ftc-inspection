import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Scanner;

public class Resources {
	public static String root;
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

}
