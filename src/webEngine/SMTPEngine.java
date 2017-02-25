package webEngine;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import recipe.Recipe;

public class SMTPEngine {
	
	//code from http://crunchify.com/java-mailapi-example-send-an-email-via-gmail-smtp/
	private Properties prop;
	private Session session;
	private MimeMessage message;
	
	private String user;
	private String pass;
	
	public SMTPEngine(String user, String pass){
		this.user = user;
		this.pass = pass;
		
		//code from http://crunchify.com/java-mailapi-example-send-an-email-via-gmail-smtp/
		prop = System.getProperties();
		prop.put("mail.smtp.port", "587");
		prop.put("mail.smtp.auth", "true");
		prop.put("mail.smtp.starttls.enable", "true");
	}
	
	public void sendRecipe(Recipe r, String recipient) throws AddressException, MessagingException{
		
		//code from http://crunchify.com/java-mailapi-example-send-an-email-via-gmail-smtp/
		session = Session.getDefaultInstance(prop, null);
		message = new MimeMessage(session);
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
		
		String body = "Hi";
		message.setContent(body, "text/html");
		
		Transport transport = session.getTransport("smtp");
		 
		//code from http://crunchify.com/java-mailapi-example-send-an-email-via-gmail-smtp/
		transport.connect("smtp.gmail.com", user, pass);
		transport.sendMessage(message, message.getAllRecipients());
		transport.close();
	}

}
