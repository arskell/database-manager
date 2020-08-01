import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Date;
import java.nio.file.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.lang.reflect.*;
import java.util.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;

public class ShopDb{
	
	private static String db_name;
	
	public static Connection conn;
	public static Statement statmt;
	public static ResultSet resSet;
	
	private static SimpleDateFormat inp_format;
	private static SimpleDateFormat out_format;
	
	//Настройка таблиц и других структур данных
	private static void startProcedure() throws ClassNotFoundException, SQLException, IOException{
		
		statmt = conn.createStatement();
		resSet = statmt.executeQuery("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name == 'clients' OR name == 'products' OR name == 'suppliers' OR name == 'client_transactions';");
		
		if(resSet.getInt(1) != 4){
			Logger.write("WARNING: Can't find some tables, setting up...");
			
			statmt.execute(new String(Files.readAllBytes(Paths.get("./database/create_queries/clients.sql"))));
			statmt.execute(new String(Files.readAllBytes(Paths.get("./database/create_queries/products.sql"))));
			statmt.execute(new String(Files.readAllBytes(Paths.get("./database/create_queries/suppliers.sql"))));
			statmt.execute(new String(Files.readAllBytes(Paths.get("./database/create_queries/client_transactions.sql"))));
			
		}else{
			Logger.write("Successfully found saved tables");
		}
		
		inp_format = new SimpleDateFormat("yyyy-MM-dd");
		out_format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	}

	//венуть крайний id у таблицы
	public static int getLastId(String table_name, ArrayList<String> fields) throws SQLException{
		Logger.write("Requesting last id in "+ table_name+" table");
		resSet = conn.createStatement().executeQuery("SELECT * FROM "+table_name+" ORDER BY "+fields.get(0)+" desc LIMIT 1;");
		int id;
		
		if(!resSet.next()){
			id = 0;
		}else{
			id = resSet.getInt(1);
		}
		return id;
	}
	
	//вернуть количество записей в таблице
	public static int getCountOfRecords(String table_name) throws SQLException{
		Logger.write("Requesting count of records in "+ table_name+" table");
		resSet = conn.createStatement().executeQuery("SELECT COUNT(*) FROM "+table_name +";");
		int id;
		
		if(!resSet.next()){
			id = 0;
		}else{
			id = resSet.getInt(1);
		}
		return id;
	}
	
	
	//удалить запись у таблицы
	public static boolean deleteRecord(String table_name, ArrayList<String> fields, String id) throws Exception{
		Logger.write("Deleting a record in table "+ table_name +" with id "+id);
		statmt.execute("DELETE FROM "+table_name+" WHERE "+fields.get(0)+"="+id+";");
		return true;
	}
	

	//преобразовать струкурированную запись в список строк
	public static NormalizedRecord normalizeRecordFromObject(Object record) throws NoSuchFieldException, IllegalAccessException{
		NormalizedRecord result = new NormalizedRecord();
		
		if(record.getClass().isAssignableFrom(Client_record.class)){
			
			result.data.add(Integer.toString((Integer)record.getClass().getField("client_id").get(record)) );
			result.data.add(record.getClass().getField("login").get(record).toString());
			result.data.add(record.getClass().getField("first_name").get(record).toString());
			result.data.add(record.getClass().getField("last_name").get(record).toString());
			result.data.add(Integer.toString((Integer)record.getClass().getField("account_pass_hash").get(record)));
			
		}else if(record.getClass().isAssignableFrom(Product_record.class)){
			
			result.data.add(Integer.toString((Integer)record.getClass().getField("product_id").get(record)) );
			result.data.add(record.getClass().getField("name").get(record).toString());
			result.data.add(record.getClass().getField("description").get(record).toString());
			result.data.add(String.valueOf((float)record.getClass().getField("price_per_unit").get(record)) );
			result.data.add(String.valueOf((float)record.getClass().getField("weight").get(record)) );
			result.data.add(record.getClass().getField("picture_path").get(record).toString());
			
		}else if(record.getClass().isAssignableFrom(Supplier_record.class)){
			
			result.data.add(Integer.toString((Integer)record.getClass().getField("id").get(record)) );
			result.data.add(Integer.toString((Integer)record.getClass().getField("product_id").get(record)) );
			result.data.add(record.getClass().getField("supplier_name").get(record).toString());
			result.data.add(String.valueOf((float)record.getClass().getField("weight").get(record)) );
			result.data.add(String.valueOf((float)record.getClass().getField("price_per_unit").get(record)) );
			result.data.add( record.getClass().getField("date").get(record).toString() );
			
		}else if(record.getClass().isAssignableFrom(Client_transaction_record_pure.class)){
			
			result.data.add(Integer.toString((Integer)record.getClass().getField("id").get(record)) );
			result.data.add(Integer.toString((Integer)record.getClass().getField("client_id").get(record)) );
			result.data.add(Integer.toString((Integer)record.getClass().getField("product_id").get(record)) );
			result.data.add(String.valueOf((float)record.getClass().getField("weight").get(record)) );
			result.data.add(String.valueOf((float)record.getClass().getField("price_per_unit").get(record)) );
			result.data.add(record.getClass().getField("date").get(record).toString());
			result.data.add(Integer.toString((Integer)record.getClass().getField("transaction_id").get(record)) );
			
		}
		
		return result;
	}
	
