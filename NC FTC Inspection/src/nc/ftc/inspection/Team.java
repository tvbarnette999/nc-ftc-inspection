package nc.ftc.inspection;

import java.util.HashMap;

public class Team implements Comparable<Team> {
		int number;
		String name;
		
		/**
		 * The master list of all registered teams.
		 */
		public static HashMap<Integer, Team> masterList = new HashMap<Integer, Team>();
		
		/**The index of the cube boolean in the fullHW boolean[]*/
		public static int CUBE_INDEX=1;//2 in old form
		
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
		boolean checkedIn=false;
		int hardware;
		int software;
		int cube;
		int field;
		boolean ready;
		String hwNote="",hwTeamSig="",hwInspSig="";
		String swNote="",swTeamSig="",swInspSig="";
		String fdNote="",fdTeamSig="",fdInspSig="";
		
		//arrays for inspection
		boolean[] hwData;
		boolean[] swData;
		boolean[] fdData;
		private Team(int n, String name){
			number=n;
//			name=Main.teamData.get(number);
			//TODO move the arrays elsewhere for memory? (teams not in event dont need arrays in RAM until added to event)
			this.name = name;
			hwData = new boolean[Server.hardwareForm.cbTotal];
			swData = new boolean[Server.softwareForm.cbTotal];
			fdData = new boolean[Server.fieldForm.cbTotal];

		}
		
		/**
		 * Adds the given team to the master list
		 * @param number
		 * @param name
		 */
		public static void registerTeam(int number, String name){
			if(masterList.containsKey(number)){
				throw new RuntimeException("Double Teams!");
			}
			masterList.put(number, new Team(number, name));
		}
		
		public static Team getTeam(int number){
			return masterList.get(number);
		}
		
		public static boolean doesTeamExist(int number){
			return masterList.containsKey(number);
		}


		public static Team loadDataFromString(String data){
			String[] dat=data.split(",");
			try{
				Team t=getTeam(Integer.parseInt(dat[0]));
				t.checkedIn=Boolean.parseBoolean(dat[1]);
				t.cube=Integer.parseInt(dat[2]);
				t.hardware=Integer.parseInt(dat[3]);
				t.software=Integer.parseInt(dat[4]);
				t.field=Integer.parseInt(dat[5]);
				return t;
			}catch(Exception e){
				e.printStackTrace();
			}
			return null;
		}
		
		public static void changeNumber(Team t, int newNum){
			if(doesTeamExist(newNum)){
				throw new IllegalArgumentException("Can't renumber! Team "+newNum+" already exists!" );
			}
			masterList.remove(t.number);
			t.number = newNum;
			masterList.put(newNum, t);
		}
		
		public static void setTeamName(int number, String newName){
			if(doesTeamExist(number)){
				masterList.get(number).name = newName;
			}
		}
		
		public static String getSaveString(int num){
			return num + ":" + masterList.get(num).name;
		}
		/**
		 * Returns the Team's status for the given level of Inspection
		 * @param i
		 * @return
		 */
		public int getStatus(int i){
			switch(i){
				case Server.CHECKIN:return checkedIn || Server.trackCheckIn?Server.PASS:0;
				case Server.CUBE:return Server.trackCube ? cube : Server.PASS;
				case Server.HARDWARE:return Server.trackHardware ? hardware : Server.PASS;
				case Server.SOFTWARE:return Server.trackSoftware ? software : Server.PASS;
				case Server.FIELD:return Server.trackField ? field : Server.PASS;
			}
			return 0;
		}
		/**
		 * Returns true if this team has passed the given level of inspection
		 * @param i
		 * @return
		 */
		public boolean passed(int i){
			return !Server.track(i) || getStatus(i) == Server.PASS;
		}
		/**
		 * Returns the Team's status for the inspection element at the specified index for the specified type.
		 * @param type
		 * @param index
		 * @return
		 */
		public boolean getStatus(int type, int index){
			switch(type){
				case Server.HARDWARE:return hwData[index];
				case Server.SOFTWARE:return swData[index];
				case Server.FIELD:return fdData[index];
				default: throw new IllegalArgumentException("Invalid inspection type");
			}
		}

