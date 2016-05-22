

public class Team implements Comparable{
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
		int software=Server.PASS;
		int cube=Server.PASS;
		int field;
		boolean ready;
		//arrays for inspection
		boolean[] hw;
		boolean[] sw;
		boolean[] fd;
		public Team(int n){
			number=n;
			name=Main.teamData.get(number);
			hw=new boolean[Server.HWForm.size()];
			sw=new boolean[Server.SWForm.size()];
			fd=new boolean[Server.FDForm.size()];
		
		}

		/**
		 * Returns the Team's status for the given level of Inspection
		 * @param i
		 * @return
		 */
		public int get(int i){
			switch(i){
				case Server.CHECKIN:return checkedIn?Server.PASS:0;
				case Server.CUBE:return cube;
				case Server.HARDWARE:return hardware;
				case Server.SOFTWARE:return software;
				case Server.FIELD:return field;
			}
			return 0;
		}
		
		/**
		 * Returns the Team's status for the inspection element at the specified index for the specified type.
		 * @param type
		 * @param index
		 * @return
		 */
		public boolean get(int type, int index){
			switch(type){
				case Server.HARDWARE:return hw[index];
				case Server.SOFTWARE:return sw[index];
				case Server.FIELD:return fd[index];
				default: throw new IllegalArgumentException("Invalid inspection type");
			}
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
//			Server.statusLog.add("[TIME]: "+this.number+" "+type+" set to "+i);//TODO make this useful ie 1533 has PASSED hardware
	
			if(i==Server.NO_DATA){
				Server.addLogEntry(this.number+" "+type+" set to Uninspected"); 
			}
			else if(i==Server.PROGRESS){
				Server.addLogEntry(this.number+" "+type+" in progress");
			}
			else Server.addLogEntry(this.number+" has "+(i==Server.PASS?"passed ":"failed ")+type);
			if(this.checkedIn&&this.cube==Server.PASS&&this.hardware==Server.PASS&&this.software==Server.PASS&&this.field==Server.PASS){
				ready=true;
			}
		}
		
		/**Checks whether a fully tracked inspection type is complete
		 * 
		 * @param type
		 * @return
		 */
		private boolean checkFullInspection(int type){
			
			boolean[] data;
			switch(type){
				case Server.HARDWARE:data=hw;break;
				case Server.SOFTWARE:data=sw;break;
				case Server.FIELD:data= fd;break;
				default: throw new IllegalArgumentException("Invalid inspection type");
			}
			
			for(boolean b:data){
				if(!b)return false;
			}
			return true;
		
		}
		public void set(String type, int index, boolean status) {
			System.out.println("TEAM SET:"+hw+" "+index+" "+status);
			if(type.equals("HW")){
				//TODO deal with cube
				hw[index]=status;				
				//if all true, set hardware to pass
				if(checkFullInspection(Server.HARDWARE))this.set(type,Server.PASS);
				else if(this.get(Server.HARDWARE)!=Server.PROGRESS)this.set(type, Server.PROGRESS);//dont set if we dont have to cuz status log
			}
			if(type.equals("SW")){
				sw[index]=status;			
				if(checkFullInspection(Server.SOFTWARE))this.set(type,Server.PASS);
				else if(this.get(Server.SOFTWARE)!=Server.PROGRESS)this.set(type, Server.PROGRESS);
			}
			if(type.equals("FD")){
				fd[index]=status;			
				if(checkFullInspection(Server.FIELD))this.set(type,Server.PASS);
				else if(this.get(Server.FIELD)!=Server.PROGRESS)this.set(type, Server.PROGRESS);
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