	//проверяет явялется ли содердимое поля нулем
	private static boolean isNullField(String record){
		if(record.equals("0") || record.equals("0.0") || record.equals((new Timestamp(0)).toString())){
			return true;
		}
		return false;
	}
	
	//преобразование даты
	private static String convertDateYYYMMDDtoDatetime(String date) throws Exception{
		return out_format.format(inp_format.parse(date)).toString();
	}
	
	//преобразование float в String
	private static String floatToStringCur(float value){
		return new DecimalFormat("#.##").format(value);
	}
	
	//вернуть значение поля в таблице
	public static String getValueById(String table_name,String id, String id_name, String column_name) throws Exception{
		String result = "";
		resSet = conn.createStatement().executeQuery("SELECT * FROM "+table_name+" WHERE "+id_name+" = '"+id+"';");
		if(resSet.next()){
			result = resSet.getString(column_name);
		}
		return result;
	}
	
	//расчет прибыли
	public static ArrayList<String[]> getCalculatorResults(String date_begin, String date_end) throws Exception{
		ArrayList<String[]> result = new ArrayList<String[]>();
		
		date_begin = convertDateYYYMMDDtoDatetime(date_begin);
		date_end = convertDateYYYMMDDtoDatetime(date_end);
		
		
		resSet = conn.createStatement().executeQuery("SELECT * FROM suppliers WHERE date BETWEEN '"+date_begin+"' AND '"+date_end+"';");
		ArrayList<Supplier_record> suppliers = new ArrayList<Supplier_record>();
		
		while(resSet.next()){
			Supplier_record tmp = new Supplier_record();
			tmp.product_id = resSet.getInt("product_id");
			tmp.weight = resSet.getFloat("weight");
			tmp.price_per_unit = resSet.getFloat("price_per_unit");
			suppliers.add(tmp);
		}
		resSet = conn.createStatement().executeQuery("SELECT * FROM client_transactions WHERE date BETWEEN '"+date_begin+"' AND '"+date_end+"';");
		ArrayList<Client_transaction_record_pure> transactions = new ArrayList<Client_transaction_record_pure>();
		
		while(resSet.next()){
			Client_transaction_record_pure tmp = new Client_transaction_record_pure();
			tmp.product_id = resSet.getInt("product_id");
			tmp.weight = resSet.getFloat("weight");
			tmp.price_per_unit = resSet.getFloat("price_per_unit");
			transactions.add(tmp);
		}
		
		TreeMap<Integer, Float> product_buy = new TreeMap<Integer, Float>();
		TreeMap<Integer, Float> product_sell = new TreeMap<Integer, Float>();
		
		float total_profit = 0;
		float total_sell = 0;
		float total_buy = 0;
		
		HashSet<Integer> pruducts_id_set = new HashSet<Integer>();
		
		for(Supplier_record record: suppliers){
			product_buy.putIfAbsent(record.product_id, 0.0f);
			float tmp =  record.weight*record.price_per_unit;
			product_buy.put(record.product_id, product_buy.get(record.product_id)+tmp);
			total_buy +=  tmp;
			pruducts_id_set.add(record.product_id);
		}
		
		for(Client_transaction_record_pure record: transactions){
			product_sell.putIfAbsent(record.product_id, 0.0f);
			float tmp = record.weight*record.price_per_unit;
			product_sell.put(record.product_id, product_sell.get(record.product_id)+tmp);
			total_sell += tmp;
			pruducts_id_set.add(record.product_id);
		}
		
		
		result.add( new String[]{"product_id", "product","buy", "sell","profit"});
		
		for(int id: pruducts_id_set){
			
			float tmp_buy = product_buy.getOrDefault(id, 0f);
			float tmp_sell =  product_sell.getOrDefault(id, 0f);
			String id_str = Integer.toString(id);
			result.add( new String[]{id_str, getValueById("products",id_str,"product_id","name"),floatToStringCur(tmp_buy),floatToStringCur(tmp_sell), floatToStringCur(tmp_sell - tmp_buy)});
		}
		
		total_profit = total_sell - total_buy;
		result.add( new String[]{"Summary", "",floatToStringCur(total_buy), floatToStringCur(total_sell),floatToStringCur(total_profit)});
		
		return result;
	}
	
	
	//поиск в таблице
	public static ArrayList<NormalizedRecord> searchInTable(String table_name, ArrayList<String> fields, Object record) throws Exception{
		Logger.write("Requesting SELECT command");
		

		StringBuilder builder = new StringBuilder();
		builder.append("SELECT * FROM "+table_name+" WHERE ");
		NormalizedRecord norm_record = normalizeRecordFromObject(record);
		for(int i = 0; i < norm_record.data.size(); ++i ){
			if(!isNullField(norm_record.data.get(i))){
				Logger.write(fields.get(i) + " has not zero");
				builder.append(fields.get(i)).append("='").append(norm_record.data.get(i)).append("';");
				break;
			}
		}
		Logger.write(builder.toString());
		resSet = conn.createStatement().executeQuery(builder.toString());
		ArrayList<NormalizedRecord> result = new ArrayList<NormalizedRecord>();		
		while(resSet.next()){
			result.add(NormalizeRecordFromRS());
		}
		
		return result;
	}
	
