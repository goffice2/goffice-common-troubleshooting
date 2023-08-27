/*
 * goffice... 
 * https://www.goffice.org
 * 
 * Copyright (c) 2005-2022 Consorzio dei Comuni della Provincia di Bolzano Soc. Coop. <https://www.gvcc.net>.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.gvcc.goffice.troubleshooting.actions;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import com.google.gson.Gson;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.gvcc.goffice.email.EMailSendHelper;

/**
 *
 * <p>
 * The <code>EmailManager</code> class
 * </p>
 * <p>
 * this class contains the management logic of the troubleshooting sub-processes to be activated starting from the business tasks or sub-processes that have gone into error
 * </p>
 * <p>
 * the sendmessage method receives as input the message coming from the queue and for which the troubleshooting notification is being sent: the message text is defined in the propertes file of the
 * reference microservice; and the list of attachments are the documents relating to the process that went into error and which will be attached to the troubleshooting communication/email
 * </p>
 * <p>
 * Data: 2 nov 2022
 * </p>
 * 
 * @author <a href="mailto:edv@gvcc.net"></a>
 * @version 1.0
 */

@Import({ SimpleMailMessage.class, EMailSendHelper.class })
@Configuration
public class EmailServiceImpl implements EmailService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

	@Autowired
	private EMailSendHelper emailSender;

	@Autowired
	private SimpleMailMessage template;

	@Value("${goffice.common.troubleshooting.mail.from}")
	private String mailFrom;
	@Value("${goffice.common.troubleshooting.mail.to}")
	private String mailTo;
	@Value("${goffice.common.troubleshooting.mail.cc: }")
	private String mailCc;
	@Value("${goffice.common.troubleshooting.mail.bcc: }")
	private String mailBcc;
	@Value("${goffice.common.troubleshooting.mail.subject}")
	private String mailSubject;
	@Value("${goffice.common.troubleshooting.mail.body}")
	private String mailBody;

	@Override
	public String getDefaultBody() {
		return mailBody;
	}

	/*
	 * the sendmessage method receives as input the message coming from the queue and for which the troubleshooting notification is being sent: the message text is defined in the propertes file of the
	 * reference microservice; and the list of attachments are the documents relating to the process that went into error and which will be attached to the troubleshooting communication/email
	 * (non-Javadoc)
	 * 
	 * @see net.gvcc.goffice.troubleshooting.actions.TroubleshootingService#sendMessage(org.springframework.amqp.core.Message, java.lang.String, java.util.Map)
	 */
	@Override
	public boolean sendMessage(Message event, String message, Map<String, InputStreamSource> attachments) {
		return sendMessageWithBody(event, mailBody + ": " + message, attachments);
	}

	@Override
	public boolean sendMessage(String subject, String body, Map<String, InputStreamSource> attachments) {
		return sendMessageWithBody(subject, body, attachments);
	}

	/*
	 * the sendmessage method receives as input the message coming from the queue and for which the troubleshooting notification is being sent: the message text is defined in the propertes file of the
	 * reference microservice; and the list of attachments are the documents relating to the process that went into error and which will be attached to the troubleshooting communication/email
	 * (non-Javadoc)
	 * 
	 * @see net.gvcc.goffice.troubleshooting.actions.TroubleshootingService#sendMessage(org.springframework.amqp.core.Message, java.lang.String, java.util.Map)
	 */
	@Override
	public boolean sendMessageWithBody(Message event, String message, Map<String, InputStreamSource> attachments) {
		LOGGER.info("sendMessage - START");

		boolean error = true;

		try {
			if (event != null || (attachments != null && !attachments.isEmpty())) {
				String json = event == null ? null : new Gson().toJson(event);
				error = sendMessageWithAttachment(message, json, attachments);
			} else {
				error = sendSimpleMessage(message);
			}
		} catch (Exception ex) {
			LOGGER.error("sendMessage - error sending troubleshooting email", ex);
		}

		LOGGER.info("sendMessage - error={}", error);
		LOGGER.info("sendMessage - END");

		return error;
	}

	@Override
	public boolean sendMessageWithBody(String subject, String body, Map<String, InputStreamSource> attachments) {
		LOGGER.info("sendMessage - START");

		boolean error = true;

		try {
			if (attachments != null && !attachments.isEmpty()) {
				error = sendMessageWithAttachment(subject, body, null, attachments);
			} else {
				error = sendSimpleMessage(subject, body);
			}
		} catch (Exception ex) {
			LOGGER.error("sendMessage - error sending troubleshooting email", ex);
		}

		LOGGER.info("sendMessage - error={}", error);
		LOGGER.info("sendMessage - END");

		return error;
	}

	@Override
	public boolean sendSimpleMessage(String body) {
		LOGGER.info("sendSimpleMessage - START");

		boolean error = sendSimpleMessage(mailSubject, body);

		LOGGER.info("sendSimpleMessage - END");

		return error;
	}

	private boolean sendSimpleMessage(String subject, String body) {
		LOGGER.info("sendSimpleMessage - START");

		boolean error = true;

		try {
			String[] destinations = createListDests(mailTo);
			String[] ccs = createListDests(mailCc);
			String[] bccs = createListDests(mailBcc);

			int addressCount = destinations.length + ccs.length + bccs.length;
			if (addressCount == 0) {
				throw new MailSendException("No targets defined for email message! (subject=".concat(subject).concat(")"));
			}

			error = !emailSender.sendMessage(destinations, ccs, bccs, mailFrom, subject, body, null);
		} catch (Exception e) {
			LOGGER.error("sendSimpleMessage - error during sending simple mail", e);
		}

		LOGGER.info("sendSimpleMessage - error={}", error);
		LOGGER.info("sendSimpleMessage - END");

		return error;
	}

	/**
	 * @param addresses
	 * @return String[]
	 */
	private String[] createListDests(String addresses) {
		String[] array = {};
		if (StringUtils.isNotBlank(addresses)) {
			array = StringUtils.defaultString(addresses).replaceAll("\s+", "").split("[,;]");
		}
		return array;
	}

	@Override
	@SuppressFBWarnings(value = { "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE" }, justification = "Checks implemented")
	public boolean sendSimpleMessageUsingTemplate(String... templateModel) {
		LOGGER.info("sendSimpleMessageUsingTemplate - START");

		boolean error = true;

		try {
			if (template == null || templateModel == null) {
				throw new Exception("Template text null");
			}

			String templateText = template.getText();
			if (templateText == null) {
				throw new Exception("Template content is null");
			}

			String text = String.format(template.getText(), templateModel);
			error = sendSimpleMessage(text);
		} catch (Exception e) {
			LOGGER.error("sendSimpleMessageUsingTemplate", e);
		}

		LOGGER.info("sendSimpleMessageUsingTemplate - END");

		return error;
	}

	@Override
	@SuppressFBWarnings("PATH_TRAVERSAL_IN")
	public boolean sendMessageWithAttachment(String text, String pathToAttachment) {
		LOGGER.info("sendMessageWithAttachment - START");

		boolean error = true;

		try {
			FileSystemResource file = new FileSystemResource(new File(pathToAttachment));
			error = sendMessageWithAttachment(text, null, Collections.singletonMap("Error", file));
		} catch (Exception e) {
			LOGGER.error("sendMessageWithAttachment", e);
		}

		LOGGER.info("sendMessageWithAttachment - END");

		return error;
	}

	@Override
	public boolean sendMessageWithAttachment(String text, String json, Map<String, InputStreamSource> listaAllegati) {
		LOGGER.info("sendMessageWithAttachment - START");

		boolean error = sendMessageWithAttachment(mailSubject, text, json, listaAllegati);

		LOGGER.info("sendMessageWithAttachment - END");

		return error;
	}

	private boolean sendMessageWithAttachment(String subject, String body, String json, Map<String, InputStreamSource> listaAllegati) {
		LOGGER.info("sendMessageWithAttachment - START");
		boolean error = true;

		try {
			if (json != null) {
				if (listaAllegati == null) {
					listaAllegati = new HashMap<>();
				}
				listaAllegati.put("amqp_message.json", new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)));
			}

			String[] to = createListDests(mailTo);
			String[] ccs = createListDests(mailCc);
			String[] bccs = createListDests(mailBcc);

			error = !emailSender.sendMessage(to, ccs, bccs, mailFrom, subject, body, listaAllegati);
		} catch (Exception e) {
			LOGGER.error("error during sending mail with attachments", e);
		}

		LOGGER.info("sendMessageWithAttachment - error={}", error);
		LOGGER.info("sendMessageWithAttachment - END");

		return error;
	}
}
