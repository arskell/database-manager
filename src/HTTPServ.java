import com.sun.net.httpserver.*;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.io.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.lang.reflect.*;

class HTTPServ{

	public static HttpServer server;
	
	public static ArrayList<HttpContext> contextList;
	
	//количество записей на страницу в режиме просмотра
	public static int records_per_page;
	
	private static String global_style;
	private static String head_links;
	
	//шаблоны для формирования web-страниц
	private static String upper_template;
	private static String down_template;
	
	private static HashMap<String, ArrayList<String>> table_fields;

	private static SimpleDateFormat inp_format;
	private static SimpleDateFormat out_format;
	
	//настройка http сервера
	public static void setup(String _host, int _port) throws Exception{
		server = HttpServer.create();
		server.bind(new InetSocketAddress(_host, _port), 10);
		
		records_per_page = 25;
		
		global_style = new String(Files.readAllBytes(Paths.get("./site/global-style.txt")));
		head_links = new String(Files.readAllBytes(Paths.get("./site/head_links.html")));
		
		StringBuilder template = new StringBuilder();
		template.append("<!DOCTYPE HTML>");
		template.append("<html>");
		template.append("<head> <meta charset=\"windows-1251\">");
		template.append(global_style);
		template.append("</head>");
		template.append("<body>");
		template.append("<div align=\"center\">");
		template.append(head_links);
		upper_template = template.toString();
		
		template = new StringBuilder();
		
		template.append("</div>");
		template.append("</body>");
		template.append("</html>");
		
		down_template = template.toString();
		
		table_fields = new HashMap<String, ArrayList<String>>();
		
		table_fields.put("clients", new ArrayList<String>());
		for(Field field: Client_record.class.getFields()){
			table_fields.get("clients").add(field.getName());
		}
		
		table_fields.put("suppliers", new ArrayList<String>());
		for(Field field: Supplier_record.class.getFields()){
			table_fields.get("suppliers").add(field.getName());
		}
		
		table_fields.put("products", new ArrayList<String>());
		for(Field field: Product_record.class.getFields()){
			table_fields.get("products").add(field.getName());
		}
		
		table_fields.put("client_transactions", new ArrayList<String>());
		for(Field field: Client_transaction_record_pure.class.getFields()){
			table_fields.get("client_transactions").add(field.getName());
		}
		
		
		inp_format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
		out_format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		
		contextList = new ArrayList<HttpContext>();
		
		//обработчики при переходе на различные ссылки
		contextList.add(server.createContext("/index", new HandlerIndex()));
		contextList.add(server.createContext("/show/", new HandlerViewer()));
		contextList.add(server.createContext("/edit/", new HandlerEditor()));
		contextList.add(server.createContext("/update/", new HandlerUpdater()));
		contextList.add(server.createContext("/create/", new HandlerCreater()));
		contextList.add(server.createContext("/add/", new HandlerAdder()));
		contextList.add(server.createContext("/remove/", new HandlerRemover()));
		contextList.add(server.createContext("/search/", new HandlerSearcher()));
		contextList.add(server.createContext("/searchresults/", new HandlerSearchResults()));
		contextList.add(server.createContext("/calculator/", new HandlerCalculator()));
		contextList.add(server.createContext("/calculatorresults/", new HandlerCalculatorResults()));
		server.start();
		
	}
	
	//закрытие сервера
	public static void close(){
		Logger.write("Stopping server...");
		server.stop(0);
	}
	

	static class HandlerIndex implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			Logger.write("HandlerIndex was called");
			StringBuilder builder = new StringBuilder();
			
			builder.append(upper_template);
			builder.append(down_template);
			
