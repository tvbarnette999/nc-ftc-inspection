package nc.ftc.inspection;

import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Math.max;

public class InspectionForm {
	public enum CB_LEVEL{
		REQUIRED  (0, "REQ"),
		OPTIONAL (1, "OPT"),
		NA (-1, "NA");
		public final int value;
		final String abbrev;
		private CB_LEVEL(int val, String abbrev){
			this.value = val;
			this.abbrev = abbrev;
		}
		
		public String toString(){
			return abbrev;
		}
		
		static CB_LEVEL get(int i){
			switch(i){
				case 0: return REQUIRED;
				case 1: return OPTIONAL;
				case -1: return NA;
			}
			return null;
		}
	}
	
	//TODO SIZING CUBE INDEX TO AUTOFIL FROM SEPARATE CUBE
	
	ArrayList<Row> rows = new ArrayList<Row>();
	public int cbTotal;
	int widestRow;
	public int type; 
	public int cubeIndex = -1;
	String delimiter;
	public String color = "#E6B222"; //the old color
	public String header;
	
	static class Row{
		int cbCount;
		int[] param;
		String explain;
		String rule;
		
		/**
		 * Takes the string from the save file split by :: and parses it into a Row object
		 * @param raw
		 */
		public Row(String[] raw){
			cbCount = Integer.parseInt(raw[0]);
			param = new int[cbCount];
			for(int i = 0; i < cbCount; i++){
				param[i] = Integer.parseInt(raw[1 + i]);
			}
			try{
				explain = raw[cbCount + 1];
				rule = raw[cbCount + 2];
			}catch(ArrayIndexOutOfBoundsException e){
				//This row doesnt have enough data.
			}
		}
		
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
			Row r = new Row();
			r.cbCount = cbCount;
			r.explain = explain;
			r.param = param.clone();			
			return r;
		}


		//only HeaderRow can get to this
		private Row(){
			
		}
		
	}
	
	
	public static class HeaderRow extends Row{
		String[] titles;
		
		public HeaderRow(String[] raw){
			cbCount = Integer.parseInt(raw[1]);
			titles = new String[cbCount];
			for(int i = 0; i < cbCount; i++){
				titles[i] = raw[2 + i];
			}
			explain = raw[cbCount + 2];
			rule = raw[cbCount + 3];
		}
		private HeaderRow(){
			
		}
		@Override
		protected Object clone() throws CloneNotSupportedException {
			HeaderRow r = new HeaderRow();
			r.cbCount = cbCount;
			r.explain = explain;
			r.titles = titles.clone();
			return r;
		}
		
	}
	public void setDelimiter(String del){
		delimiter = del;
	}
	
	public InspectionForm(int type){
		this.type = type;
	}
	
	public void addRow(String row){
//		this.delim = delimiter;
//		System.out.println("Adding row: " + row);
		try{
			String[] split = row.split(delimiter, -1); //negative number doesnt leave off trailing empty strings
			if(split[0].startsWith("I")){
				//metadata row.
				for(String data : split){
					System.out.println("Data Line: "+data);
					if(data.startsWith("color=")){
						color = data.substring(data.indexOf("=") + 1);
					}
					if(data.startsWith("cube_index=")){
						cubeIndex = Integer.parseInt(data.substring(data.indexOf("=") + 1));
					}
					if(data.startsWith("header=")){
						header = data.substring(data.indexOf("=") + 1);
					}
				}
			}
			else if(split[0].startsWith("H")){
				Row r = new HeaderRow(split);
				rows.add(r);
				widestRow = max(widestRow, r.cbCount);
			} 
			else{
				Row r = new Row(split);
				rows.add(r);
				cbTotal += r.cbCount;
				widestRow = max(widestRow, r.cbCount);			
			}
		}catch(Exception e){
			Server.addErrorEntry("Error Loading Inspection Form row: " + row);
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the table part of the inspection form
	 * @return
	 */
	public String getFormTable(Team ... teams){ //for multiple teams, make Team ..., at each step do another <td> / <th> for each. For headers, do <br> then (#). multiple comments/pass buttons? or select team then enter?
		String type = null;
		switch(this.type){
			case Server.HARDWARE: type = "_HW"; break;
			case Server.SOFTWARE: type = "_SW"; break;
			case Server.FIELD:    type = "_FD"; break;
		}
		StringBuffer table = new StringBuffer();
		table.append("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">");
		int cbIndex = 0;
		for(Row r : rows){
			int span = r.cbCount == 0 ? 0 : widestRow / r.cbCount;
			if(r instanceof HeaderRow){
				//TODO LCM of table widths
				table.append("<tr bgcolor=\""+color+"\">"); //old color is #E6B222
				for(Team t : teams){
					for(String title : ((HeaderRow)r).titles){
						table.append("<th colspan=\"" + span + "\" style=\"min-width:50pt\">"); //NOTE: size optimized for mobile!
						table.append(title);
						if(teams.length > 1)table.append("<br>("+t.number+")");
						table.append("</th>");
					}
				}
				table.append("<th align=\"left\" >" + r.explain + "</th>");
				table.append("<th>" + r.rule + "</th>");
			}
			else{
				table.append("<tr bgcolor=\"#FFFFFF\"  height=\"30px\">");
				
				for(int param : r.param){
					for(Team t : teams){
						table.append("<td");
						if(param == CB_LEVEL.NA.value){
							table.append(" colspan=\"" + span + "\"");
							table.append(" align=\"center\"");
							table.append(">NA");						
						} else{
							
							table.append(" id=" + t.number + type + cbIndex);
							table.append(" colspan=\"" + span + "\"");
							//table.append("<input type=\"checkbox\"");
	//						table.append(" name=\"" + team.number + type + cbIndex + "\" ");
							table.append(" checked=" + t.getStatus(this.type, cbIndex)); 
							table.append(" value=\"" + param + "\"");
							table.append(" class=" + CB_LEVEL.get(param)); //todo change to req vs opt to simplify
							table.append(" align=\"center\"");
							table.append(" onclick=\"update()\">");
							table.append(t.getStatus(this.type,cbIndex) ? '\u2713' : " "); 
	//						table.append("</label>");
							
						}
						
						table.append("</td>");
						
					}
					if(param != CB_LEVEL.NA.value){
						cbIndex++;
					}
				}
				table.append("<td>" + r.explain + "</td>");
				table.append("<td>" + r.rule + "</td>");
			}
			table.append("</tr>");
		}
		table.append("</table>");
		
		return table.toString().replaceAll("&lt;br&gt;", "<br>");
	}
	
	
}
