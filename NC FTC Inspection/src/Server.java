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
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
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
	
	public static final int LOGIN = 1;
	public static final int HARDWARE=2;
	public static final int SOFTWARE=3;
	public static final int FIELD=4;
	public static final int CUBE=5;
	public static final int CHECKIN=6;
	public static final int HOME=7;
	public static final int LOG=11;
	
	//These parameters are set to determine whther a given event will show status for that stage and do paperless inspection.
	public static boolean trackCheckin=true;
	/**Cube is still shown in status if HW is Full
	 * Set this to true only if cube is separate?
	 * */
	//TODO change name to separateCube?
	public static boolean trackCube=true;
	public static boolean trackHardware=true;
	public static boolean fullHardware=true;
	public static boolean trackSoftware=true;
	public static boolean fullSoftware=true;
	public static boolean trackField=true;
	public static boolean fullField=true;
	
	private boolean done=false;
	
	public static Vector<String> HWForm=new Vector<String>();
	public static Vector<String> SWForm=new Vector<String>();
	public static Vector<String> FDForm=new Vector<String>();
	

	public static final long SEED = System.currentTimeMillis();
	public static final String password="hello123";//"NCftc2016";

	public static String event="BCRI2017";
	public static String fullEventName;

	private static ExecutorService threadPool;

	Vector<Team> teams=new Vector<Team>();
	
	static Vector<String> statusLog=new Vector<String>();

	public static String cookieHeader="FTC_COOKIE=\"";

	public static Server theServer=new Server();
	
	private Server(){		//Singleton
	}
	
	private byte[] hashedPass;
	private String hashedPassString;
	
	private int cookieCount;
	private int traffic;
	
	public void setPassword(String password) {
		cookieCount = 0;
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
	public int getTraffic() {
		int temp = traffic;
		traffic = 0;
		return temp;
	}
	public int getCookieCount() {
		return cookieCount;
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
	public void sendPage(Socket sock,int i, String extras, boolean verified, Object ... other) throws IOException{
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
		
		if (extras == null){ 
			extras = "";
		}
		pw.println(extras);
		//TODO make constants for this, or an enum? Also replace all instances of the hardcoded #s with whichever we go with
		switch(i){
			case 0:sendStatusPage(pw);break;
			case 1:sendPage(pw,"Resources/inspectorLogin.php");break;
			case HARDWARE: 
				if(other.length>0)sendFullInspectionPage(pw,i,other[0].toString());
				else if(fullHardware)sendInspectionTeamSelect(pw,i);
				else sendInspectionEditPage(pw,i);
				break;
			case SOFTWARE:
				if(other.length>0)sendFullInspectionPage(pw,i,other[0].toString());
				else if(fullSoftware)sendInspectionTeamSelect(pw,i);
				else sendInspectionEditPage(pw,i);
				break;
			case FIELD:
				if(other.length>0)sendFullInspectionPage(pw,i,other[0].toString());
				else if(fullField)sendInspectionTeamSelect(pw,i);
				else sendInspectionEditPage(pw,i);
				break;
			case CUBE:
			case CHECKIN:sendInspectionEditPage(pw,i);break;
			case HOME:
				sendHomePage(pw);
				break;
			case LOG:sendLogPage(pw);break;
			
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
		traffic++;
	}

	/**
	 * This method handles GET requests. Any pages that require passwords are NOT requested by GETs, so if they are, direct them to password page.
	 * @param req
	 * @param sock
	 * @param fullReq 
	 * @throws IOException
	 */
	public void get(String req,Socket sock, String fullReq) throws IOException{
		String other=null;
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
		if(req.length() == 0)pageID = 0; //just localhost, show status page
		if(req.equals("hardware"))pageID=verified?HARDWARE:LOGIN;
		if(req.equals("software"))pageID=verified?SOFTWARE:LOGIN;
		if(req.equals("field"))pageID=verified?FIELD:LOGIN;
		if(req.equals("cube"))pageID=verified?CUBE:LOGIN;
		if(req.equals("checkin"))pageID=verified?CHECKIN:LOGIN;
		if(req.equals("home"))pageID=verified?HOME:LOGIN;
		if(req.equals("log"))pageID=verified?LOG:LOGIN;
		
		if(req.startsWith("hardware/") && fullHardware){
			pageID=verified?HARDWARE:LOGIN;
			other=req.substring(req.indexOf("/")+1);			
		}
		if(req.startsWith("software/") && fullSoftware){
			pageID=verified?SOFTWARE:LOGIN;
			other=req.substring(req.indexOf("/")+1);			
		}
		if(req.startsWith("field/") && fullField){
			pageID=verified?FIELD:LOGIN;
			other=req.substring(req.indexOf("/")+1);			
		}
		//these do not require login
		if(req.equals("favicon.ico"))pageID=100;
		if(req.equals("manual1"))pageID=98;
		if(req.equals("manual2"))pageID=99;

		if (req.equals("firstfavicon.png")) pageID = -1;
		if(other!=null)sendPage(sock,pageID,null,verified,other);
		else sendPage(sock,pageID,null,verified);

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
				cookieCount++;
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
				extras = generateExtrasPopup("Incorrect Password");
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
				getTeam(t).set(type,Integer.parseInt(v));
				//TODO send http status 204
			}
			///fullupdate?team=5064_HW1&value=true HTTP/1.1
			else if(req.startsWith("fullupdate?")){//full inspection state change
				String s=req.substring(req.indexOf("=")+1);
				System.out.println(s);
				int t=Integer.parseInt(s.substring(0, s.indexOf("_")));
				String type=s.substring(s.indexOf("_")+1,s.indexOf("&"));
				int index=Integer.parseInt(type.substring(2));//type is 2 characters
				type=type.substring(0, 2);
				String v=s.substring(s.indexOf("=")+1);
				v=v.substring(0, v.indexOf(" "));
				System.out.println(t+":"+type+":"+v);
				getTeam(t).set(type,index,Boolean.parseBoolean(v));
				//TODO send http status 204
			}
			else if(req.startsWith("note?")){
				String s=req.substring(req.indexOf("=")+1);
				System.out.println(s);
				int t=Integer.parseInt(s.substring(0, s.indexOf("_")));
				String type=s.substring(s.indexOf("_")+1,s.indexOf(" "));
				System.out.println(t+":"+type);
				String note=data.substring(0, data.indexOf("&&&"));
				getTeam(t).setNote(type,note);
			}
			pageID=1;
		}
		System.out.println(pageID);
		sendPage(sock,pageID, extras, valid);	
	}
	/**
	 * Use extras = generateExtrasPopup(popup) to render a java script pop up on the page
	 * @param popup The string to render
	 * @return The extra to show the pop up
	 */
	public String generateExtrasPopup(String popup) {
		return "\n\n<script> window.alert(\"" + popup + "\") </script>";
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
			//FIXME if 2 clients request manual within ~sec of each other is problem! might want to buffer into ram once. also syncrhonize
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
		//TODO do we want to have an overall inspection progress bar across the top? like its 100% when every team is fully through, etc..
		//TODO only show tracked ones (only hide cube if cube is untracked and hw is not full)
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
	public Team getTeam(int num){
		for(int i=0;i<teams.size();i++){
			if(teams.get(i).number==num)return teams.get(i);
		}
		return null;
	}
	
	public void sendInspectionTeamSelect(PrintWriter pw, int i){
		String type="";
		switch(i){
			case HARDWARE:type="hardware";break;
			case SOFTWARE:type="software";break;
			case FIELD: type="field";break;
			default:return;//TODO something else here?
		}
		pw.println("<html><body><table><tr><th>Team #</th><th>Link</th></tr>");
		for(Team t:teams){
			pw.println("<tr><td bgcolor="+getColor(t.get(i))+">"+t.number+"</td><td><a href=\"/"+type+"/"+t.number+"\">Inspect</a></td></tr>");
		}
		pw.println("</table></body></html>");
		pw.flush();
	}
	
	public void sendFullInspectionPage(PrintWriter pw, int i, String extras){
		//TODO have notes page at bottom, and submit button
		//when submit button clicked, send note and thats how you know IP->fail (or pass)
		//note beng reason for failure as prescribed 
		//TODO do we want to be able to print an inspection sheet for a team if they ask? IE print job? -Nah scoring pc can just connect and print webpage
		Team team=null;
		try{
			team=getTeam(Integer.parseInt(extras));
			if(team==null) throw new IllegalArgumentException("Invalid team #: "+extras);
		}catch(Exception e){
			//TODO send error page. 404? some better way to do this?
			return;
		}
		
		Vector<String> form;
		System.out.println("full: "+i);
		String type="";
		String note="";
		String head="Appendix ";
		switch(i){
			case HARDWARE: form=HWForm; type="_HW"; note=team.hwNote;break;
			case SOFTWARE: form=SWForm; type="_SW"; note= team.swNote;break;
			case FIELD: form=FDForm; type="_FD";note=team.fdNote;break;
			default: throw new IllegalArgumentException("Full inspection not supported");
		}
		if(type.contains("W")) head+="A: Robot Inspection Checklist";
		else head+="B: Field Inspection Checklist";
		pw.println("<html><head><h2>"+head+"</h2><hr style=\"border: 3px solid #943634\" /><h3>Team Number: "+extras+"</h3></head>");
		pw.println("<body><table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">");
		pw.println("<tr bgcolor=\"#E6B222\" ><th>Insp.</th><th>Inspection Rule</th><th>Rule #</th></tr>");
		
		int j=0;
		/*
		 * TODO improve js for pass and fail buttons:
		 * 
		 * pass: check all boxes are checked.
		 *       popup for "signature"? like NobleHour did?
		 * fail: dont need to check (could fail for safety)
		 * both: send comments 
		 *       send status update (pass only when signed)-DONE(no sign for pass yet)
		 *       
		 * remove auto check for pass when all checked? (forces signature) -DONE
		 * 
		 * TODO: signature: add username to login page so it autofills inspector's signature?
		 */
		for(String s:form){
			pw.print("<tr><td><label>");
			pw.println("<input type=\"checkbox\" name=\""+extras+type+j+"\" "+(team.get(i,j)?"checked=\"checked\"":"")+" onclick=\"update()\"/>");
			pw.println("</label></td><td>"+s+"</td></tr>");
			j++;
		}
		pw.println("</table><br><b>General Comments or Reasons for Failure:</b><br><textarea name="+extras+type+" id=\"note\" rows=\"4\" cols=\"100\">"+note+"</textarea>");
		pw.println("<br><br><button type=\"button\" name=\""+extras+type+"\" onclick=\"fullpass()\">Pass</button>&nbsp;&nbsp;&nbsp;");
		pw.println("<button type=\"button\" name=\""+extras+type+"\" onclick=\"fullfail()\">Fail</button>");
		
		pw.println("<script>");
		try {
			sendPage(pw,"Resources/fullUpdate.js");
		} catch (IOException e) {
			e.printStackTrace();
		}
		pw.println("</script></body></html>");
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

	public void sendLogPage(PrintWriter pw){
		pw.println("<html><body>");
		for(String s:statusLog){
			pw.println(s+"<br>");
		}
		pw.println("</body></html>");
		pw.flush();
	}
	@SuppressWarnings("unchecked")
	public void startServer(final int port) throws FileNotFoundException{
		this.setPassword(password); 
		loadEvent();
		threadPool=Executors.newCachedThreadPool();
		try {
			ServerSocket server=new ServerSocket(port);
			server.setSoTimeout(1000);
			addLogEntry("Starting server...");
			while(!done){
				try{
					threadPool.execute(new Handler(server.accept()));
				}catch(SocketTimeoutException e){
					//this is so we can safely shutdown}
				}
			}
			server.close();
			addLogEntry("ServerSocket closed");
		} catch (IOException e) {
			e.printStackTrace();
		}
		addLogEntry("Attempting to shutdown client threads...");
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		threadPool.shutdownNow();
		
	}

	public void loadEvent() throws FileNotFoundException{
		Scanner scan=new Scanner(new File("Resources/"+event));
		fullEventName=scan.nextLine();
		String[] nums=scan.nextLine().split(",");
		scan.close();
		for(String s:nums){
			teams.add(new Team(Integer.parseInt(s)));
		}
		addLogEntry("Loaded team data");
		Collections.sort(teams);
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
	//				System.err.println("******** RENDERING POST **********");
					String[] datarray=req.split("\n");
	//				System.err.println(Arrays.toString(datarray));
					String data=datarray[datarray.length-1];
	//				System.err.println(data); // this is correct
					data=data.substring(data.indexOf("\n")+3);//why is this here?
	//				System.err.println(data);
					post(req.substring(5,req.indexOf("\n")),data,sock);
				}
				sock.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static void addLogEntry(String s){
		String time=new SimpleDateFormat ("[hh:mm:ss] ").format(Calendar.getInstance().getTime());
		statusLog.add(time+s);
		Main.me.consoleTextArea.append("\n"+time+s);
		//TODO fire event to update GUI
		System.out.println(time+s);
	}
	
	/**
	 * Stops the server
	 */
	public static void stopServer(){
		//TODO save things!
		addLogEntry("Stopping server!");
		theServer.done=true;
	}
}
