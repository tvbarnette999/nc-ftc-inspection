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
		try{
			in=Resources.class.getResourceAsStream("Resources/"+name);
			if(in==null)throw new Exception("First failed");
		}catch(Exception e){
			try{
				in=new FileInputStream(root+"/"+name);
			}catch(Exception e1){
				try{
					in=new FileInputStream("Resources/"+name);//this should not work outside ecplise for anything.
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
