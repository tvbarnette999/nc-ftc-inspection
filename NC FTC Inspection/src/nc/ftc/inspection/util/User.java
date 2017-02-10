package nc.ftc.inspection.util;

import nc.ftc.inspection.Server;

public class User {
	public static final int GENERAL = 0;
	public static final int TEAM = 1;
	public static final int INSPECTOR = 5;
	public static final int ADMIN = 100;
	public String user;
	public String pass;
	public int level = User.GENERAL;
	public User(String user, String pass) {
		if (pass.equals(Server.password)) {
			level = User.INSPECTOR;
		} else if (user.equals("admin") && pass.equals(Server.adminPassword)){
			level = User.ADMIN;
		} else if (user.equals("team") && pass.equals(Server.teamPassword)) {
			//TODO change login format
			level = User.TEAM;
		}
	}
	public User() {
		user = "general";
	}
	public boolean is(int level) {
		System.out.println("checking if " + user + " is " + level + ": " + (this.level >= level));
		return this.level >= level;
	}
}
