package kr.co.nh.atms.file.remote.protocol;

import java.math.*;
import java.security.*;
import java.security.spec.*;
import java.util.concurrent.locks.*;
import javax.crypto.*;

/**
 * 구 새마을금고 atmf.lib (atmf.server.ProtcolRsaTransfer) 원본 이식. 패키지만 변경.
 */
final public class ProtcolRsaTransfer {
	/*
	|============================================================================|
	| RSA(2048BIT) 를 통한 AES 대칭키(128BIT) 교환 규약                          |
	| : RSA/ECB/PKCS1PADDING / LITTLE ENDIAN / UNSIGNED                          |
	|============================================================================|
	*/

	public static final int MODULUS_MAX_SIZE = 256; // 고정
	public static final int EXPONENT_MAX_SIZE = 4;   // 3~4
	public static final String SPEC = "RSA/ECB/PKCS1Padding";

	final static public class ReqHead {
		public static final int _00_MODULUS_SIZE = 2;
		public static final int _01_EXPONENT_SIZE = 2;

		public static final int size = _00_MODULUS_SIZE
				+ _01_EXPONENT_SIZE;
	}

	final static public class ResHead {
		public static final int _00_AES_MAX_ENCRYPTED_SIZE = 4;
		public static final int _01_AES_KEY_SIZE = 2;
		public static final int _02_MAC_KEY_SIZE = 2;

		public static final int size = _00_AES_MAX_ENCRYPTED_SIZE
				+ _01_AES_KEY_SIZE
				+ _02_MAC_KEY_SIZE;
	}

	public static Cipher createPublic(byte[] modulus, byte[] exponent)
			throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidKeySpecException {
		Cipher cipher = Cipher.getInstance(SPEC);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PublicKey publicKey = kf.generatePublic(new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent)));
		cipher.init(Cipher.PUBLIC_KEY, publicKey);
		return cipher;
	}

	public static Cipher createPrivate(PrivateKey privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		Cipher cipher = Cipher.getInstance(SPEC);
		cipher.init(Cipher.PRIVATE_KEY, privateKey);
		return cipher;
	}

	private static volatile KeyPair kp;
	private static long last = 0;
	private static final ReentrantLock lock = new ReentrantLock();

	public static KeyPair generateKey() throws NoSuchAlgorithmException {
		try {
			long interval = 60 * 60 * 1000;
			lock.lock();
			long now = System.currentTimeMillis();
			if (kp == null || (now - last) >= interval) {
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
				keyGen.initialize(2048);
				kp = keyGen.generateKeyPair();
				last = now;
			}
			return kp;
		} finally {
			lock.unlock();
		}
	}
}
