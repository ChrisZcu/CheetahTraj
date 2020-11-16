package vqgs.util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * This class is used to remind the status of the experiments.
 * Set {@link #to} to empty string to prevent sending.
 */
public class EmailSender {
	private static final String to = "";
	private static final String from = "";
	private static final String authCode = "";
	private static final String host = "smtp.qq.com";


	public static void sendEmail() {
		if (!PSC.SEND_EMAIL || to.isEmpty()) {
			System.out.println("\nNot to send the email.");
			return;
		}

		System.out.println("\nBegin to send the email...");

		Properties properties = System.getProperties();

		properties.setProperty("mail.smtp.host", host);
		properties.put("mail.smtp.auth", "true");

		Session session = Session.getDefaultInstance(properties, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(from, authCode);
			}
		});

		try{
			MimeMessage message = new MimeMessage(session);

			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO,
					new InternetAddress(to));

			if (!WF.error) {
				message.setSubject("Run Success !");
				message.setText(PSC.str());
			} else {
				message.setSubject("Run Fail !");
				message.setText("status = " + WF.status + "\n" + PSC.str());
			}

			Transport.send(message);
			System.out.println("Sent email successfully");
		} catch (MessagingException mex) {
			System.out.println("Failed to send email");
			mex.printStackTrace();
		}
	}

	public static void main(String [] args) {
		// inner test
		sendEmail();
	}
}
