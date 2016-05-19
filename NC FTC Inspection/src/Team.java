

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
			hw=new boolean[Server.HW_SIZE];
			sw=new boolean[Server.SW_SIZE];
			fd=new boolean[Server.FD_SIZE];
		
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
			Server.statusLog.add("[TIME]: "+this.number+" "+type+" set to "+i);//TODO make this useful ie 1533 has PASSED hardware
			if(this.checkedIn&&this.cube==Server.PASS&&this.hardware==Server.PASS&&this.software==Server.PASS&&this.field==Server.PASS){
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