		/**Sets the Team's status for the given level of Inspection
		 * 
		 * @param type
		 * @param i
		 */
		public void setStatus(String type, int i) {
			if(type.equals("CI"))this.checkedIn = i==3?true:false;
			if(type.equals("SC")){
				this.cube = i;
				if(this.cube ==  Server.PASS && !Server.separateCube){
					hwData[CUBE_INDEX] = true;
				}
			}
			if(type.equals("HW"))this.hardware=i;
			if(type.equals("SW"))this.software=i;
			if(type.equals("FD"))this.field=i;
			System.out.println("set "+this.number+" "+type+":"+i);			
	
			if(i==Server.NO_DATA){
				Server.addLogEntry(this.number+" "+type+" set to Uninspected"); 
			}
			else if(i==Server.PROGRESS){
				Server.addLogEntry(this.number+" "+type+" in progress");
			}
			else Server.addLogEntry(this.number+" has "+(i==Server.PASS?"passed ":"failed ")+type);
			
			if(passed(Server.CHECKIN) && passed(Server.CUBE) && passed(Server.HARDWARE) && passed(Server.SOFTWARE) && passed(Server.FIELD)){
				ready=true;
				System.out.println("READY");
			}
		}
		public void setNote(String type, String note){
			if(type.equals("HW"))this.hwNote=note;
			if(type.equals("SW"))this.swNote=note;
			if(type.equals("FD"))this.fdNote=note;
		}
		
		/**Checks whether a fully tracked inspection type is complete
		 * 
		 * @param type
		 * @return
		 */
		private boolean checkFullInspection(int type){
			
			boolean[] data;
			switch(type){
				case Server.HARDWARE:data=hwData;break;
				case Server.SOFTWARE:data=swData;break;
				case Server.FIELD:data= fdData;break;
				default: throw new IllegalArgumentException("Invalid inspection type");
			}
			
			for(boolean b:data){
				if(!b)return false;
			}
			return true;
		
		}
		/**
		 * This method sets the value of the boolean at the given index to the given status for the
		 * given inspection type.
		 * @param type
		 * @param index
		 * @param status
		 */
		public void setInspectionIndex(String type, int index, boolean status) {
			System.out.println("TEAM SET:"+hwData+" "+index+" "+status);
			if(type.equals("HW")){
				hwData[index]=status;				
				//TODO pass hw and fail cube when not separate? Or not, cuz they have technically failed HW at that point. consider sigs for hw and how that affect this
				if(this.getStatus(Server.HARDWARE)!=Server.PROGRESS)this.setStatus(type, Server.PROGRESS);//dont set if we dont have to cuz status log
				if(index == CUBE_INDEX && Server.trackCube && !Server.separateCube){
					setStatus("SC",status?Server.PASS:Server.FAIL);
				}
			}
			if(type.equals("SW")){
				swData[index]=status;			
				if(this.getStatus(Server.SOFTWARE)!=Server.PROGRESS)this.setStatus(type, Server.PROGRESS);
			}
			if(type.equals("FD")){
				fdData[index]=status;			
				if(this.getStatus(Server.FIELD)!=Server.PROGRESS)this.setStatus(type, Server.PROGRESS);
			}
		}
		
		@Override
		public int compareTo(Team o) {
			if(o instanceof Team){
				return number-((Team)o).number;
			}
			return 0;
		}
		
		/**
		 * Returns a comma separated String containing the team number followed by
		 *  the overall status in the following order:<br>
		 *  CheckIn, Cube, Hardware, Software, Field
		 *  
		 *  <br>
		 *  This is used to save team status data.
		 * 
		 * @return
		 */
		public String getStatusString(){
			return number+","+checkedIn+","+cube+","+hardware+","+software+","+field;
		}
		

		public void setSignature(String type, String teamSig, String inpSig) {
			if(type.equals("HW")){
				this.hwTeamSig=teamSig;
				this.hwInspSig=inpSig;
			}
			if(type.equals("SW")){
				this.swTeamSig=teamSig;
				this.swInspSig=inpSig;
			}
			if(type.equals("FD")){
				this.fdTeamSig=teamSig;
				this.fdInspSig=inpSig;
			}
		}
		
		public String[] getSigs(String type){
			String[] s=new String[0];
			if(type.equals("HW") && hardware==Server.PASS){
				s= new String[]{hwTeamSig,hwInspSig};
			}
			if(type.equals("SW") && software==Server.PASS){
				s= new String[]{swTeamSig,swInspSig};
			}
			if(type.equals("FD") && field==Server.PASS){
				s= new String[]{fdTeamSig,fdInspSig};
			}
			return s;
		}
		
		public void setName(String name){
			this.name = name;
			
		}
		
		
		public String toString(){
			return number+"  "+name;
		}

		
}
