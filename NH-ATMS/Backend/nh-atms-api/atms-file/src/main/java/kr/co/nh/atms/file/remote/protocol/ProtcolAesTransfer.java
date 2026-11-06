package kr.co.nh.atms.file.remote.protocol;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * ProtcolAesTransfer
 * 구 새마을금고 atmf.lib (atmf.server.ProtcolAesTransfer) 원본 이식. 패키지만 변경.
 */
final public class ProtcolAesTransfer {
	/*
	|==============================================================================|
	| AES(128BIT)+CBC+PKCS7+HMACSHA256 전송 규약                                   |
	| HEAD : NONCE(16)+HMAC(32)+BLOCK COUNT(2)+RESERVE(14) | BODY : BLOCK*16BYTE   |
	|==============================================================================|
	*/
	public static final int KEY_SIZE = 16;
	public static final int BLOCK_SIZE = 16;

	public static final String SPEC = "AES/CBC/PKCS5Padding";// PKCS7Padding
	public static final String HMAC = "HmacSHA256";

	/**
	 * Head
	 */
	final static public class Head {
		public static final int _00_NONCE_SIZE = 16;
		public static final int _01_HMAC_SIZE = 32;
		public static final int _02_BLOCK_COUNT = 02;
		public static final int _03_RESERVE_SIZE = 14;

		public static final int size = _00_NONCE_SIZE
				+ _01_HMAC_SIZE
				+ _02_BLOCK_COUNT
				+ _03_RESERVE_SIZE;
	}

	/**
	 * create
	 */
	public static Cipher create() throws NoSuchAlgorithmException, NoSuchPaddingException {
		return Cipher.getInstance(SPEC);
	}

	/**
	 * mac
	 */
	public static Mac mac() throws NoSuchAlgorithmException {
		return Mac.getInstance(HMAC);
	}

	/**
	 * generateKey
	 */
	public static SecretKey generateKey() throws NoSuchAlgorithmException {
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(KEY_SIZE * 8);
		return kg.generateKey();
	}

	/**
	 * generateKey
	 */
	public static SecretKey generateKey(byte[] key) {
		return new SecretKeySpec(key, "AES");
	}

	private static final ThreadLocal<SecureRandom> thread_local_random = ThreadLocal.withInitial(SecureRandom::new);

	/**
	 * nonce
	 */
	public static byte[] nonce() {
		byte[] nonce = new byte[Head._00_NONCE_SIZE];
		thread_local_random.get().nextBytes(nonce);
		return nonce;
	}

	/**
	 * hmac
	 */
	public static byte[] hmac() {
		byte[] hmac = new byte[Head._01_HMAC_SIZE];
		thread_local_random.get().nextBytes(hmac);
		return hmac;
	}
}