			exchange.sendResponseHeaders(200,builder.length());
			exchange.getResponseBody().write(builder.toString().getBytes());
			exchange.getResponseBody().close();
			
		}
	}
	
	static class HandlerCalculatorResults implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			Logger.write("HandlerCalculatorResults was called");

			String params[] = getQuery(exchange).split("&");
			
			String start_date_str = new String();
			String end_date_str = new String();
			
			for(String param: params){
				String[] elems = param.split("=");
				
				switch(elems[0]){
					case "start_date":
					start_date_str = elems[1];
					break;
					case "end_date":
					end_date_str = elems[1];
					break;
				}
			}
			
			StringBuilder builder = new StringBuilder();
			
			builder.append(upper_template);
			try{
				
				builder.append("<table border=\"1\" >");

				
				for(String[] row: ShopDb.getCalculatorResults(start_date_str, end_date_str)){
					
					builder.append("<tr>");
					for(String elem: row){
						
						builder.append("<td>").append(elem).append("</td>");
						
					}
					builder.append("</tr>");
				
				}
				builder.append("</table>");
				
			}catch(Exception exc){
				builder.append(exc.toString());
				Logger.write(exc.toString());
			}
			builder.append(down_template);
			
			exchange.sendResponseHeaders(200,builder.length());
			exchange.getResponseBody().write(builder.toString().getBytes());
			exchange.getResponseBody().close();
		}
	}
	
	
	static class HandlerCalculator implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			
			Logger.write("HandlerCalculator was called");
			StringBuilder builder = new StringBuilder();
			
			builder.append(upper_template);
			
			builder.append("<br><a>Calculation of revenue for the period:</a><br>");

			builder.append("<form accept-charset=\"utf-8\" name=\"Calculator\" method=\"post\" action=\"\\calculatorresults\\\">");	
			
			builder.append("<label for=\"start_date\"> From: </label>").append("<input type=\"date\" name=\"start_date\" value=\"\"><br>");
			builder.append("<br>");
			builder.append("<label for=\"end_date\"> To: </label>").append("<input type=\"date\" name=\"end_date\" value=\"\"><br>");
			
			builder.append("<button>Submit</button>");
			builder.append("</form>");
			
			builder.append(down_template);
			
			exchange.sendResponseHeaders(200,builder.length());
			exchange.getResponseBody().write(builder.toString().getBytes());
			exchange.getResponseBody().close();
		}
	}
	
	//вернуть имя таблицы для обращения из url 
	private static String getDestNameInPath(String pure_path){
		String[] path = pure_path.split("/");
		return path[path.length-1];
	} 
	
	
	private static String searchResults(String table_name, ArrayList<String> fields, HttpExchange exchange, String data) throws IOException{
		
		try{
			Object pattern = createRecordObjectFromRequest(table_name, data);
			StringBuilder builder = new StringBuilder();
			
			builder.append(upper_template);
			builder.append("<table border=\"1\" >");
			builder.append("<caption>" +table_name+ " search results</caption> ");
			try{
			
				builder.append("<tr>");
				for(String field: fields){
					builder.append("<td>").append(field).append("</td>");
				}
				builder.append("</tr>");

					
				for(NormalizedRecord record:ShopDb.searchInTable(table_name,fields, pattern)){
					builder.append("<tr>");
					for(String element:record.data){
						builder.append("<td>").append(element).append("</td>");
					}
					builder.append("<td>").append("<a href=\"/edit/"+table_name+"?"+record.id+"\">edit</a>").append("</td>");
					builder.append("<td>").append("<a href=\"/remove/"+table_name+"?"+record.id+"\">remove</a>").append("</td>");
					builder.append("</tr>");
				}
			}catch(SQLException e){
				Logger.write(e.toString());
				builder.append(e.toString());
			}
			builder.append("</table>");
			builder.append("<a href=\"/create/"+table_name+"\">add record</a><br>");
			
			builder.append(down_template);
			return builder.toString();	
			
		}catch(Exception e){
			Logger.write(e.toString());
			return e.toString();
		}
		
	}
	
	//вернуть post запрос
	private static String getQuery(HttpExchange exchange) throws IOException{
			
			String result = "";
			
			InputStream inp = exchange.getRequestBody();
			InputStreamReader isReader = new InputStreamReader(inp);

			BufferedReader reader = new BufferedReader(isReader);
			StringBuffer sb = new StringBuffer();
			String str;
			while((str = reader.readLine())!= null){
				sb.append(str);
			}
			
			result = java.net.URLDecoder.decode(sb.toString(),  java.nio.charset.StandardCharsets.UTF_8.name());
			return result;
	}
	
	static class HandlerSearchResults implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			
			Logger.write("HandlerSearchResults was called");
			String table = getDestNameInPath(exchange.getRequestURI().getPath());
			String res = searchResults(table, table_fields.get(table), exchange, getQuery(exchange));

			exchange.sendResponseHeaders(200,res.length());
			exchange.getResponseBody().write(res.getBytes());
			exchange.getResponseBody().close();
			
		}
			
	}
	
	
	
	private static String searcher(String table_name, ArrayList<String> fields, HttpExchange exchange) throws IOException{
		
		StringBuilder builder = new StringBuilder();
			
		builder.append(upper_template);
			
		builder.append("<form accept-charset=\"utf-8\" name=\"Search record\" method=\"post\" action=\"\\searchresults\\"+table_name+"\">");
	
			for(int i = 0; i < fields.size(); i++){
				
				if(fields.get(i).equals("date")){
					builder.append("<label for=\""+fields.get(i)+"\">"+fields.get(i)+": </label>").append("<input type=\"datetime-local\" name=\""+fields.get(i)+"\" value=\"\"><br>");
				}else{
					builder.append("<label for=\""+fields.get(i)+"\">"+fields.get(i)+": </label>").append("<input name=\""+fields.get(i)+"\" value=\"\"><br>");
				}
				
			}
		builder.append("<button>Submit</button>");
		builder.append("</form>");
		builder.append(down_template);
		
		
		return builder.toString();
	}
	
	static class HandlerSearcher implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			
			Logger.write("HandlerSearcher was called");
			
			String table = getDestNameInPath(exchange.getRequestURI().getPath());
			String res = searcher(table, table_fields.get(table), exchange);
			
			exchange.sendResponseHeaders(200, res.length());
			OutputStream os = exchange.getResponseBody();
			os.write(res.getBytes());
			os.close();
			
		}
	}
	
	private static String remove(String table_name, ArrayList<String> fields, HttpExchange exchange, String id) throws IOException{
		
		
		String response = "<head><meta http-equiv=\"refresh\" content=\"1;URL=/show/"+table_name+"?1\"/></head>Removed!";
		
		try{
			ShopDb.deleteRecord(table_name, fields, id);
		}catch(Exception e){
			response = e.toString();
			Logger.write(response);
		}
		
		return response;
		
	}
	
	static class HandlerRemover implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			
			
			String res;
			
			try{

				String client_id = exchange.getRequestURI().getQuery();
				Logger.write("HandlerRemover was called. Id: " + client_id);

				String table = getDestNameInPath(exchange.getRequestURI().getPath());
				res = remove(table, table_fields.get(table), exchange, client_id);
				
				
			}catch(Exception e){
				Logger.write(e.toString());
				res = e.toString();
				return;
			}

			exchange.sendResponseHeaders(200,res.length());
			exchange.getResponseBody().write(res.getBytes());
			exchange.getResponseBody().close();
			
		}
	}
	
	//преобразовать строку запроса в готовую запись 
	private static Object createRecordObjectFromRequest(String table_name, String data) throws Exception{
		
		
		String[] params = data.split("&");
				
		Object record;
		Class<?> cl;
		
		switch(table_name){
			case "clients":
				record = new Client_record();
				cl = Client_record.class;
				break;
			case "products":
				record = new Product_record();
				cl = Product_record.class;
				break;
			case "client_transactions":
				record = new Client_transaction_record_pure();
				cl = Client_transaction_record_pure.class;
				break;
			case "suppliers":
				record = new Supplier_record();
				cl = Supplier_record.class;
				break;
			default:
				return new Object();
		}
		for(String param: params){
			String[] e = param.split("=");
			Field field = cl.getField(e[0]);
			
			if(field.getType().isAssignableFrom(int.class)){
				if(e.length > 1){
					if(e[0].equals("account_pass_hash")){
						field.set(record, e[1].hashCode());
					}else{
						field.set(record, Integer.parseInt(e[1]));
					}
				}
				else{
					field.set(record, 0);
				}
			} else if(field.getType().isAssignableFrom(String.class)){
				if(e.length > 1){
					field.set(record, e[1]);
				}else{
					field.set(record, "0");
				}
			} else if(field.getType().isAssignableFrom(float.class)){
				if(e.length > 1){
					field.set(record, Float.parseFloat(e[1]));
				}else{
					field.set(record, 0.0f);
				}
			} else if(field.getType().isAssignableFrom(Timestamp.class)){
				
				if(e.length > 1){
					try{
						e[1] = out_format.format(inp_format.parse(e[1])).toString();
					}catch(IllegalArgumentException excep){}

					field.set(record, Timestamp.valueOf(e[1]));
				}
				else{
					field.set(record,new Timestamp(0));
				}
			}
		}
			
		return record;
	}
	
	
	private static String add(String table_name, ArrayList<String> fields, HttpExchange exchange, String data) throws IOException{
		
		try{
			
			Object record = createRecordObjectFromRequest(table_name, data);	
			ShopDb.createRecord(table_name,fields,record);
				
		}catch(Exception e){
			Logger.write(e.toString());
			return e.toString();
		}
		
		return "<head><meta http-equiv=\"refresh\" content=\"1;URL=/show/"+table_name+"?1\"/></head>Added!";
		
	}
	
	static class HandlerAdder implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			
			Logger.write("HandlerAdder was called");

			String table = getDestNameInPath(exchange.getRequestURI().getPath());
			String res = add(table, table_fields.get(table), exchange, getQuery(exchange));
			
			exchange.sendResponseHeaders(200,res.length());
			exchange.getResponseBody().write(res.getBytes());
			exchange.getResponseBody().close();
		}
	}
	
	
	private static String create(String table_name, ArrayList<String> fields, HttpExchange exchange) throws IOException, SQLException{
		
			StringBuilder builder = new StringBuilder();
			
			builder.append(upper_template);
	
			builder.append("<form accept-charset=\"utf-8\" name=\"Add record\" method=\"post\" action=\"\\add\\"+table_name+"\">");
				
			builder.append("<label for=\""+fields.get(0)+"\">"+fields.get(0)+": </label>").append("<input name=\""+fields.get(0)+"\" value=\"");
			builder.append(Integer.toString(ShopDb.getLastId(table_name, fields) + 1)).append("\"><br>");
				
			for(int i = 1; i < fields.size(); i++){
				if(fields.get(i).equals("date")){
					builder.append("<label for=\""+fields.get(i)+"\">"+fields.get(i)+": </label>").append("<input type=\"datetime-local\" name=\""+fields.get(i)+"\" value=\"\"><br>");
				}else if(fields.get(i).equals("account_pass_hash")){
					builder.append("<label for=\""+fields.get(i)+"\">password: </label>").append("<input name=\""+fields.get(i)+"\" value=\"\"><br>");
				}
				else{
					builder.append("<label for=\""+fields.get(i)+"\">"+fields.get(i)+": </label>").append("<input name=\""+fields.get(i)+"\" value=\"\"><br>");
				}
			}
			builder.append("<button>Submit</button>");
			builder.append("</form>");
				

			builder.append(down_template);
			return builder.toString();
		
	}
	
	static class HandlerCreater implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			
			Logger.write("HandlerCreater was called");
			String res = "";
			
			try{
			String table = getDestNameInPath(exchange.getRequestURI().getPath());
			res = create(table, table_fields.get(table), exchange);
			}catch(Exception ex){
				Logger.write(ex.toString());
				res = ex.toString();
			}
			exchange.sendResponseHeaders(200,res.length());
			exchange.getResponseBody().write(res.getBytes());
			exchange.getResponseBody().close();
			
		}
	}
	
	

	private static String update(String table_name, ArrayList<String> fields, HttpExchange exchange, String request) throws IOException{
		
			try{
				Object record = createRecordObjectFromRequest(table_name, request);	
				ShopDb.changeRecord(table_name,fields,record);
				
			}catch(Exception e){
				Logger.write(e.toString());
				return e.toString();
			}
			
			return "<head><meta http-equiv=\"refresh\" content=\"1;URL=/show/"+table_name+"?1\"/></head>Updated!";
	}
	
	static class HandlerUpdater implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			Logger.write("HandlerClientsUpdater was called");

			String table = getDestNameInPath(exchange.getRequestURI().getPath());
			
			String res = update(table, table_fields.get(table), exchange,getQuery(exchange));
			
			exchange.sendResponseHeaders(200,res.length());
			exchange.getResponseBody().write(res.getBytes());
			exchange.getResponseBody().close();
		}
	}
	

	private static String editor(String table_name, ArrayList<String> fields, HttpExchange exchange, int id) throws Exception{
		
		StringBuilder builder = new StringBuilder();
			
			builder.append(upper_template);
			try{
				NormalizedRecord record = ShopDb.getNormalizedRecord(table_name, id);
				builder.append("<form accept-charset=\"utf-8\" name=\"Editing info\" method=\"post\" action=\"\\update\\"+table_name+"\">");
				
				for(int i = 0; i < fields.size(); i++){
					builder.append("<label for=\""+fields.get(i)+"\">"+fields.get(i)+": </label>").append("<input name=\""+fields.get(i)+"\" value=\"").append(record.data.get(i)).append("\"><br>");
				}
				builder.append("<button>Submit</button>");
				builder.append("</form>");
				
			}catch(SQLException e){
				Logger.write(e.toString());
				builder.append(e.toString());
			}
			builder.append(down_template);
			return builder.toString();
		
	}
	
	
	static class HandlerEditor implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			
			int id = Integer.parseInt(exchange.getRequestURI().getQuery());
			Logger.write("HandlerEditor was called. Id: " + Integer.toString(id));
			String table = getDestNameInPath(exchange.getRequestURI().getPath());
			String res = "";
			try{
			res = editor(table, table_fields.get(table), exchange, id);
			}catch(Exception ex){
				res = ex.toString();
				Logger.write(res);
			}
			exchange.sendResponseHeaders(200, res.length());
			OutputStream os = exchange.getResponseBody();
			os.write(res.getBytes());
			os.close();
		}
	}
	
	
	private static String viewer(String table_name, ArrayList<String> fields, HttpExchange exchange, int page) throws Exception{
			StringBuilder builder = new StringBuilder();
			int start_record_index = (page-1)*records_per_page+1;
			int stop_record_index = start_record_index+records_per_page;
			
			builder.append(upper_template);
			builder.append("<a href=\"/search/"+table_name+"\">search</a><br>");
			builder.append("<a href=\""+ exchange.getRequestURI().getPath()+"?"+Integer.toString(page-1)+"\">prev page</a>");
			builder.append("<a> - </a>");
			builder.append("<a href=\""+ exchange.getRequestURI().getPath()+"?"+Integer.toString(page+1)+"\">next page</a>");
			
			builder.append("<table border=\"1\" >");
			
			builder.append("<caption>" +table_name+ " table with id between ");
			builder.append(Integer.toString(start_record_index));
			builder.append(" and ");
			builder.append(Integer.toString(stop_record_index - 1));
			try{
				
				builder.append(". Max id: ").append(Integer.toString(ShopDb.getLastId(table_name, fields)));
				builder.append("<br>Count of records: ").append(ShopDb.getCountOfRecords(table_name));
				builder.append("</caption>");
			
				builder.append("<tr>");
				for(String field: fields){
					builder.append("<td>").append(field).append("</td>");
				}
				builder.append("</tr>");

					
				for(NormalizedRecord record:ShopDb.readTable(table_name,start_record_index,records_per_page-1)){
					builder.append("<tr>");
					for(String element:record.data){
						builder.append("<td>").append(element).append("</td>");
					}
					builder.append("<td>").append("<a href=\"/edit/"+table_name+"?"+record.id+"\">edit</a>").append("</td>");
					builder.append("<td>").append("<a href=\"/remove/"+table_name+"?"+record.id+"\">remove</a>").append("</td>");
					builder.append("</tr>");
				}
			}catch(SQLException e){
				Logger.write(e.toString());
				builder.append(e.toString());
			}
			builder.append("</table>");
			builder.append("<a href=\"/create/"+table_name+"\">add record</a><br>");
			builder.append("<a href=\""+ exchange.getRequestURI().getPath()+"?"+Integer.toString(page-1)+"\">prev page</a>");
			builder.append("<a> - </a>");
			builder.append("<a href=\""+ exchange.getRequestURI().getPath()+"?"+Integer.toString(page+1)+"\">next page</a>");
			
			
			builder.append(down_template);
			return builder.toString();
			
	}
	
	
	static class HandlerViewer implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			
			String page_str = exchange.getRequestURI().getQuery();
			int req_page = Integer.parseInt(page_str);
			if(req_page < 1){
				req_page = 1;
			}
			
			Logger.write("HandlerViewer was called. Page: " + Integer.toString(req_page));

			String table = getDestNameInPath(exchange.getRequestURI().getPath());
			String res = "";
			try{
			res = viewer(table, table_fields.get(table) ,exchange, req_page);
			
			}catch(Exception ex){
				res = ex.toString();
				Logger.write(res);
			}
			
			
			exchange.sendResponseHeaders(200, res.length());
			OutputStream os = exchange.getResponseBody();
			os.write(res.getBytes());
			os.close();
			
		}
	}
	
	
	
}