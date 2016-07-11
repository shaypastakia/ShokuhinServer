package webEngine;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.util.URIUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import recipe.Recipe;
import recipe.RecipeHTML;

@WebServlet("/shokuhin")
public class WebEngine extends HttpServlet{
	SQLEngine engine;

	public void init() throws ServletException {
		// Do required initialization
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		engine = new SQLEngine("jdbc:postgresql://localhost:5432/shokuhin", "read", "read");
	}
	
//	public void service(HttpServletRequest request, HttpServletResponse response){
//		System.out.println(request.getMethod());
//	}

	public void doGet (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("GET");
		PrintWriter out = response.getWriter();
		
		if (request.getParameterMap().containsKey("title")){
			Recipe r = engine.getRecipe(URIUtil.decode(request.getParameter("title")));
			String html = "<a href=\"javascript:history.back()\">Go Back</a>";
			html += new RecipeHTML(r).getHTML();
			out.write(html);
			out.close();
			return;
		}
		
		response.setContentType("text/html");
//		ArrayList<String> recipes = engine.getAllRecipeTitles();
		TreeMap<String, Timestamp> recipes = engine.getLastModificationDates();
		
//		Collections.sort(recipes, new Comparator<Pair<String, Timestamp>>() {
//			@Override
//			public int compare(Pair<String, Timestamp> arg0, Pair<String, Timestamp> arg1) {
//				return arg0.getKey().compareToIgnoreCase(arg1.getKey());
//			}
//	    });
		
		
		String html = "";
		html += "<h1>Welcome to Shokuhin!</h1><br /><br />";
		html += "<h2>The recipes currently on the server are:</h2>";
		
		html += "<table border=\"1\">";
		for (String s : recipes.keySet()){
			String ref = URIUtil.encodeQuery("/ShokuhinServer/shokuhin?title=" + s);
			html += "<tr><th><a href=\"" + ref + "\">" + s + "</a></th>" + "<th>(" + recipes.get(s) + ")</th>"+ "</tr>";
		}
		
		html += "</table>";
		out.write(html);
		out.close();
		return;
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("POST");
		/*Create a Session
		 *
		 *If the session is new, take the Client's MINE argument.
		 *Process it against the SQL Server to work out the NEW, UPDATE, and DELETE files
		 *Produce these three lists, as JSON Objects, produced from an ArrayList of Recipes. Empty where necessary.
		 *Send a SUCCESS/FAIL object to the Client
		 *
		 */

		/*Existing Session
		 * 
		 * If the session exists, take the Client's NEW, UPDATE, DELETE argument
		 * As per the argument, send across the NEW, UPDATE, or DELETE JSON Object
		 * Where the object is DELETE, you should also delete the session afterwards
		 * Should also be able to UPLOAD a recipe, as well as REQUEST a recipe
		 */

		//THE PHONE SHOULD ASK USER IF SHOULD UPLOAD OR DELETE, AS WELL AS ADD NEW AND UPDATE

		// Set response content type
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		request.setCharacterEncoding("UTF-8");
		String s;
		Enumeration<String> strings = request.getHeaderNames();

		while ((s = strings.nextElement()) != null)
		System.out.println(s + ": " + request.getHeader(s));
		
		ObjectMapper mapper = new ObjectMapper();
		String json;
		PrintWriter out;
		
		switch (request.getParameter("type")) {
		case "REQUEST": //Client requests a single recipe. Server sends it.
			//Decoding from http://stackoverflow.com/questions/573184/java-convert-string-to-valid-uri-object
			String rec = URIUtil.decode(request.getParameter("recipe"));
			Recipe r = engine.getRecipe(rec);
			json = mapper.writeValueAsString(r);
			out = response.getWriter();
			out.write(json);
			out.close();
			//			LOGIC + SECOND ARG (RECIPE TITLE)

			break;
		case "SENDTIMES": //Client sends its list of Recipes. Response with NEW, UPDATE, DELETE recipes as a HashMap<String, String>
			//Answer by limc, on http://stackoverflow.com/questions/5175203/httpservlet-request-getinputstream-always-receiving-blank-line
		    StringBuilder stringBuilder = new StringBuilder(1000);
		    Scanner scanner = new Scanner(request.getInputStream());
		    while (scanner.hasNextLine()) {
		        stringBuilder.append(scanner.nextLine());
		    }
		    
		    String resp = stringBuilder.toString();

		    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    mapper.setDateFormat(format);
		    TreeMap<String, Long> clientTimes = mapper.readValue(resp, new TreeMap<String, Long>().getClass());
		    TreeMap<String, String> results = getNewUpdateDelete(clientTimes);
		    
		    json = mapper.writeValueAsString(results);
		    out = response.getWriter();
		    out.write(json);
			out.close();
			break;
		default:
			break;
		}
	}

	public void destroy()
	{
		// do nothing.
	}
	
	private TreeMap<String, String> getNewUpdateDelete(TreeMap<String, Long> clientTimes){
		TreeMap<String, String> temp = new TreeMap<String, String>();
		
		TreeMap<String, Timestamp> remoteTimes = engine.getLastModificationDates();

		if (clientTimes == null || clientTimes.isEmpty())
			for (String s : remoteTimes.keySet())
				temp.put(s, "NEW");
		
		for (String s : remoteTimes.keySet()){
			if (!clientTimes.containsKey(s)){ //If the Server contains a Recipe that the Client doesn't have.
				temp.put(s, "NEW");
			} else if (clientTimes.containsKey(s)){ //If both devices have the Recipe, check for UPDATE
				
				Date serverDate = new Date(remoteTimes.get(s).getTime());
				Date clientDate = new Date(clientTimes.get(s));
				if (serverDate.after(clientDate) && !serverDate.equals(clientDate)){
					temp.put(s, "UPDATE");
				}
			}
			
		}
		
		for (String s : clientTimes.keySet()){
			if (!remoteTimes.containsKey(s)) //If the Client has a recipe that the Server doesn't have, then delete it.
				temp.put(s, "DELETE");
		}
		
		for (String s : temp.keySet()){
			System.out.println(s + ": " + temp.get(s));
		}
		return temp;
	}

}
