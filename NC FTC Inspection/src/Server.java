import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
	public static final String RED="\"#FF0000\"";
	public static final String GREEN="\"#00FF00\"";
	public static final String CYAN="\"#00FFFF\"";
	public static final String WHITE="\"#FFFFFF\"";

	public static final int NO_DATA=0;
	public static final int PASS=3;
	public static final int FAIL=1;
	public static final int PROGRESS=2;

	public static final int CHECKIN=0;
	public static final int CUBE=1;
	public static final int HARDWARE=2;
	public static final int SOFTWARE=3;
	public static final int FIELD=4;


	public static final String password="hello123";//"NCftc2016";

	public static final String event="BCRI2017";

	private static ExecutorService threadPool;


	public static HashMap<Integer,String> teamData=new HashMap<Integer,String>();

	Vector<Team> teams=new Vector<Team>();
	static Vector<String> statusLog=new Vector<String>();

	public static String cookie="";//Each time run, generate a new cookie. That secure enough?


	//TODO Monitoring GUI- allow editing what teams are there
	/*
	 * TODO Decide how events are structured:
	 * 
	 * Premake team list for each event 
	 * or select teams during setup?
	 * 
	 * teamdata.dat will have team # and name for each NC team (can make from data on FIRST's website)
	 * add team location?
	 * 
	 * 
	 * Also, lets avoid any items above java 1.6 incase this ends up running on linux (a Pi for example)
	 */
	public static void main(String[] args) throws MalformedURLException, IOException {

		Server w=new Server();
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
			w.startServer();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	public static class Team implements Comparable{
		int number;
		String name;
		/*
		 * TODO make it so that each event can configure which stages are tracked
		 * If an event doesnt track Check-in with this system.
		 * Or if Cube and HW are combined.
		 * etc.
		 * 
		 * NOT HW and SW. If they are combined, this program can determine based off electronic inspection
		 *  form if they fail both or just one.(OR inspector can just switch betwee the 2)
		 * 
		 * Then, decide whether we want to just issue a PASS to all teams in that category, or not display it (PASS is easier)
		 */
		boolean checkedIn=true;
		int hardware;
		int software=PASS;
		int cube=PASS;
		int field;
		boolean ready;
		public Team(int n){
			number=n;
			name=teamData.get(number);
		}

		/**
		 * Returns the Team's status for the given level of Inspection
		 * @param i
		 * @return
		 */
		public int get(int i){
			switch(i){
				case CHECKIN:return checkedIn?PASS:0;
				case CUBE:return cube;
				case HARDWARE:return hardware;
				case SOFTWARE:return software;
				case FIELD:return field;
			}
			return 0;
		}

		/**Sets the Team's status for the given level of Inspection
		 * 
		 * @param type
		 * @param i
		 */
		public void set(String type, int i) {
			if(type.equals("CI"))this.checkedIn=i==3?true:false;
			if(type.equals("SC"))this.cube=i;
			if(type.equals("HW"))this.hardware=i;
			if(type.equals("SW"))this.software=i;
			if(type.equals("FD"))this.field=i;
			System.out.println("set "+this.number+" "+type+":"+i);			
			statusLog.add("[TIME]: "+this.number+" "+type+" set to "+i);//TODO make this useful ie 1533 has PASSED hardware
			if(this.checkedIn&&this.cube==PASS&&this.hardware==PASS&&this.software==PASS&&this.field==PASS){
				ready=true;
			}
		}
		@Override
		public int compareTo(Object o) {
			if(o instanceof Team){
				return number-((Team)o).number;
			}
			return 0;
		}
	}
	public void sendPage(Socket sock,int i) throws IOException{
		OutputStream out=sock.getOutputStream();
		PrintWriter pw=new PrintWriter(out);
		if(i>=100){
			pw.println("HTTP/1.1 200 OK");
			pw.println("Content-Type: image/x-icon");
			//FIXME Sending favicon doesnt work- but problem probably isnt here.
			//			return;
		}
		else if(i>90){
			//\nContent-Transfer-Encoding: binary
			pw.println("HTTP/1.1 200 OK");
			pw.println("Content-Type: application/pdf");
			pw.println("Content-Disposition: inline; filename=manual1.pdf");

		}
		else pw.print("HTTP/1.1 200 OK\nContent-Type: text/html\n\n");
		//TODO make constants for this, or an enum? Also replace all instances of the hardcoded #s with whichever we go with
		switch(i){
			case 0:sendStatusPage(pw);break;
			case 1:sendPage(pw,"Resources/inspectorLogin.html");break;
			case 2:sendInspectionEditPage(pw,HARDWARE);break;
			case 3:sendInspectionEditPage(pw,SOFTWARE);break;
			case 4:sendInspectionEditPage(pw,FIELD);break;
			case 5:sendInspectionEditPage(pw,CUBE);break;
			//TODO add forums. add truncated manual sections? ie Robot Rules section, etc?
			case 98:sendDocument(pw,out,"Resources/manual1.pdf");break;
			case 99:sendDocument(pw,out,"Resources/manual2.pdf");break;
			case 100:sendDocument(pw,out,"Resources/firstfavicon.ico");break;
			case -1:
				sendDocument(pw, out, "Resources/firstfavicon.png");
				break;
			default://404
		}
		pw.println("\n");// <html>Hello, <br>Chrome!<a href=\"/p2html\">Visit W3Schools.com!</a></html>\n");
		pw.flush();
		pw.close();
		//TODO send 404
	}

	/**
	 * This method handles GET requests. Any pages that require passwords are NOT requested by GETs, so if they are, direct them to password page.
	 * @param req
	 * @param sock
	 * @throws IOException
	 */
	public void get(String req,Socket sock) throws IOException{

		req=req.substring(1,req.indexOf(" "));
		System.out.println(req);
		int pageID=0;
		//These all require login, so send to login page
		if(req.equals("inspector"))pageID=1;	
		if(req.equals("software"))pageID=1;
		if(req.equals("field"))pageID=1;

		//these do not require login
		if(req.equals("favicon.ico"))pageID=100;
		if(req.equals("manual1"))pageID=98;
		if(req.equals("manual2"))pageID=99;

		if (req.equals("firstfavicon.png")) pageID = -1;
		sendPage(sock,pageID);

	}

	/**
	 * This method handles POST requests. It should be passes the request, the data line, and the Socket.
	 * Any pages requiring passwords are requested through POST, so are handles here.
	 * @param req
	 * @param data
	 * @param sock
	 * @throws IOException
	 */
	public void post(String req, String data,Socket sock) throws IOException{
		int pageID=0;
		boolean valid=false;
		System.out.println("POST: \n"+req+"\nData:\n"+data);
		/*
		 * if the data contains a password, its from the login page.
		 * That means we can send it a secured page.
		 */
		if(data.contains("password")){
			String pass=data.substring(data.indexOf("password")+9);
			pass=pass.substring(0, pass.indexOf("&"));
			if(pass.equals(password)){
				valid=true;
				System.out.println("VERIFIED");
				req=req.substring(req.indexOf("/")+1, req.indexOf(" "));
				if(req.equals("inspector"))pageID=2;
				if(req.equals("field"))pageID=4;

			}
			//else, no password, pageID stays 0 (the status page)
			//TODO incorrect password page?
		}
		/*If there is no password, then the POST's source is a page that already required authentication,
		 * therefore, we can handle it appropriately
		 */
		else{
			//HANDLE POST FROM VERIFIED INSPECTOR
			req=req.substring(1);
			System.out.println("VERIFIED "+req);
			if(req.startsWith("inspection?")||req.startsWith("field?")){//These are requests that contain a state change for a team for a level of inspection. rn only these 2.
				String s=req.substring(req.indexOf("=")+1);
				System.out.println(s);
				int t=Integer.parseInt(s.substring(0, s.indexOf("_")));
				String type=s.substring(s.indexOf("_")+1,s.indexOf("&"));
				String v=s.substring(s.indexOf("=")+1);
				v=v.substring(0, v.indexOf(" "));
				System.out.println(t+":"+type+":"+v);
				for(int i=0;i<teams.size();i++){
					if(teams.get(i).number==t){
						teams.get(i).set(type,Integer.parseInt(v));
						System.out.println(teams.get(i).number+","+teams.get(i).hardware);
					}
				}
			}
			pageID=1;
		}

		sendPage(sock,pageID);	
	}

	/**
	 * This is for sending an html webpage that is completely contained within the file. (No real-time generation by this server)
	 * @param pw
	 * @param f
	 * @throws IOException
	 */
	public void sendPage(PrintWriter pw,String f) throws IOException{
		Scanner s=new Scanner(new File(f));
		while(s.hasNextLine())pw.println(s.nextLine());
		s.close();
	}

	/**
	 * This is for sending more complex pages that are contained within a file. These are pdfs,images,etc.
	 * 
	 * @param pw
	 * @param out
	 * @param f
	 * @throws IOException
	 */
	public void sendDocument(PrintWriter pw,OutputStream out,String f) throws IOException{
		//if(f.substring(f.lastIndexOf(".")+1).equals("pdf")){
		try{
			System.out.println("Sending: "+f);
			FileInputStream fin=new FileInputStream(f);
			int q=0;
			ByteArrayOutputStream bout=new ByteArrayOutputStream();
			while((q=fin.read())!=-1){
				bout.write(q);
			}
			fin.close();
			if (f.endsWith(".pdf")) {
				pw.println("Content-Length:"+bout.size());
				pw.println("\n");;
				pw.flush();
			}
			bout.writeTo(out);
			bout.flush();
			bout.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		//}

	}
	public String getColor(int i){
		switch(i){
			case 0:return WHITE;
			case 1:return RED;
			case 2:return CYAN;
			case 3:return GREEN;
		}
		return "black";
	}
	public String getColor(boolean b){
		return getColor(b?3:0);
	}
	public void sendStatusPage(PrintWriter pw){
		pw.println("<html><meta http-equiv=\"refresh\" content=\"15\"><table border=\"3\"><tr>");
		pw.println("<th>CI</th><th>SC</th><th>HW</th><th>SW</th><th>FD</th><th>Team #</th><th>Team name</th></tr>");
		for(Team t:teams){
			pw.print("<tr><td bgcolor="+getColor(t.checkedIn)+"></td>"+
					"<td bgcolor="+getColor(t.cube)+"></td>"+
					"<td bgcolor="+getColor(t.hardware)+"></td>"+
					"<td bgcolor="+getColor(t.software)+"></td>"+
					"<td bgcolor="+getColor(t.field)+"></td>"+
					"<td bgcolor="+getColor(t.ready)+">"+t.number+"</td>"+
					"<td bgcolor="+getColor(t.ready)+">"+t.name+"</td>");
			pw.println("</tr>");
		}
//		pw.println("<img src=\"firstfavicon.png\"></html>");
	}
	public void sendInspectionEditPage(PrintWriter pw, int i) throws IOException{
		System.out.println("ITS ME");
		String type="";
		switch(i){
			case 0:type="_CI";break;
			case 1:type="_SC";break;
			case 2:type="_HW";break;
			case 3:type="_SW";break;
			case 4:type="_FD";break;
		}
		pw.println("<html><body><table>");
		for(Team t:teams){
			pw.println("<tr><td>"+t.number+"</td><td>");
			pw.println("<td><label><input type=\"radio\" name=\""+t.number+type+"\" value=\""+PASS+"\" "+(t.get(i)==PASS?"checked=\"checked\"":"")+" onclick=\"update()\"/>Pass</label></td>");
			pw.println("<td><label><input type=\"radio\" name=\""+t.number+type+"\" value=\""+FAIL+"\" "+(t.get(i)==FAIL?"checked=\"checked\"":"")+" onclick=\"update()\"/>Fail</label></td>");
			pw.println("<td><label><input type=\"radio\" name=\""+t.number+type+"\" value=\""+PROGRESS+"\" "+(t.get(i)==PROGRESS?"checked=\"checked\"":"")+" onclick=\"update()\"/>In Progress</label></td>");
			pw.println("<td><label><input type=\"radio\" name=\""+t.number+type+"\" value=\""+NO_DATA+"\" "+(t.get(i)==NO_DATA?"checked=\"checked\"":"")+" onclick=\"update()\"/>Uninspected</label></td>");
			pw.println("</tr>");
		}
		pw.println("</table><script>");
		try {
			switch(i){
				case 0:type="_CI";break;
				case 1:type="_SC";break;
				case 2:sendPage(pw,"Resources/inspectionUpdate.js");;break;
				case 3:type="_SW";break;
				case 4:sendPage(pw,"Resources/fieldUpdate.js");break;
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pw.println("</script></body></html>");
	}


	public void startServer() throws FileNotFoundException{

		Scanner scan=new Scanner(new File("Resources/"+event));
		String[] nums=scan.nextLine().split(",");
		scan.close();
		for(String s:nums){
			teams.add(new Team(Integer.parseInt(s)));
		}
		Collections.sort(teams);
		threadPool=Executors.newCachedThreadPool();
		try {
			ServerSocket server=new ServerSocket(80);
			//TODO once we have a GUI, closing that will shutdown server and break this while loop.
			while(true){
				threadPool.execute(new Handler(server.accept()));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		threadPool.shutdown();
		try {
			threadPool.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		threadPool.shutdownNow();
	}


	/**
	 * Handles the HTTP requests and directs them to appropriate methods.
	 * 
	 *
	 */
	public class Handler implements Runnable{
		Socket sock;
		public Handler(Socket s){
			sock=s;
		}
		public void run(){
			try{
				byte[] b=new byte[1024];
				sock.getInputStream().read(b);
				String req=new String(b);
				//System.out.println(req);

				//				req=req.substring(0,req.indexOf("\n"));

				if(req.startsWith("GET")){
					//	System.out.println(req);
					get(req.substring(4,req.indexOf("\n")),sock);
				}
				if(req.startsWith("POST")){					
					String[] datarray=req.split("\n");
					String data=datarray[datarray.length-1];
					data=data.substring(data.indexOf("\n")+3);					
					System.out.println(data);
					post(req.substring(5,req.indexOf("\n")),data,sock);
				}
				sock.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

}
