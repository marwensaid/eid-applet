/*
 * eID Applet Project.
 * Copyright (C) 2008-2010 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eid.applet.service.spi;

import java.security.cert.X509Certificate;

/**
 * SPI for the channel binding service.
 * 
 * @author Frank Cornelis
 * 
 */
public interface ChannelBindingService {

	/**
	 * Gives back the X509 server SSL certificate that should be used for
	 * verification of the secure channel binding. When <code>null</code> is
	 * being returned the eID Applet Service can of course not perform the
	 * secure channel binding verification.
	 * 
	 * @return the X509 server certificate.
	 */
	X509Certificate getServerCertificate();
}
