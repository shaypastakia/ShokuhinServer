package webEngine;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.httpclient.util.URIUtil;

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
	
	public void sendRecipe(Recipe r, String recipient) throws AddressException, MessagingException, IOException{
		
		//code from http://crunchify.com/java-mailapi-example-send-an-email-via-gmail-smtp/
		session = Session.getDefaultInstance(prop, null);
		message = new MimeMessage(session);
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
		
		String bodyText = "To view this recipe on the Shokuhin website, please click this link:\n"
				+ "<a href=\"http://www.shokuhin.herokuapp.com/shokuhin?title="
				+ URIUtil.encodeAll(r.getTitle())
				+ "\">"
				+ r.getTitle() + "</a><br /><br />"
				+ "You will also find the recipe attached to this email.";
		message.setSubject("Recipe for " + r.getTitle());
//		message.setContent(bodyText, "text/html");
		
		BodyPart bodyPart = new MimeBodyPart();
		bodyPart.setContent(bodyText, "text/html");
		Multipart multi = new MimeMultipart();
		multi.addBodyPart(bodyPart);
		
		BodyPart attachment = new MimeBodyPart();
		
		File tempFile = File.createTempFile(r.getTitle(),".html");
		tempFile.deleteOnExit();
		
		OutputStream fileOut = Files.newOutputStream(tempFile.toPath());
		BufferedOutputStream buff = new BufferedOutputStream(fileOut);
		ObjectOutputStream obj = new ObjectOutputStream(buff);
		obj.writeObject(r);
		obj.close();
		buff.close();
		fileOut.close();
		
		attachment.setDataHandler(new DataHandler(new FileDataSource(tempFile)));
		attachment.setFileName(r.getTitle() + ".html");
		
		multi.addBodyPart(attachment);
		
		message.setContent(multi);
		
		Transport transport = session.getTransport("smtp");
		 
		//code from http://crunchify.com/java-mailapi-example-send-an-email-via-gmail-smtp/
		transport.connect("smtp.gmail.com", user, pass);
		transport.sendMessage(message, message.getAllRecipients());
		transport.close();
	}

}
