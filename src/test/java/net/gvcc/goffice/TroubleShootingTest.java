package net.gvcc.goffice;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.TestPropertySources;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import net.gvcc.goffice.troubleshooting.actions.EmailServiceImpl;

/**
 *
 * <p>
 * The <code>Application</code> class
 * </p>
 * <p>
 * Data: 28 mar 2022
 * </p>
 * 
 * @author <a href="mailto:edv@gvcc.net"></a>
 * @version 1.0
 */
@TestPropertySources({ @TestPropertySource("classpath:application.properties") })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({ SimpleMailMessage.class, EmailServiceImpl.class })
@TestInstance(Lifecycle.PER_CLASS)
public class TroubleShootingTest {
	@Autowired
	private EmailServiceImpl emailService;

	private GreenMail smtpServer;

	@BeforeAll
	public void setUp() {
		// Start the mock SMTP server
		ServerSetup setup = new ServerSetup(8000, "localhost", "smtp");
		smtpServer = new GreenMail(setup);
		smtpServer.start();
	}

	@AfterAll
	public void tearDown() {
		// Stop the mock SMTP server
		smtpServer.stop();
	}

	@Test
	public void sendemailTest() {
		String testoMessaggio = "Send simple email ";
		Assertions.assertDoesNotThrow(() -> emailService.sendMessage((Message) null /* event */, testoMessaggio, null));
	}

	@Test
	public void sendemailTestWithAttachments() {
		String testoMessaggio = "Send email with attachments";
		// Message message = createFakeRequest();
		Message message = new Message(testoMessaggio.getBytes());
		Assertions.assertDoesNotThrow(() -> emailService.sendMessage(message, testoMessaggio, null));
	}
}
