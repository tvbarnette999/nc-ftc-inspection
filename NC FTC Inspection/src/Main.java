import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class Main {

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
		
	public static void main(String[] args) {
		
		
		try {
			Scanner scan=new Scanner(new File("Resources/teamdata.dat"));
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
			
			scan=new Scanner(new File("Resources/hwform.dat"));
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
			
			scan=new Scanner(new File("Resources/swform.dat"));
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
			
			scan=new Scanner(new File("Resources/fdform.dat"));
			while(scan.hasNextLine()){
				try{
					String line=scan.nextLine();
					line=line.replaceAll(":", "</td><td>");
					Server.FDForm.add(line);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			
			Server.theServer.startServer(80);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
