package praktikum1;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Properties;

public class MailFile {
	
	private Socket so;
	private BufferedReader inFromServer;  // Rueckmeldung vom Server
	private DataOutputStream outToServer; // An Server schicken
	private Properties prop;
	
	private String userBase64;
	private String passwordBase64;
	
	private String log = "";
	
	private final String BOUNDARY = "98766789";
	
	public MailFile() {
		loadServerData();
	}
	
	private void loadServerData() {
		prop = new Properties();
		BufferedInputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream("src/praktikum1/usrProp"));
			prop.load(input);
		} catch(IOException e) {
			System.err.println("Fehler beim Laden der Props" + e);
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch(IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}
	
	private String sendMail(String target, String path) {
		log = "\nLOG START::\r\n";
		File file = new File(path);
		
		try {
			userBase64 = Base64.encodeBytes(prop.getProperty("benutzer").getBytes());
			passwordBase64 = Base64.encodeBytes(prop.getProperty("passwort").getBytes());
						
			so = new Socket(prop.getProperty("hostname"), Integer.parseInt(prop.getProperty("port")));
			outToServer = new DataOutputStream(so.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(so.getInputStream()));
			
			send("EHLO " + prop.getProperty("hostname"));
			receive();
			send("AUTH LOGIN");
			receive();
			send(userBase64);
			receive();
			send(passwordBase64);
			receive();
			send("MAIL From: " + prop.getProperty("mailadresse"));
			receive();
			send("RCPT To: " + target);
			receive();
			send("DATA");
			receive();
			send("From: ");
			receive();
			send("To: ");
			send("Subject: " + prop.getProperty("betreff"));
			send("MIME-Version: 1.0");
			send("ContentType: multipart/mixed; boundary=" + BOUNDARY);
			
			send("--" + BOUNDARY);
			send("Content-Transfer-Encoding: quoted-printable");
			send("Content-Type: text/plain");
			send(prop.getProperty("inhalt"));
			send("--" + BOUNDARY);
			
			send("Content-Transfer-Encoding: base64");
			send("Content-Type: application/txt");
			send("Content-Disposition: attachment; filename=" + file.getName());
			send(Base64.encodeBytes(Files.readAllBytes(file.toPath())));
			send("--" + BOUNDARY + "--");
			
			send(".");
			send("QUIT");
			receive();
			
		} catch(NumberFormatException e) {
			System.err.println("Falscher Port");
		} catch(UnknownHostException e) {
			System.err.println("Hostname nicht gefunden");
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {
				inFromServer.close();
				outToServer.close();
				so.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		return log;
	}
	
	/**
	 * Empfängt die Antworten vom Server
	 * @throws IOException
	 */
	private void receive() throws IOException {
		do {
			String s = inFromServer.readLine(); // "Hoere" Server zu
			System.err.println("<< " + s);		// Ausgabe auf Konsole, was Server antwortet
			log += "<<" + s + "\r" + "\n";		// Schreibe log
		} while(inFromServer.ready());
	}
	
	/**
	 * Sendet die Anfragen zum Server
	 * @throws IOException
	 */
	private void send(String s) throws IOException {
		System.err.println(">> " + s);			// Ausgabe auf Konsole, was zum Server gesendet wird
		outToServer.writeBytes(s + "\r" + "\n");// "Spreche" mit Server
		log += ">>" + s + "\r" + "\n";			// Schreibe log
	}
	
	/**
	 * 
	 * @param args[0] = Empfaenger-Mailadresse, args[1] = Dateianhang-Pfad 
	 */
	public static void main(String[] args) {
		MailFile mf = new MailFile();
		String log = mf.sendMail(args[0], args[1]);
		try {
			File logFile = new File(args[1] + "/log.txt");
			FileWriter writer = new FileWriter(logFile);
			writer.write(log);
			writer.flush();
			writer.close();
		} catch(IOException e) {
			System.err.println("Fehler bei log-Erstellung");
		} 
	}
}
