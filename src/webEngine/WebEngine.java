package webEngine;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.util.URIUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.util.Pair;
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
		ArrayList<Pair<String, Timestamp>> recipes = engine.getLastModificationDates();
		
		Collections.sort(recipes, new Comparator<Pair<String, Timestamp>>() {
			@Override
			public int compare(Pair<String, Timestamp> arg0, Pair<String, Timestamp> arg1) {
				return arg0.getKey().compareToIgnoreCase(arg1.getKey());
			}
	    });
		
		
		String html = "";
		html += "<h1>Welcome to Shokuhin!</h1><br /><br />";
		html += "<h2>The recipes currently on the server are:</h2>";
//		html += "<ul>";
//		
//		
//		for (Pair<String, Timestamp> s : recipes){
//			String ref = URIUtil.encodeQuery("/ShokuhinServer/shokuhin?title=" + s.getKey());
//			html += "<li><a href=\"" + ref + "\">" + s.getKey() + "</a>" + " (" + s.getValue() + ") "+ "</li>";
//		}
//		
//		html += "</ul>";
		
		html += "<table border=\"1\">";
		for (Pair<String, Timestamp> s : recipes){
			String ref = URIUtil.encodeQuery("/ShokuhinServer/shokuhin?title=" + s.getKey());
			html += "<tr><th><a href=\"" + ref + "\">" + s.getKey() + "</a></th>" + "<th>(" + s.getValue() + ")</th>"+ "</tr>";
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
		
		ObjectMapper mapper = new ObjectMapper();
		String json;
		PrintWriter out;
		
		switch (request.getParameter("type")) {
		case "MINE":	
			//			Logic
			
			json = mapper.writeValueAsString(engine.getAllRecipeTitles());
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8")), true);
			out.write(json);
			out.close();
			break;
		case "NEW":
			//			Logic

			break;
		case "UPDATE":
			//			Logic

			break;
		case "REQUEST":
			//Decoding from http://stackoverflow.com/questions/573184/java-convert-string-to-valid-uri-object
			String rec = URIUtil.decode(request.getParameter("recipe"));
			Recipe r = engine.getRecipe(rec);
			json = mapper.writeValueAsString(r);
			out = response.getWriter();
			out.write(json);
			out.close();
			//			LOGIC + SECOND ARG (RECIPE TITLE)

			break;
		case "SENDTIMES":
			//			LOGIC + Ability to retrieve a file from the client.
			String temp;
			String resp;
			BufferedReader bufRead = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
			String line;
	        StringBuffer req = new StringBuffer();
	        while ((line = bufRead.readLine()) != null) {
	            req.append(line);
	            req.append('\r');
	        }
	        bufRead.close();
	        System.out.println(request.getParameter("type"));
			break;
		case "DELETE":
			//			Logic

			break;
		default:
			break;
		}
	}

	public void destroy()
	{
		// do nothing.
	}

}