	//изменение записи
	public static void changeRecord(String table_name, ArrayList<String> fields, Object record) throws Exception{
		StringBuilder q = new StringBuilder();
		
		NormalizedRecord norm_record = normalizeRecordFromObject(record);		
		Logger.write("Updating "+ table_name +" record with id "+ norm_record.data.get(0));
		q.append("UPDATE "+ table_name +" SET ");
		for(int i = 1; i < fields.size(); ++i){
			q.append(fields.get(i)).append(" = '").append(norm_record.data.get(i)).append("'");
			if(i != fields.size()-1) q.append(",");
		}
		q.append(" WHERE "+fields.get(0) +"= ").append(norm_record.data.get(0)).append(";");
		
		statmt.execute(q.toString());
	}
	//добавление записи
	public static boolean createRecord(String table_name, ArrayList<String> fields, Object record) throws Exception{
		StringBuilder q = new StringBuilder();
		NormalizedRecord norm_record = normalizeRecordFromObject(record);
		
		q.append("INSERT INTO ").append(table_name).append(" ");
		
		q.append("(");
		for(int i = 0; i < fields.size(); ++i){
			q.append(fields.get(i));
			if(i != fields.size()-1) q.append(",");
		}
		q.append(") VALUES(");
		
		for(int i = 0; i < fields.size(); ++i){
			q.append("'").append(norm_record.data.get(i)).append("'");
			if(i != fields.size()-1) q.append(",");
		}
		
		q.append(");");

		statmt.execute(q.toString());
		return true;
	}
	
	//вернуть список значений записи в формате String из resultset 
	private static NormalizedRecord NormalizeRecordFromRS() throws Exception{
		
		NormalizedRecord result = new NormalizedRecord();
		
		result.id = resSet.getInt(1);
		
		for(int i = 1; i < resSet.getMetaData().getColumnCount() + 1; i++){
			result.data.add(resSet.getString(i));
		}
		return result;
	}
	
	//чтение таблицы
	public static ArrayList<NormalizedRecord> readTable(String table_name, int start_id, int count) throws Exception{
		Logger.write("Requesting "+ table_name +" records list with start id " + Integer.toString(start_id) + ". Count: "+Integer.toString(count + 1));
		ArrayList<NormalizedRecord> result = new ArrayList<NormalizedRecord>();
		
		resSet = conn.createStatement().executeQuery("SELECT * FROM "+table_name+";"); 
		
		resSet = conn.createStatement().executeQuery("SELECT * FROM "+table_name+" WHERE "+resSet.getMetaData().getColumnName(1)+" BETWEEN "+Integer.toString(start_id)+ " AND "+Integer.toString(start_id+count)+ ";");
		
		while(resSet.next()){
			result.add(NormalizeRecordFromRS());
		}
		
		return result;
	}
	
	public static NormalizedRecord getNormalizedRecord(String table, int id) throws Exception{
		return readTable(table, id, 0).get(0);
	}
	//соединение к базе данных
	public static void connect(String name) throws ClassNotFoundException, SQLException, IOException{
		db_name = name;
		
		conn = null;
		Class.forName("org.sqlite.JDBC");
		Properties p=new Properties();
		p.setProperty("useUnicode","yes");
		p.setProperty("characterEncoding","UTF-8");
		
		conn = DriverManager.getConnection("jdbc:sqlite:database/"+name+".sqlite", p);
		
		Logger.write("Database '"+name+"' was connected");
		startProcedure();
		Logger.write("Database is up");
	}
	//закрытие соединения 
	public static void close() throws SQLException{
		Logger.write("Closing database...");
		if(conn != null)conn.close();
		if(statmt != null)statmt.close();
		if(resSet != null)resSet.close();
	}
	
}