package praktikum1;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;


public class MailFile {

	private Socket so;
	private BufferedReader inFromServer;  // Rueckmeldung vom Server
	private DataOutputStream outToServer; // An Server schicken
	private Properties prop;
	
	private static String toMailAdress;
	private static String filePath;

	private String userBase64;
	private String passwordBase64;

	public MailFile() {
		//
	}	
	
	private void loadServerData() {
		prop = new Properties();
		BufferedInputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream("src/praktikum1/usrProp"));

			prop.load(input);
		} catch (IOException e) {
			System.err.println("Fehler beim Laden der Props" + e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}
	
	private void send() throws NumberFormatException, UnknownHostException, IOException {
		userBase64 = Base64.encodeBytes(prop.getProperty("benutzer").getBytes());
		passwordBase64 = Base64.encodeBytes(prop.getProperty("passwort").getBytes());
		
		
		so = new Socket(prop.getProperty("hostname"), Integer.parseInt(prop.getProperty("port")));
		outToServer = new DataOutputStream(so.getOutputStream());
		inFromServer = new BufferedReader(new InputStreamReader(so.getInputStream()));
		
		outToServer.writeBytes("EHLO " + prop.getProperty("hostname") + "\r\n"); 
		System.err.println(inFromServer.readLine());
		outToServer.writeBytes("AUTH LOGIN");
		do{
			System.err.println(inFromServer.readLine());
			}while(inFromServer.ready());
		outToServer.writeBytes(userBase64);
		System.err.println(inFromServer.readLine());
		outToServer.writeBytes(passwordBase64);
		System.err.println(inFromServer.readLine());
		outToServer.writeBytes("MAIL From: " + prop.getProperty("mailadresse") + "\r\n");
		System.err.println(inFromServer.readLine());
		outToServer.writeBytes("RCPT To: " + toMailAdress + "\r\n");
		System.err.println(inFromServer.readLine());
		outToServer.writeBytes("DATA\r\n");
		System.err.println(inFromServer.readLine());
		outToServer.writeBytes("Subject: " + prop.getProperty("betreff") + "\r\n");
		outToServer.writeBytes("MIME-Version: 1.0\r\n");
		outToServer.writeBytes("ContentType: multipart/mixed; boundary=98766789\r\n");
		outToServer.writeBytes("\r\n");
		outToServer.writeBytes("--98766789\r\n");
		outToServer.writeBytes("Content-Transfer-Encoding: quoted-printable\r\n");
		outToServer.writeBytes("Content-Type: text/plain\r\n");
		outToServer.writeBytes("\r\n");
		outToServer.writeBytes(prop.getProperty("inhalt") + "\r\n");
		outToServer.writeBytes("--98766789\r\n");
		// TODO Anhang
		// TODO Ausgabe als log-file
	}
	
	public static void main(String[] args) {
//		toMailAdress = args[0];
//		filePath = args[1];
		MailFile mf = new MailFile();
		mf.loadServerData();
		try {
			mf.send();
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
