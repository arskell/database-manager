import java.util.ArrayList;
import java.lang.Thread;

import java.util.Scanner; 

public class Main{
	
	static public void main(String[] args){
		try{
			ShopDb.connect("Shop_db");;
			Logger.write("Setting up server..");
			HTTPServ.setup("0.0.0.0", 100);
			Logger.write(" - Type 'quit' for quitting -");
			Logger.write("Server is up");
			Scanner in = new Scanner(System.in); 
			//ожидание команды выхода
			while(!in.nextLine().equals("quit"));
			HTTPServ.close();
			ShopDb.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}