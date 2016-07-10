package webEngine;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import javafx.util.Pair;
import recipe.Recipe;
import recipe.RecipeMethods;

/**
 * Class used to contain methods for interfacing with SQL Server
 * @author Shaylen Pastakia
 *
 */
public class SQLEngine {
	
	private String db_url; //jdbc:mysql://www.db4free.net:3306/shokuhin
	private String user; //shokuhin
	private String pass;
	
	private Connection conn = null;
	private PreparedStatement stmt = null;

	private String sql;
	
	private enum exists{
		FAILED, NO, YES;
	}
	
	/**
	 * 
	 * @param url The SQL Database URL to connect to.
	 * @param user The username to connect with.
	 * @param pass The password to connect with.
	 */
	public SQLEngine(String url, String user, String pass){
		this.db_url = url;
		this.user = user;
		this.pass = pass;
	}
	
	/**
	 * Checks whether or not a Recipe exists on the SQL Server
	 * @param r The Recipe to check
	 * @return -1 if there was an error when checking.<br>0 if the Recipe does not exist.<br>1 if the Recipe does exist
	 */
	public exists recipeExists(Recipe r){
		if (!connect())
			return exists.FAILED;
		
		try {
			sql = "SELECT COUNT(*) AS total FROM recipes WHERE title = ?";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, r.getTitle());
			ResultSet rs = stmt.executeQuery();
			
			rs.next();
//			System.out.println(rs.getInt("total"));
			if (rs.getInt("total") > 0){
				return exists.YES;
			}
			else 
				return exists.NO;
		} catch (SQLException e){
			e.printStackTrace();
			return exists.FAILED;
		}
	}
	
	/**
	 * Add a brand new Recipe to the SQL Server
	 * @param r The Recipe to add
	 * @return True, if the method succeeds without throwing an Exception. False otherwise.
	 */
	public boolean addRecipe(Recipe r){
		if (!connect())
			return false;
		
		try {
			exists check = recipeExists(r);
			if (check == exists.YES){
				return false;
			} else if (check == exists.FAILED){
				return false;
			}
			insertRecipes(r);
			insertIngredients(r);
			insertTags(r);
			insertMethodSteps(r);
			
		    stmt.close();
//		    conn.close();
			
			return true;
		} catch (SQLException e){
			e.printStackTrace();
			return false;
		}
	}
	
	
	public boolean updateRecipe(Recipe newRec){
		
		if (!connect())
			return false;
		
//		Instant start = Instant.now();
		exists check = recipeExists(newRec);
		if (check == exists.NO){
			return false;
		} else if (check == exists.FAILED){
			return false;
		}
		
		Recipe oldRec = RecipeMethods.readRecipe(new File("./Shokuhin/Recipes/" + newRec.getTitle() + ".rec"));
		if (oldRec == null){
			return false;
		}
//		Instant end = Instant.now();
//		System.out.println(1 + " " + Duration.between(start, end));
		
		try {
//			start = Instant.now();
			//UPDATE the fields stored within the 'recipes' table.
			//UPDATE course, prepTime, cookTime, rating, servings, lastModificationDate WHERE title
			sql = "UPDATE recipes SET course = ?, prepTime = ?, cookTime = ?, rating = ?, servings = ?, lastModificationDate = ? WHERE title = ?";
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, newRec.getCourse());
			stmt.setInt(2, newRec.getPrepTime());
			stmt.setInt(3, newRec.getCookTime());
			stmt.setInt(4, newRec.getRating());
			stmt.setInt(5, newRec.getServings());
			stmt.setTimestamp(6, newRec.getLastModificationDate());
			stmt.setString(7, newRec.getTitle());
			stmt.execute();
			System.out.println("Updated 'recipes'");
//			end = Instant.now();
//			System.out.println(2 + " " + Duration.between(start, end));
			
//			start = Instant.now();
			//DELETE and INSERT ingredients if and only if the ingredients have been changed.
			if (!selectIngredients(oldRec).equals(newRec.getIngredients())){
				sql = "DELETE FROM ingredients WHERE title = ?";
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, newRec.getTitle());
				stmt.execute();
				insertIngredients(newRec);
				System.out.println("Updated 'ingredients'");
			}
			
			//DELETE and INSERT method if and only if the method have been changed.
			if (!selectMethodSteps(oldRec).equals(newRec.getMethodSteps())){
				sql = "DELETE FROM methodSteps WHERE title = ?";
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, newRec.getTitle());
				stmt.execute();
				insertMethodSteps(newRec);
				System.out.println("Updated 'methodSteps'");
			}
			
			//DELETE and INSERT tags if and only if the tags have been changed.
			if (!selectTags(oldRec).equals(newRec.getTags())){
				sql = "DELETE FROM tags WHERE title = ?";
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, newRec.getTitle());
				stmt.execute();
				insertTags(newRec);
				System.out.println("Updated 'tags'");
			}
			
