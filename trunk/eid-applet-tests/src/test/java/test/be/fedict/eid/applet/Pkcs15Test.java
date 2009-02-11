/*
 * eID Applet Project.
 * Copyright (C) 2008-2009 FedICT.
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

package test.be.fedict.eid.applet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import test.be.fedict.eid.applet.PcscTest.TestView;
import be.fedict.eid.applet.PcscEid;
import be.fedict.eid.applet.PcscEidSpi;

public class Pkcs15Test {

	private static final Log LOG = LogFactory.getLog(Pkcs15Test.class);

	private PcscEidSpi pcscEidSpi;

	@Before
	public void setUp() throws Exception {
		this.pcscEidSpi = new PcscEid(new TestView());
		if (false == this.pcscEidSpi.isEidPresent()) {
			LOG.debug("insert eID card");
			this.pcscEidSpi.waitForEidPresent();
		}
	}

	@After
	public void tearDown() throws Exception {
		this.pcscEidSpi.close();
	}

	@Documented
	@Target( { ElementType.TYPE, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Tag {
		byte value();
	}

	@Tag(0x61)
	public static class Pkcs15ApplicationTemplate {
		@Tag(0x50)
		String label;

		@Tag(0x4f)
		byte[] id;

		@Tag(0x51)
		byte[] path;

		@Tag(0x73)
		byte[] discretionaryDataObjects;
	}

	public static class Pkcs15_EF_DIR {
		Pkcs15ApplicationTemplate[] applicationTemplates;
	}

	public static <T> T parsePkcs15File(byte[] data, Class<T> type)
			throws InstantiationException, IllegalAccessException {
		T result = type.newInstance();
		int dataIdx = 0;
		Tag tagAnnotation = type.getAnnotation(Tag.class);
		if (null != tagAnnotation) {
			byte tag = tagAnnotation.value();
			if (tag != data[0]) {
				throw new RuntimeException("incorrect tag: "
						+ Integer.toHexString(tag));
			}
			dataIdx++;
			int size = data[1];
			dataIdx++;
			LOG.debug("size: " + size);
			if (data.length - dataIdx != size) {
				throw new RuntimeException("data size incorrect: " + size
						+ " for tag " + Integer.toHexString(tag));
			}
		}
		Field[] fields = type.getDeclaredFields();
		if (1 == fields.length) {
			Field field = fields[0];
			if (field.getType().isArray()) {
				Class<?> componentType = field.getType().getComponentType();
				Object component = parsePkcs15File(data, componentType);
				Object array = Array.newInstance(componentType, 1);
				Array.set(array, 0, component);
				field.set(result, array);
				return result;
			}
		}
		while (dataIdx < data.length) {
			byte tag = data[dataIdx];
			dataIdx++;
			int size = data[dataIdx];
			dataIdx++;
			LOG.debug("tag: " + Integer.toHexString(tag) + "; size: " + size);
			for (Field field : fields) {
				Tag fieldTagAnnotation = field.getAnnotation(Tag.class);
				if (null != fieldTagAnnotation) {
					byte fieldTag = fieldTagAnnotation.value();
					if (fieldTag == tag) {
						LOG.debug("field found for tag "
								+ Integer.toHexString(tag) + ": "
								+ field.getName());
						Object value;
						if (String.class.equals(field.getType())) {
							value = new String(Arrays.copyOfRange(data,
									dataIdx, dataIdx + size));
						} else if (byte[].class.equals(field.getType())) {
							value = Arrays.copyOfRange(data, dataIdx, dataIdx
									+ size);
						} else {
							throw new RuntimeException(
									"unsupported field type: "
											+ field.getType().getName()
											+ " for field " + field.getName());
						}
						field.set(result, value);
					}
				}
			}
			dataIdx += size;
		}
		return result;
	}

	@Test
	public void EF_DIR() throws Exception {
		byte[] dir = this.pcscEidSpi.readFile(new byte[] { 0x2f, 0x00 });
		LOG.debug("size of EF(DIR): " + dir.length);
		LOG.debug("EF(DIR): " + new String(Hex.encodeHex(dir)));

		Pkcs15_EF_DIR result = parsePkcs15File(dir, Pkcs15_EF_DIR.class);
		assertNotNull(result);
		assertNotNull(result.applicationTemplates);
		assertEquals(1, result.applicationTemplates.length);
		Pkcs15ApplicationTemplate applicationTemplate = result.applicationTemplates[0];
		assertNotNull(applicationTemplate);
		assertNotNull(applicationTemplate.label);
		LOG.debug("application label: " + applicationTemplate.label);
		LOG.debug("application id: "
				+ new String(Hex.encodeHex(applicationTemplate.id)));
		LOG.debug("application path: "
				+ new String(Hex.encodeHex(applicationTemplate.path)));
		LOG
				.debug("application discretionary data objects: "
						+ new String(
								Hex
										.encodeHex(applicationTemplate.discretionaryDataObjects)));
	}

	private Pkcs15ApplicationTemplate getApplication(Pkcs15_EF_DIR dir,
			byte[] applicationId) {
		for (Pkcs15ApplicationTemplate applicationTemplate : dir.applicationTemplates) {
			if (Arrays.equals(applicationId, applicationTemplate.id)) {
				return applicationTemplate;
			}
		}
		throw new RuntimeException(
				"no application template found for application id: "
						+ new String(Hex.encodeHex(applicationId)));
	}

	@Test
	public void EF_ODF() throws Exception {
		byte[] dirData = this.pcscEidSpi.readFile(new byte[] { 0x2f, 0x00 });
		Pkcs15_EF_DIR dir = parsePkcs15File(dirData, Pkcs15_EF_DIR.class);

		Pkcs15ApplicationTemplate applicationTemplate = getApplication(dir,
				new byte[] { (byte) 0xa0, 0x00, 0x00, 0x01, 0x77, 0x50, 0x4b,
						0x43, 0x53, 0x2d, 0x31, 0x35 });

		byte[] odfFileId = new byte[applicationTemplate.path.length + 2];
		System.arraycopy(applicationTemplate.path, 0, odfFileId, 0,
				applicationTemplate.path.length);
		System.arraycopy(new byte[] { 0x50, 0x31 }, 0, odfFileId, 4, 2);

		byte[] odf = this.pcscEidSpi.readFile(odfFileId);
		LOG.debug("size of EF(ODF): " + odf.length);
		LOG.debug("EF(ODF): " + new String(Hex.encodeHex(odf)));
	}
}