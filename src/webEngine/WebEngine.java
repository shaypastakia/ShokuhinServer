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

	private static final long serialVersionUID = 8186358153531802098L;
	SQLEngine engine;
	public static final String image = "coffee.jpg";
	
	//Fade delay by http://devinvinson.com/delayed-fade-in-effects-with-css/ AND http://stackoverflow.com/questions/18265846/css-animation-delay-not-working
	public static final String style = "<style>"
			+ "@keyframes fadein {from { opacity: 0; }to   { opacity: 0.85; }}"
			+ "@-moz-keyframes fadein {from { opacity: 0; }to   { opacity: 0.85; }}"
			+ "@-webkit-keyframes fadein {from { opacity: 0; }to   { opacity: 0.85; }}"
			+ "@-ms-keyframes fadein {from { opacity: 0; }to   { opacity: 0.85; }}"
			+ "@-o-keyframes fadein {from { opacity: 0; }to   { opacity: 0.85; }}"
			
			+ "div {"
			+ "font-family: Sans-Serif;"
			+ "font-size: 22;"
			+ "}"
			
			+ " .one, .two, .three {"
			+ "opacity: 0;"
			+ "background: rgba(200, 200, 200, 0.85); "
			+ "-moz-animation: fadein 1s ease-in forwards; "
			+ "-webkit-animation: fadein 1s ease-in forwards; "
			+ "-o-animation: fadein 1s ease-in forwards; "
			+ "}"
			+ "</style>";
	public static final String style2 = "<style> .a, .b, .c {"
			+ "-moz-animation: fadein 1s ease-in forwards; "
			+ "-webkit-animation: fadein 1s ease-in forwards; "
			+ "-o-animation: fadein 1s ease-in forwards; "
			+ "}"
			+ "@keyframes fadein {from { opacity: 0; }to   { opacity: 1; }}"
			+ "@-moz-keyframes fadein {from { opacity: 0; }to   { opacity: 1; }}"
			+ "@-webkit-keyframes fadein {from { opacity: 0; }to   { opacity: 1; }}"
			+ "@-ms-keyframes fadein {from { opacity: 0; }to   { opacity: 1; }}"
			+ "@-o-keyframes fadein {from { opacity: 0; }to   { opacity: 1; }}"
			+ "</style>";
	public static final String style3 = "<style>"
			+ ".one, .a {-moz-animation-delay: 0s;-webkit-animation-delay: 0s;-o-animation-delay: 0s;animation-delay: 0s;}"
			+ ".two, .b {-moz-animation-delay: 0.75s;-webkit-animation-delay: 0.75s;-o-animation-delay: 0.75s;animation-delay: 0.75s;}"
			+ ".three, .c {-moz-animation-delay: 1.5s;-webkit-animation-delay: 1.5s;-o-animation-delay: 1.5s;animation-delay: 1.5s;}";

	public void init() throws ServletException {
		// Do required initialization
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		engine = new SQLEngine("jdbc:postgresql://localhost:5432/shokuhin", "read", "read");
//		engine = new SQLEngine("jdbc:postgresql://127.13.121.130:5432/recipes", "shokuhin", "shokuhin");
	}

	public void doGet (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("------------------------------");
		System.out.println("GET");
		String s2;
		Enumeration<String> strings = request.getHeaderNames();
		while ((s2 = strings.nextElement()) != null)
		System.out.println(s2 + ": " + request.getHeader(s2));
	    
		PrintWriter out = response.getWriter();
		
		Recipe showRec = engine.getMostRecentRecipe();
		
		if (request.getParameterMap().containsKey("print")){
			Recipe r = engine.getRecipe(URIUtil.decode(request.getParameter("title")));
			String html = "<a href=\"javascript:history.back()\">Go Back</a>";
			html += new RecipeHTML(r).getHTML();
			out.write(html);
			out.close();
			return;
		}
		
		if (request.getParameterMap().containsKey("title")){
			showRec = engine.getRecipe(request.getParameter("title"));
		}
		
		System.out.println(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()) + ": " + request.getParameter("title"));
		response.setContentType("text/html");
		TreeMap<String, Timestamp> recipes = engine.getLastModificationDates();		
		
		String html = "";
		html += "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
		"<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>" + style + style2 + style3		
		
		+ "<meta content=\"en-gb\" http-equiv=\"Content-Language\" />" + 
		"<meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />" + 
		"<title>Shokuhin</title><style type=\"text/css\">.auto-style2 {font-family: \"DejaVu Sans\";color:black;border-collapse: collapse}" +
		"</style></head><body background=" + request.getContextPath() + "/images/" + image + " link=\"#000000\" vlink=\"#000000\" alink=\"#000000\" bgcolor=\"#DDDDDD\">"
		
		+ "<div class=\"one\"><div class=\"a\"><table style=\"width: 100%; border-collapse: collapse; \">"
		
		+ "<tr >" + //table row containing header (image + heading)
		
		"<td style=\"width: 126px\"><span class=\"auto-style2\"><img alt=\"Shokuhin Logo\" height=\"87\" src=" + request.getContextPath() + "/images/ShokuhinLogo.png style=\"float: left\" width=\"109\" /></span></td>" + 
		
		"<th><h1><span class=\"auto-style2\">Welcome to Shokuhin</span></h1></th></tr></table></div></div>";
		
		html += "<table style=\"height: 10px\"><tr></tr></table>";
		
		html += "<div class=\"two\" style=\"float:left\"> <div class=\"b\"><table border=\"0\">";
		
		for (String s : recipes.keySet()){
			String ref = URIUtil.encodeQuery("/shokuhin?title=" + s);

			html += "<tr><td onMouseOver=\" this.style.backgroundColor='rgba(200, 200, 200, 1)' \" onMouseOut=\" this.style.backgroundColor='rgba(200, 200, 200, 0)' \"><b><a style=\"text-decoration:none;\" href=\"" + ref + "\">" + s + "</a></b></td>" + "</tr>";
		}
		
		engine.getMostRecentRecipe();
		html += "</table></div></div>" + "<div style=\"float:left\"><table style=\"width: 50px\"><td><tr></tr></td></table></div>";
		
		html += "<div class=\"three\" style=\"float:left;\"><div class=\"c\"><table style=\"width: 100%; box-shadow: 0 4px 8px 5px rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);\" border=\"0\"><tr><td style=\"color:black;\"><b><a href=\"/shokuhin?print=true&title=" + showRec.getTitle() + "\">Print Recipe</b></tr></td>"
				+ "<td>" +	new RecipeHTML(showRec).getHTML() + "</td></table></div></div>";
		
		html += "</body></html>";
		out.write(html);
		out.close();
		return;
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("------------------------------");
		System.out.println("POST");

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
		case "ONLINE":
			response.setContentType("text/plain");
			out = response.getWriter();
			out.write("true");
			out.close();
			System.out.println(new SimpleDateFormat("dd/MM/yyy HH:mm:ss").format(new Date()) + " - Checked online status");
			break;
		case "REQUEST": //Client requests a single recipe. Server sends it.
			//Decoding from http://stackoverflow.com/questions/573184/java-convert-string-to-valid-uri-object
			String rec = URIUtil.decode(request.getParameter("recipe"));
			Recipe r = engine.getRecipe(rec);
			json = mapper.writeValueAsString(r);
			out = response.getWriter();
			out.write(json);
			out.close();
			System.out.println(new SimpleDateFormat("dd/MM/yyy HH:mm:ss").format(new Date()) + " - Requested: " + r.getTitle());
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
			System.out.println(new SimpleDateFormat("dd/MM/yyy HH:mm:ss").format(new Date()) + " - Responding with NEW, UPDATE, DELETE");
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
