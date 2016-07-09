package recipe;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javafx.util.Pair;

/**
 * 
 * @author Shaylen Pastakia
 *
 */
public class RecipeMethods {
	
	/**
	 * Write a Recipe to the hard drive, allowing it to be persistently stored.
	 * @param rec The Recipe to write to disk
	 * @param file The file representing the file path to be written to
	 * @return True if the file write succeeded
	 */
	public static boolean writeRecipe(Recipe rec){
		File file = new File("./Shokuhin/Recipes/" + rec.getTitle() + ".rec");
		
		String filePath = file.getAbsolutePath();
		String[] split = filePath.split("\\\\");
		int end = split.length-1;
		String fileDir = "";
		for (int i = 0; i < end; i++)
			fileDir += split[i] + "\\";
		
		Path fullFile = new File(filePath).toPath();
		Path dirFile = new File(fileDir).toPath();
		
		try {
			Files.createDirectories(dirFile);
			Files.createFile(fullFile);
			OutputStream fileOut = Files.newOutputStream(fullFile);
			BufferedOutputStream buff = new BufferedOutputStream(fileOut);
			ObjectOutputStream obj = new ObjectOutputStream(buff);
			obj.writeObject(rec);
			obj.close();
			buff.close();
			fileOut.close();
			return true;
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
		
	}
	
	/**
	 * Read a Recipe from the hard drive.
	 * @param file The file representing the file path to be read as a Recipe object
	 * @return The Recipe read in from the specified file. Returns null if the recipe cannot be read.
	 */
	public static Recipe readRecipe(File file){
		try {
			InputStream fileIn = Files.newInputStream(file.toPath(), StandardOpenOption.READ);
			BufferedInputStream buff = new BufferedInputStream(fileIn);
			ObjectInputStream obj = new ObjectInputStream(buff);
			Recipe rec = (Recipe) obj.readObject();
			obj.close();
			fileIn.close();
			return rec;
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static ArrayList<Recipe> readAllRecipes(){
		ArrayList<Recipe> temp = new ArrayList<Recipe>();
		for (String s : getRecipeFileNames()){
			temp.add(readRecipe(new File("./Shokuhin/Recipes/" + s + ".rec")));
		}
		
		return temp;
	}
	
	/**
	 * Delete a recipe from the Shokuhin directory
	 * @param recipe The Recipe to delete
	 * @return True, if the method succeeds without exception
	 */
	public static boolean deleteRecipe(Recipe recipe){
		File file = new File("./Shokuhin/Recipes/" + recipe.getTitle() + ".rec");
		try {
			return Files.deleteIfExists(file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Get all Filenames from the Recipes folder
	 * @return An ArrayList of Strings, containing the Recipe names (e.g. "Victoria Sponge")
	 */
	public static ArrayList<String> getRecipeFileNames(){
		File parent = new File("./Shokuhin/Recipes/");
		ArrayList<String> files = new ArrayList<String>();
		for (File file: parent.listFiles()){
			if (file.isFile() && file.getAbsolutePath().endsWith(".rec"))
				files.add(file.getName().replaceAll(".rec", ""));
		}
		
		return files;
	}
	
	@Deprecated
	public static ArrayList<String> getRemoteRecipeFileNames(String url){
		try {
            ArrayList<String> temp = new ArrayList<String>();
            ArrayList<String> temp2 = new ArrayList<String>();
            Document doc = Jsoup.connect(url).get();
            for (Element file : doc.select("a")) {
                temp.add(file.attr("href"));
            }

            for (String s : temp){
                if (s.endsWith(".rec")){
                    String s2 = s.replaceAll("%20", " ").replaceAll(".rec", "");
                    temp2.add(s2);
                }
            }
            temp.clear();
            return temp2;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
	}
	
	/**
	 * Produce a list of all recipes, as well as their date of last modification.
	 * @return An ArrayList of Pairs, where the Pair contains the Recipe Title, and the LastModificationDate
	 */
	public static ArrayList<Pair<String, Timestamp>> getLastModificationDates(){
		ArrayList<Recipe> recs = readAllRecipes();
		ArrayList<Pair<String, Timestamp>> temp = new ArrayList<Pair<String, Timestamp>>();
		
		for (Recipe r : recs)
			temp.add(new Pair<String, Timestamp>(r.getTitle(), r.getLastModificationDate()));
		
		return temp;
	}
	
	/**
	 * Attempt to parse a Recipe from BBC Good Food or All Recipes
	 * @param url The full URL of the Recipe to parse
	 * @return the Recipe parsed from the URL
	 */
	public static Recipe parseRecipeUrl(String url){
		if (url == null){
			return null;
		}
		//Don't proceed if the link isn't a valid Recipe URL
		if (!url.contains("recipe")){
			System.out.println("Invalid URL. Cannot parse from " + url + "\n" + "Please use a recipe from http://www.bbcgoodfood.com or http://allrecipes.co.uk");
			return null;
		}
		
		//Add protocol if it isn't already there
		if(!url.startsWith("http://"))
			url = "http://www.bbcgoodfood.com".concat(url);
		
		try {
			System.out.println(url);
			//Connect to the Recipe URL
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestProperty("User-Agent", "Chrome");
			
			//Get the resulting page 
			InputStreamReader in = new InputStreamReader(connection.getInputStream());
			BufferedReader bufIn = new BufferedReader(in);
			String temp;
			String response = "";
			
			//Write the page into a String
			while ((temp = bufIn.readLine()) != null){
				response = response.concat(temp);
			}
			bufIn.close();
			//Produce a HTML Document from the response
			Document doc = Jsoup.parse(response);
			if(url.contains("bbcgoodfood"))
				return parseBBCGoodFood(doc);
			else if(url.contains("bbc"))
				return parseBBC(doc);
			else if(url.contains("allrecipes"))
				return parseAllRecipes(doc);
			else
				return null;
		} catch (Exception e){
			System.out.println(e.getMessage());
			return null;
		}
	}
	
	private static Recipe parseBBCGoodFood(Document doc){
		Recipe parsedRecipe = new Recipe("");
		
		try {
			//Get the title of the Recipe
			Elements titleElement = doc.getElementsByAttributeValue("itemprop", "name");
			String titleElementText = titleElement.get(0).text();
			parsedRecipe.setTitle(new String(titleElementText.getBytes(), "UTF-8"));
			
			//Get the rating of the Recipe
			String ratingValue = doc.getElementsByAttributeValue("itemprop", "ratingValue").get(0).attr("content");
			parsedRecipe.setRating((int) Math.round(Double.parseDouble(ratingValue)));
			
			//Get the ingredients of the Recipe
			Elements ingredientsElement = doc.getElementsByClass("ingredients-list__item");
			ArrayList<String> ingredients = new ArrayList<String>();
			for (Element e : ingredientsElement){
				if (e.getAllElements().size() == 1){
					ingredients.add(new String(e.text().replaceAll("\\. ", "\\.\n").getBytes(), "UTF-8"));
				} else if (e.getAllElements().size() > 1){
					String temp = "";
					Elements elems = e.getAllElements();
					for (Element elem : elems){
						if (elem.children().size() != 0)
							temp += elem.ownText().trim();
						else {
							//Parse Ingredients differently, depending on whether Chef Recipe or User Recipe
							Element recipeType = doc.getElementById("main-content");
							if (recipeType.attr("class").contains("user"))
								temp += " " + elem.select("span").text().trim();
							else 
								temp += " " + elem.select("a.ingredients-list__glossary-link").text().trim();
						}
					}
					ingredients.add(new String(temp.replaceAll("\\. ", "\\.\n").getBytes(), "UTF-8"));
				}
				
			}
			parsedRecipe.setIngredients(ingredients);
			
			//Get the method steps of the Recipe
			Elements methodElement = doc.getElementsByAttributeValue("itemprop", "recipeInstructions");
			ArrayList<String> methodSteps = new ArrayList<String>();
			for (Element e : methodElement){
				methodSteps.add(new String(e.text().replaceAll("\\. ", "\\.\n").getBytes(), "UTF-8"));
			}
			parsedRecipe.setMethodSteps(methodSteps);
					
			return parsedRecipe;
		} catch (Exception e){
			return null;
		}
	}
	
	private static Recipe parseAllRecipes(Document doc){
		Recipe parsedRecipe = new Recipe("");
		
		try {
			//Get the title of the Recipe
			Elements titleElement = doc.getElementsByAttributeValue("itemprop", "name");
			String titleElementText = titleElement.get(0).text();
			parsedRecipe.setTitle(new String(titleElementText.getBytes(), "UTF-8"));
			
			//Get the rating of the Recipe
			Element ratingElement = doc.getElementById("starRating");
			String[] ratingHTML = ratingElement.getAllElements().get(0).html().split("rating");
			int ratingRaw = Integer.parseInt(ratingHTML[1].split("\"")[0]);
			if (ratingRaw <= 5){
				parsedRecipe.setRating(ratingRaw);
			} else {
				parsedRecipe.setRating((ratingRaw+5)/10);
			}
			
			//Get the ingredients of the Recipe
			Elements ingredientsElement = doc.getElementsByAttributeValue("itemprop", "ingredients");
			ArrayList<String> ingredients = new ArrayList<String>();
			for (Element e : ingredientsElement){
				ingredients.add(new String(e.text().getBytes(), "UTF-8"));
			}
			parsedRecipe.setIngredients(ingredients);

			//Get the method steps of the Recipe
			Elements methodElement = doc.getElementsByAttributeValue("itemprop", "recipeInstructions");
			doc = Jsoup.parse(methodElement.html());
			methodElement = doc.select("span");
			ArrayList<String> methodSteps = new ArrayList<String>();
			for (Element e : methodElement){
				methodSteps.add(new String(e.text().replaceAll("\\. ", "\\.\n").getBytes(), "UTF-8"));
			}
			parsedRecipe.setMethodSteps(methodSteps);

			return parsedRecipe;
		} catch (Exception e){
			return null;
		}
	}
	
	private static Recipe parseBBC(Document doc){
		Recipe parsedRecipe = new Recipe("");
		
		try {
			//Get the title of the Recipe
			Elements titleElement = doc.getElementsByClass("content-title__text");
			String titleElementText = titleElement.get(0).text();
			parsedRecipe.setTitle(new String(titleElementText.getBytes(), "UTF-8"));
			
			//Get the ingredients of the Recipe
			Elements ingredientsElement = doc.getElementsByClass("recipe-ingredients-wrapper");
			ingredientsElement = Jsoup.parse(ingredientsElement.html()).select("li");
			ArrayList<String> ingredients = new ArrayList<String>();
			for (Element e : ingredientsElement){
				ingredients.add(new String(e.text().replaceAll("\\. ", "\\.\n").getBytes(), "UTF-8"));
			}
			parsedRecipe.setIngredients(ingredients);
			
			//Get the method steps of the Recipe
			Elements methodElement = doc.getElementsByClass("recipe-method-wrapper");
			methodElement = Jsoup.parse(methodElement.html()).select("li");
			ArrayList<String> methodSteps = new ArrayList<String>();
			for (Element e : methodElement){
					Elements el = e.select("p");
					System.out.println(el.get(0).ownText());
					methodSteps.add(new String(el.get(0).ownText().replaceAll("\\. ", "\\.\n").getBytes(), "UTF-8"));
				}
			parsedRecipe.setMethodSteps(methodSteps);

			return parsedRecipe;
		} catch (Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	

	/**
	 * Export a single recipe in HTML format
	 * @param recipe
	 */
	public static void exportHTML(Recipe recipe){
		JFileChooser chooser = new JFileChooser();
		chooser.showSaveDialog(null);
		File saveFile = chooser.getSelectedFile();
		
		if (saveFile == null)
			return;
		
		if (!saveFile.toString().endsWith(".html"))
			saveFile = new File(saveFile.toString() + ".html");
		
		try {
			Files.createFile(saveFile.toPath());
			Files.write(saveFile.toPath(), new RecipeHTML(recipe).getHTML().getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			return;
		}
		
	}
	
	/**
	 * Export all Recipes to HTML
	 */
	public static void exportAll(){
		try {
		for (String s : getRecipeFileNames()){
			Recipe r = readRecipe(new File("./Shokuhin/Recipes/" + s + ".rec"));
			Files.createFile(new File("C://Users/Shaylen/Desktop/Recipes/" + s + ".html").toPath());
			Files.write(new File("C://Users/Shaylen/Desktop/Recipes/" + s + ".html").toPath(), new RecipeHTML(r).getHTML().getBytes(), StandardOpenOption.CREATE);
		}
		} catch (Exception e){
			System.out.println("Failed\n" + e.getMessage());
		}
	}
}