//			end = Instant.now();
			
//			System.out.println(3 + " " + Duration.between(start, end));
			
			return true;
		} catch (IOException | SQLException e){
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Retrieve a Recipe in its entirety from the SQL Server
	 * @param title The name of the Recipe to retrieve
	 * @return The Recipe in Recipe format
	 */
	public Recipe getRecipe(String title){
		if (!connect())
			return null;
		
		Recipe temp = new Recipe(title);
		exists check = recipeExists(temp);
		if (check == exists.NO){
		} else if (check == exists.FAILED){
			return null;
		}
		
		try {
			ResultSet rs = selectRecipes(temp);
			rs.next();
			//course, prepTime, cookTime, rating, servings, lastModificationDate
			temp.setCourse(rs.getInt(1));
			temp.setPrepTime(rs.getInt(2));
			temp.setCookTime(rs.getInt(3));
			temp.setRating(rs.getInt(4));
			temp.setServings(rs.getInt(5));
			temp.setLastModificationDate(rs.getTimestamp(6));
			
			temp.setIngredients(selectIngredients(temp));
			temp.setTags(selectTags(temp));
			temp.setMethodSteps(selectMethodSteps(temp));
			return temp;
		} catch (IOException | SQLException e){
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Delete a Recipe from the SQL Server.
	 * <br>
	 * Deletes from the 'recipes' table, which cascades to ingredients, methodSteps, and tags.
	 * @param r The Recipe to delete from the SQL Server.
	 * @return True, if the code runs without any Exceptions.
	 */
	public boolean deleteRecipe(Recipe r){
		if (!connect())
			return false;

		try {
			sql = "DELETE FROM recipes WHERE title = ?";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, r.getTitle());
			stmt.execute();
			return true;
		} catch (SQLException e){
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Get all the recipe titles, and their last modification dates.
	 * @return A list of Pairs. Each Pair contains the name of all recipes, and the date they were last modified.
	 */
	public ArrayList<Pair<String, Timestamp>> getLastModificationDates(){
		if (!connect())
			return null;

		try {
		ArrayList<Pair<String, Timestamp>> temp = new ArrayList<Pair<String, Timestamp>>();
		
		sql = "SELECT title, lastModificationDate FROM recipes";
		stmt = conn.prepareStatement(sql);
		ResultSet result = stmt.executeQuery();
		
		while (result.next()){
			temp.add(new Pair<String, Timestamp>(result.getString(1), result.getTimestamp(2)));
		}
		
		return temp;
		} catch (SQLException e){
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get all titles of recipes stored on the SQL Server.
	 * @return An ArrayList containing the titles of every Recipe on the SQL Server.
	 */
	public ArrayList<String> getAllRecipeTitles(){
		if (!connect())
			return null;

		try {
			ArrayList<String> temp = new ArrayList<String>();
			
			sql = "SELECT title FROM recipes";
			stmt = conn.prepareStatement(sql);
			ResultSet result = stmt.executeQuery();
			
			while (result.next()){
				temp.add(result.getString(1));
			}
			
			return temp;
		} catch (SQLException e){
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Remove this method once complete
	 */
	private boolean execute(String s){
		if (!connect())
			return false;

		try {
			sql = s;
			stmt = conn.prepareStatement(sql);
			return stmt.execute();
		} catch (SQLException e){
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Helper method for add and update methods
	 * @param r 
	 * @throws SQLException
	 */
	private void insertRecipes(Recipe r) throws SQLException{
		//title, course, prepTime, cookTime, rating, servings, lastModifiedDate
		sql = "INSERT INTO recipes VALUES (?,?,?,?,?,?,?)";
		stmt = conn.prepareStatement(sql);
		stmt.setString(1, r.getTitle());
		stmt.setInt(2, r.getCourse());
		stmt.setInt(3, r.getPrepTime());
		stmt.setInt(4, r.getCookTime());
		stmt.setInt(5, r.getRating());
		stmt.setInt(6, r.getServings());
		stmt.setTimestamp(7, r.getLastModificationDate());
		stmt.execute();
	}
	
	/**
	 * Helper method for add and update methods
	 * @param r 
	 * @throws SQLException
	 */
	private void insertIngredients(Recipe r) throws SQLException{
		if (r.getIngredients().size() == 0)
			return;
		
		//Batch INSERT based on http://viralpatel.net/blogs/batch-insert-in-java-jdbc/
		sql = "INSERT INTO ingredients VALUES (?,?)";
		stmt = conn.prepareStatement(sql);
		for (String s : r.getIngredients()){
			stmt.setString(1, r.getTitle());
			stmt.setString(2, s);
			stmt.addBatch();
		}
		stmt.executeBatch();
	}
	
	/**
	 * Helper method for add and update methods
	 * @param r 
	 * @throws SQLException
	 */
	private void insertTags(Recipe r) throws SQLException{
		if (r.getTags().size() == 0)
			return;

		//Batch INSERT based on http://viralpatel.net/blogs/batch-insert-in-java-jdbc/
		sql = "INSERT INTO tags VALUES (?,?)";
		stmt = conn.prepareStatement(sql);
		for (String s : r.getTags()){
			stmt.setString(1, r.getTitle());
			stmt.setString(2, s);
			stmt.addBatch();
		}
		stmt.executeBatch();
	}
	
	/**
	 * Helper method for add and update methods
	 * @param r 
	 * @throws SQLException
	 */
	private void insertMethodSteps(Recipe r) throws SQLException{
		if (r.getMethodSteps().size() == 0)
			return;

		//Batch INSERT based on http://viralpatel.net/blogs/batch-insert-in-java-jdbc/
		sql = "INSERT INTO methodSteps VALUES (?,?)";
		stmt = conn.prepareStatement(sql);
		for (String s : r.getMethodSteps()){
//			StringReader sr = new StringReader(s);
			stmt.setString(1, r.getTitle());
//			stmt.setCharacterStream(2, sr);
			stmt.setString(2, s);
			stmt.addBatch();
		}
		stmt.executeBatch();
	}
	
	/**
	 * Read the standard details of an existing Recipe from the SQL Server
	 * @param r
	 * @return The Details as a ResultSet, in order to maintain String and Timestamp format.
	 * @throws SQLException
	 */
	private ResultSet selectRecipes(Recipe r) throws SQLException{
		if (!connect())
			return null;
		
		//SELECT course, prepTime, cookTime, rating, servings, lastModificationDate FROM recipes WHERE title
		sql = "SELECT course, prepTime, cookTime, rating, servings, lastModificationDate FROM recipes WHERE title = ?";
		stmt = conn.prepareStatement(sql);
		stmt.setString(1, r.getTitle());
		ResultSet rs = stmt.executeQuery();
		
		return rs;

	}
	
	/**
	 * Select the Ingredients of a current Recipe from the SQL Server
	 * @param r The local recipe to get the Ingredients for
	 * @return
	 * @throws SQLException
	 */
	private ArrayList<String> selectIngredients(Recipe r) throws SQLException{
		if (!connect())
			return null;
		
		sql = "SELECT ingredient FROM ingredients WHERE title = ?";
		stmt = conn.prepareStatement(sql);
		stmt.setString(1, r.getTitle());
		
		ResultSet rs = stmt.executeQuery();

		ArrayList<String> temp = new ArrayList<String>();
		while (rs.next())
			temp.add(rs.getString(1));
		
		return temp;
	}
	
	/**
	 * Select the Tags of a current Recipe from the SQL Server
	 * @param r The local recipe to get the Tags for
	 * @return
	 * @throws SQLException
	 */
	private ArrayList<String> selectTags(Recipe r) throws SQLException{
		if (!connect())
			return null;
		
		sql = "SELECT tag FROM tags WHERE title = ?";
		stmt = conn.prepareStatement(sql);
		stmt.setString(1, r.getTitle());
		
		ResultSet rs = stmt.executeQuery();

		ArrayList<String> temp = new ArrayList<String>();
		while (rs.next())
			temp.add(rs.getString(1));
		
		return temp;
	}
	
	/**
	 * Select the MethodSteps of a current Recipe from the SQL Server
	 * @param r The local recipe to get the MethodSteps for
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	private ArrayList<String> selectMethodSteps(Recipe r) throws SQLException, IOException{
		if (!connect())
			return null;
		
		sql = "SELECT methodStep FROM methodSteps WHERE title = ?";
		stmt = conn.prepareStatement(sql);
		stmt.setString(1, r.getTitle());
		
		ResultSet rs = stmt.executeQuery();

		ArrayList<String> temp = new ArrayList<String>();
		while (rs.next()){
			Reader reader;
			StringWriter writer = new StringWriter();
			
			reader = rs.getCharacterStream(1);
			int x;
			
			while((x = reader.read()) > -1){
				writer.write(x);
			}
			
			temp.add(writer.toString());
			reader.close();
			writer.close();
		}
		
		return temp;
	}
	
	/**
	 * Connect to the SQL Server
	 */
	private boolean connect(){
		if (db_url == null || db_url.equals("") || user == null){
			return false;
		}
		
		try {
			if (conn != null && conn.isValid(5))
				return true;
			
			System.out.println("Connecting");
			conn = DriverManager.getConnection(db_url, user, pass);
			return true;
			
		} catch (SQLException e){
			e.printStackTrace();
			return false;
		}
	}
}