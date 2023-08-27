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

import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.core.io.InputStreamSource;

/**
 *
 * <p>
 * The <code>TroubleshootingService</code> class
 * </p>
 * <p>
 * Data: 7 nov 2022
 * </p>
 * 
 * @author <a href="mailto:edv@gvcc.net"></a>
 * @version 1.0
 */
public interface TroubleshootingService {

	/**
	 * @param event
	 *            message event to be reported
	 * @param message
	 *            message text to be reported to final user: it will be appened to the default body
	 * @param attachments
	 *            documents list of the message to be attached to the reported issue
	 * @return true if error; false otherwise
	 */
	boolean sendMessage(Message event, String message, Map<String, InputStreamSource> attachments);

	/**
	 * @param subject
	 *            subject of message
	 * @param body
	 *            message text to be reported to final user
	 * @param attachments
	 *            documents list of the message to be attached to the reported issue
	 * @return true if error; false otherwise
	 */
	boolean sendMessage(String subject, String body, Map<String, InputStreamSource> attachments);

	/**
	 * @param event
	 *            message event to be reported
	 * @param message
	 *            message text to be reported to final user: it will be appened to the default body
	 * @param attachments
	 *            documents list of the message to be attached to the reported issue
	 * @return true if error; false otherwise
	 */
	boolean sendMessageWithBody(Message event, String message, Map<String, InputStreamSource> attachments);

	/**
	 * @param subject
	 *            subject of message
	 * @param body
	 *            body of message
	 * @param attachments
	 *            documents list of the message to be attached to the reported issue
	 * @return true if error; false otherwise
	 */
	boolean sendMessageWithBody(String subject, String body, Map<String, InputStreamSource> attachments);

	/**
	 * 
	 * @return the configured default body using SpringBoot properties
	 */
	String getDefaultBody();
}