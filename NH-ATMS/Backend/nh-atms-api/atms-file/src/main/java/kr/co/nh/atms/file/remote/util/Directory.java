package kr.co.nh.atms.file.remote.util;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

/**
 * 구 새마을금고 atmf.lib (atmf.util.Directory) 원본 이식. 패키지만 변경.
 */
public final class Directory {

	/**
	 * createDirectories
	 * @param path
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static void createDirectories(Path path)
			throws
			IOException,
			ClassNotFoundException,
			IllegalAccessException,
			IllegalArgumentException,
			InvocationTargetException,
			NoSuchMethodException,
			SecurityException {
		if (Files.exists(path)) return;

		if (isPosixSupported(path)) {
			createPosixDirectory(path);
		} else {
			Files.createDirectories(path);
			setWindowsAttributes(path, false, false);
		}
	}

	/**
	 * isPosixSupported
	 * @param path
	 * @return
	 */
	public static boolean isPosixSupported(Path path) {
		try {
			path = path.getRoot().resolve(path.subpath(0, 1));
			return Files.getFileStore(path).supportsFileAttributeView(PosixFileAttributeView.class);
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * createPosixDirectory
	 * @param path
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 */
	private static void createPosixDirectory(Path path)
			throws
			IOException,
			IllegalArgumentException,
			SecurityException {
		Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxr-x");
		FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(perms);
		Files.createDirectories(path, attrs);
	}

	/**
	 * setWindowsAttributes
	 * @param path
	 * @param readOnly
	 * @param hidden
	 * @throws IOException
	 */
	private static void setWindowsAttributes(Path path, boolean readOnly, boolean hidden) throws IOException {
		DosFileAttributeView dosAttr = Files.getFileAttributeView(path, DosFileAttributeView.class);
		if (dosAttr != null) {
			dosAttr.setReadOnly(readOnly);
			dosAttr.setHidden(hidden);
		}
	}
}
