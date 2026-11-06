package kr.co.nh.atms.file.remote.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * 구 새마을금고 atmf.lib (atmf.util.SafeClose) 원본 이식. 패키지만 변경.
 */
final public class SafeClose {

	private static final Logger LOGGER = LoggerFactory.getLogger(SafeClose.class);

	public static <T> T close(T t) {
		if (t == null) { return t; }
		try {
			if (t.getClass().isArray()) {
				Object[] array = (Object[]) t;
				for (Object obj : array) {
					close(obj);
				}
				return null;
			}
			if (t instanceof List<?>) {
				List<?> list = (List<?>) t;
				for (Object obj : list) {
					close(obj);
				}
				return null;
			}
			if (t instanceof Closeable) {
				((Closeable) t).close();
				return null;
			}
			Method m = t.getClass().getMethod("close");
			m.setAccessible(true);
			m.invoke(t);
		} catch (NoSuchMethodException ignored) {
			LOGGER.error("NoSuchMethodException", ignored);
		} catch (Exception e) {
			LOGGER.error("Exception", e);
		}
		return null;
	}
}
