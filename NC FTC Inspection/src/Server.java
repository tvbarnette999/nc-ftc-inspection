import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
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
	
	public static final int HARDWARE=2;
	public static final int SOFTWARE=3;
	public static final int FIELD=4;
	public static final int CUBE=5;
	public static final int CHECKIN=6;
	public static final int HOME=7;
	
	//These parameters are set to determine whther a given event will show status for that stage and do paperless inspection.
	public static boolean trackCheckin=true;
	public static boolean trackCube=true;
	public static boolean trackHardware=true;
	public static boolean fullHardware=true;
	public static boolean trackSoftware=true;
	public static boolean fullSoftware=true;
	public static boolean trackField=true;
	public static boolean fullField=true;
	
	public static Vector<String> HWForm=new Vector<String>();
	public static Vector<String> SWForm=new Vector<String>();
	public static Vector<String> FDForm=new Vector<String>();
	

	public static final long SEED = System.currentTimeMillis();
	public static final String password="hello123";//"NCftc2016";

	public static final String event="BCRI2017";

	private static ExecutorService threadPool;

	Vector<Team> teams=new Vector<Team>();
	
	static Vector<String> statusLog=new Vector<String>();

	public static String cookieHeader="FTC_COOKIE=\"";

	public static Server theServer=new Server();
	
	private Server(){		//Singleton
	}
	
	private byte[] hashedPass;
	private String hashedPassString;
	
	public void setPassword(String password) {
		SecureRandom rand = new SecureRandom();
		ByteBuffer buff = ByteBuffer.allocate(Long.BYTES + password.getBytes().length);
		buff.putLong(SEED);
		buff.put(password.getBytes());
		rand.setSeed(buff.array());
		hashedPass = new byte[32];
		rand.nextBytes(hashedPass);
		hashedPassString = "";
		for (Byte b : hashedPass)
			hashedPassString += (char) (((int)'a') + Math.abs(b) / 12);
	}
	public boolean checkPassword(String password) {
		System.out.println("Check Password: " + password);
		SecureRandom rand = new SecureRandom();
		ByteBuffer buff = ByteBuffer.allocate(Long.BYTES + password.getBytes().length);
		buff.putLong(SEED);
		buff.put(password.getBytes());
		rand.setSeed(buff.array());
		byte[] checkPass = new byte[hashedPass.length];
		rand.nextBytes(checkPass);
		System.out.println(Arrays.toString(hashedPass));
		System.out.println(Arrays.toString(checkPass));
		System.out.println(Arrays.toString(hashedPass));
		System.out.println(Arrays.toString(checkPass));
		for (int i = 0; i < checkPass.length; i++)
			if (checkPass[i] != hashedPass[i])
				return false;
		return true;
	}
	
	public boolean checkHash(String checkPass) {
		System.out.println(checkPass);
		System.out.println(hashedPassString);
		return hashedPassString.equals(checkPass);
	}
	/**
	 * 
	 * @param sock
	 * @param i
	 * @param extras
	 * @param verified Boolean for if the request is from a logged in user
	 * @throws IOException
	 */
	public void sendPage(Socket sock,int i, String extras, boolean verified) throws IOException{
		OutputStream out=sock.getOutputStream();
		PrintWriter pw=new PrintWriter(out);
		if(i>=100){
			pw.println("HTTP/1.1 200 OK");
			pw.println("Content-Type: image/x-icon");
		}
		else if(i>90){
			//\nContent-Transfer-Encoding: binary
			pw.println("HTTP/1.1 200 OK");
			pw.println("Content-Type: application/pdf");
			pw.println("Content-Disposition: inline; filename=manual1.pdf");

		}
		else pw.print("HTTP/1.1 200 OK\nContent-Type: text/html\n");
		if (extras == null) 
			extras = "";
		pw.println(extras);
		//TODO make constants for this, or an enum? Also replace all instances of the hardcoded #s with whichever we go with
		switch(i){
			case 0:sendStatusPage(pw);break;
			case 1:sendPage(pw,"Resources/inspectorLogin.php");break;
			case HARDWARE:sendFullInspectionPage(pw,i,1);break;
			case SOFTWARE:
			case FIELD:
			case CUBE:
			case CHECKIN:sendInspectionEditPage(pw,i);break;
			case HOME:
				sendHomePage(pw);
				break;
				
			
			//TODO add forums. add truncated manual sections? ie Robot Rules section, etc?
			case 98:sendDocument(pw,out,"Resources/manual1.pdf");break;
			case 99:sendDocument(pw,out,"Resources/manual2.pdf");break;
			case 100:sendDocument(pw,out,"Resources/firstfavicon.ico");break;
			case -1:
				sendDocument(pw, out, "Resources/firstfavicon.png");
				break;
			default:
				//404
				pw.write("Error 404: Showing default<br><br>\n\n");
				if (verified) {
					sendHomePage(pw);
				} else {
					sendStatusPage(pw);
				}
		}
		pw.println("\n");// <html>Hello, <br>Chrome!<a href=\"/p2html\">Visit W3Schools.com!</a></html>\n");
		pw.flush();
		pw.close();
	}

	/**
	 * This method handles GET requests. Any pages that require passwords are NOT requested by GETs, so if they are, direct them to password page.
	 * @param req
	 * @param sock
	 * @param fullReq 
	 * @throws IOException
	 */
	public void get(String req,Socket sock, String fullReq) throws IOException{
		String check = fullReq;
		boolean verified = false;
		try {
			check = check.substring(check.indexOf(cookieHeader) + cookieHeader.length());
			check = check.substring(0, check.indexOf('\"')); // also take off [ ]
			verified = checkHash(check);
		} catch (Exception e) {
			verified = false;
			e.printStackTrace();
			//we dont have the password
		}
		System.err.println("VERIFIED" + verified);
		req=req.substring(1,req.indexOf(" "));
		System.out.println(req);
		int pageID=Integer.MIN_VALUE; //default case
		//These all require login, so send to login page
		if(req.length() == 0)pageID = 0;
		if(req.equals("hardware"))pageID=verified?HARDWARE:1;	
		if(req.equals("software"))pageID=verified?SOFTWARE:1;
		if(req.equals("field"))pageID=verified?FIELD:1;
		if(req.equals("cube"))pageID=verified?CUBE:1;
		if(req.equals("checkin"))pageID=verified?CHECKIN:1;
		if(req.equals("home"))pageID=verified?HOME:1;
		
		//these do not require login
		if(req.equals("favicon.ico"))pageID=100;
		if(req.equals("manual1"))pageID=98;
		if(req.equals("manual2"))pageID=99;

		if (req.equals("firstfavicon.png")) pageID = -1;
		sendPage(sock,pageID,null,verified);

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
		String extras = "";
		System.out.println("POST: \n"+req+"\nData:\n"+data);
		/*
		 * if the data contains a password, its from the login page.
		 * That means we can send it a secured page.
		 */
		if(data.contains("password")){
			String pass=data.substring(data.indexOf("password")+9);
			pass=pass.substring(0, pass.indexOf("&"));
			if(checkPassword(pass)){
//				OutputStream out=sock.getOutputStream();
//				PrintWriter pw=new PrintWriter(out);
				extras = "Set-Cookie: " + cookieHeader + hashedPassString + "\"\n";
//				pw.print("HTTP/1.1 200 OK\nContent-Type: text/html\nSet-Cookie: " + cookieHeader + hashedPassString + "\"\n\n    \n");
//				pw.flush();
				valid=true;
				System.out.println("VERIFIED PASSWORD");
				req=req.substring(req.indexOf("/")+1, req.indexOf(" "));
				System.out.println("REQ:"+req);
				if(req.equals("hardware"))pageID=HARDWARE;
				if(req.equals("field"))pageID=FIELD;
				if(req.equals("home"))pageID=HOME;
				if(req.equals("software"))pageID=SOFTWARE;
				if(req.equals("cube"))pageID=CUBE;
				if(req.equals("checkin"))pageID=CHECKIN;
				
			} else {
				pageID = 1;
				extras = "\n\n<script> window.alert(\"Incorrect Password\") </script>";
			}
			//else, no password, pageID stays 0 (the status page)
		}
		/*If there is no password, then the POST's source is a page that already required authentication,
		 * therefore, we can handle it appropriately
		 */
		else{
			//HANDLE POST FROM VERIFIED INSPECTOR
			req=req.substring(1);
			System.out.println("VERIFIED "+req);
			if(req.startsWith("update?")){//"inspection?")){//||req.startsWith("field?")){//These are requests that contain a state change for a team for a level of inspection. rn only these 2.
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
					}
				}
			}
			pageID=1;
		}
		System.out.println(pageID);
		sendPage(sock,pageID, extras, valid);	
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
		
		/*
		 * TODO Check if doing a full inspection for that type
		 * 				-if so, send list of teams and button to inspect them, which loads /hardware/#####
		 *              -if not, check if tracking
		 *              		-if so, send old page
		 *              		-if not, return to home?
		 *              
		 *              
		 */
		
		String type="";
		switch(i){
			case CHECKIN:type="_CI";break;
			case CUBE:type="_SC";break;
			case HARDWARE:type="_HW";break;
			case SOFTWARE:type="_SW";break;
			case FIELD:type="_FD";break;
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
//			switch(i){
//				case 0:type="_CI";break;
//				case 1:type="_SC";break;
//				case 2:sendPage(pw,"Resources/inspectionUpdate.js");;break;
//				case 3:type="_SW";break;
//				case 4:sendPage(pw,"Resources/fieldUpdate.js");break;
//			}
			sendPage(pw,"Resources/update.js");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pw.println("</script></body></html>");
	}

	public void sendFullInspectionPage(PrintWriter pw, int i, int team){
		Vector<String> form;
		System.out.println("full: "+i);
		switch(i){
			case HARDWARE: form=HWForm;break;
			case SOFTWARE: form=SWForm;break;
			case FIELD: form=FDForm;break;
			default: throw new IllegalArgumentException("Full inspection not supported");
		}
		pw.println("<html><head>Team: "+team+"</head><body><table><tr><th>Inspector</th><th>Inspection Rule</th><th>Rule #</th></tr>");
		for(String s:form){
			pw.print("<tr><td><input type=\"checkbox\"/></td><td>"+s+"</td></tr>");
		}
		pw.println("</table></body></html>");
		pw.flush();
	}
	
	
	public void sendHomePage(PrintWriter pw){
		//TODO make this page better
		pw.println("<html>\n<body>");
		if(trackCheckin)pw.println("<a href=\"/checkin\">Checkin</a>");
		/*TODO if cube separate, do this, or if tracking cube and !fullhardware
		 * Also, if cube separate, dont update from hardware POST
		 * TODO move or copy this todo where relevant
		 */
		if(trackCube)pw.println("<a href=\"/cube\">Sizing Cube</a>");
		if(trackHardware || fullHardware)pw.println("<a href=\"/hardware\">Hardware</a>");
		if(trackSoftware || fullSoftware)pw.println("<a href=\"/software\">Software</a>");
		if(trackField || fullField)pw.println("<a href=\"/field\">Field</a>");
		pw.println("<a href=\"/\">Status</a>");//TODO do we want to have a /status page that has the links at the top? (diff from /)
		pw.println("</body></html>");
		pw.flush();
	}

	public void startServer(final int port) throws FileNotFoundException{
		this.setPassword(password); //TODO GUI for prompt
		Scanner scan=new Scanner(new File("Resources/"+event));
		String[] nums=scan.nextLine().split(",");
		scan.close();
		for(String s:nums){
			teams.add(new Team(Integer.parseInt(s)));
		}
		Collections.sort(teams);
		threadPool=Executors.newCachedThreadPool();
		try {
			ServerSocket server=new ServerSocket(port);
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
				System.err.println(req);
				if(req.startsWith("GET")){
					//	System.out.println(req);
					get(req.substring(4,req.indexOf("\n")),sock, req);
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
