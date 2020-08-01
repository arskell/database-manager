import java.util.Queue;
import java.util.Date;
import java.text.SimpleDateFormat;
public class Logger{

	public static void write(String msg){
		SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
		Date date = new Date(System.currentTimeMillis());
		System.out.print(formatter.format(date) + " | " + msg +"\n\r");
	}
}