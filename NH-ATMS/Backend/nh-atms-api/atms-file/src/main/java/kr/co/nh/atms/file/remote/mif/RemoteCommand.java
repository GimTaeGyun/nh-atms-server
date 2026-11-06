package kr.co.nh.atms.file.remote.mif;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.security.interfaces.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import kr.co.nh.atms.file.remote.protocol.*;
import kr.co.nh.atms.file.remote.util.SafeClose;

/**
 * RemoteCommand
 * 구 새마을금고 atmf.lib (atmf.server.agent.RemoteCommand) 원본 이식.
 * 기기측 AP(원격명령 수신서버)로 RSA 핸드셰이크 + AES/HMAC 암호화 전문을 push 하는 소켓 클라이언트.
 * 프로토콜 호환을 위해 로직 변경 없이 패키지/import만 변경.
 */
public final class RemoteCommand {

	/**
	 * AgentClientException
	 */
	@SuppressWarnings("serial")
	public static class AgentClientException extends Exception {
		public AgentClientException(String format, Object... args) {
			super(String.format(format, args));
		}
	}

	/**
	 * send
	 */
	public static byte[] send(String ip, int port, int connectionMilliSecTimeout, byte[] reqBuffer)
			throws
			AgentClientException, IOException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException {
		if (connectionMilliSecTimeout < 500) { connectionMilliSecTimeout = 500; }
		Socket socket = null;
		InputStream  ins = null;
		OutputStream ous = null;
		try {
			byte[] req_bin = reqBuffer;
			if (req_bin.length > ProtcolDataTransfer.MAX_FIXED_SIZE) {
				throw new AgentClientException("[%s:%d][요청]사이즈 초과 size(%d) > limit(%d)", ip, port, req_bin.length, ProtcolDataTransfer.MAX_FIXED_SIZE);
			}

			@SuppressWarnings("resource")
			Stream src = new Stream(ProtcolDataTransfer.FIXED_HEAD_SIZE + req_bin.length);
			{
				ByteBuffer bb = src.wrapCapacity();
				bb.putInt(req_bin.length);
				bb.put(req_bin);
				bb.flip();
				src.size(bb.limit());
			}

			socket = new Socket();
			socket.connect(new InetSocketAddress(ip, port), connectionMilliSecTimeout);
			socket.setKeepAlive(true);
			ins = socket.getInputStream();
			ous = socket.getOutputStream();

			// 원격 서버에서 설정된 버퍼 : network에서 사용
			ByteBuffer buf = null;
			// 원격 서버에서 설정된 버퍼로 보낼수 있는 최대 블럭 개수
			int max_block = 0;
			// 원격 서버에서 설정된 버퍼로 한번에 보낼수 있는 최대 평문 사이즈
			int max_plain = 0;
			Cipher aes = ProtcolAesTransfer.create();
			SecretKey key = null;
			Mac mac = ProtcolAesTransfer.mac();
			{
				KeyPair keyPair = ProtcolRsaTransfer.generateKey();
				RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
				RSAPublicKey  publicKey  = (RSAPublicKey) keyPair.getPublic();

				byte[] modulus  = publicKey.getModulus().toByteArray();
				byte[] exponent = publicKey.getPublicExponent().toByteArray();

				int modulus_offset = 0;
				int modulus_length = modulus.length;
				if (modulus.length == 257 && modulus[0] == 0x00) {
					modulus_offset = 1;
					modulus_length--;
				}

				ByteBuffer bb = ByteBufferLE.allocate(ProtcolRsaTransfer.ReqHead.size + modulus_length + exponent.length);
				bb.putShort((short) modulus_length);
				bb.putShort((short) exponent.length);
				bb.put(modulus, modulus_offset, modulus_length);
				bb.put(exponent);
				bb.flip();
				ous.write(bb.array(), 0, bb.limit());

				bb.clear();
				bb.limit(ProtcolRsaTransfer.ResHead.size);
				if (recvn(ins, bb.array(), 0, bb.limit()) == 0) {
					throw new AgentClientException("[%s:%d][HANDSHAKE]서버로 부터 연결 끊김", ip, port);
				}
				bb.position(bb.limit());
				bb.flip();
				buf = ByteBufferLE.allocate(bb.getInt());
				max_block = buf.limit() / ProtcolAesTransfer.BLOCK_SIZE;
				max_plain = buf.limit() - ProtcolAesTransfer.Head.size - ProtcolAesTransfer.BLOCK_SIZE;

				byte[] rsa_aes_key = new byte[(int) bb.getShort()];
				byte[] rsa_mac_key = new byte[(int) bb.getShort()];
				if (recvn(ins, rsa_aes_key, 0, rsa_aes_key.length) == 0) {
					throw new AgentClientException("[%s:%d][HANDSHAKE]서버로 부터 연결 끊김", ip, port);
				}
				if (recvn(ins, rsa_mac_key, 0, rsa_mac_key.length) == 0) {
					throw new AgentClientException("[%s:%d][HANDSHAKE]서버로 부터 연결 끊김", ip, port);
				}

				Cipher rsa = ProtcolRsaTransfer.createPrivate(privateKey);
				byte[] aes_key = rsa.doFinal(rsa_aes_key);
				byte[] mac_key = rsa.doFinal(rsa_mac_key);

				key = ProtcolAesTransfer.generateKey(aes_key);
				mac.init(new SecretKeySpec(mac_key, "HmacSHA256"));
			}

			{
				ByteBuffer net = buf;

				byte[] plain = new byte[Math.min(src.available(), max_plain)];
				byte[] reserve = new byte[ProtcolAesTransfer.Head._03_RESERVE_SIZE];
				@SuppressWarnings("unused")
				int plain_remain = src.size() % max_plain;
				int plain_block  = (src.size() - 1) / max_plain + 1;
				int enc_block;
				byte[] enc;
				byte[] nonce;
				byte[] hmac;
				for (int i = 0, io; i < plain_block; i++) {
					io = src.read(plain, 0, plain.length);

					nonce = ProtcolAesTransfer.nonce();
					aes.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));
					enc = aes.doFinal(plain, 0, io);
					hmac = mac.doFinal(enc);

					enc_block  = (enc.length - 1) / ProtcolAesTransfer.BLOCK_SIZE + 1;
					net.clear();
					net.put(nonce);
					net.put(hmac);
					net.putShort((short) enc_block);
					net.put(reserve);
					net.put(enc);
					net.flip();
					ous.write(net.array(), 0, net.limit());
				}
			}

			{
				@SuppressWarnings("resource")
				Stream dst = new Stream();
				int enc_block;
				byte[] nonce;
				byte[] hmac0, hmac1;
				byte[] enc, plain;
				int fixedlen;

				nonce = new byte[ProtcolAesTransfer.Head._00_NONCE_SIZE];
				hmac0 = new byte[ProtcolAesTransfer.Head._01_HMAC_SIZE];
				while (true) {
					ByteBuffer net = buf;
					net.clear();
					net.limit(ProtcolAesTransfer.Head.size);
					if (recvn(ins, net.array(), 0, net.limit()) == 0) {
						throw new AgentClientException("[%s:%d][수신][head]서버로 부터 연결 끊김", ip, port);
					}

					net.get(nonce);
					net.get(hmac0);
					enc_block = net.getShort();
					if (enc_block > max_block) {
						throw new AgentClientException("[%s:%d][수신]최대 블럭사이즈 초과", ip, port);
					}

					buf.clear();
					buf.limit(enc_block * ProtcolAesTransfer.BLOCK_SIZE);
					if (recvn(ins, buf.array(), 0, buf.limit()) == 0) {
						throw new AgentClientException("[%s:%d][수신][body]서버로 부터 연결 끊김", ip, port);
					}

					enc = new byte[buf.limit()];
					buf.get(enc);
					hmac1 = mac.doFinal(enc);
					if (!Arrays.equals(hmac0, hmac1)) {
						throw new AgentClientException("[%s:%d][수신]hmac 검증 실패", ip, port);
					}

					aes.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(nonce));
					plain = aes.doFinal(enc);
					dst.write(plain, 0, plain.length);

					if (dst.size() < ProtcolDataTransfer.FIXED_HEAD_SIZE) {
						continue;
					}

					fixedlen = dst.wrap().getInt();
					if (dst.size() < ProtcolDataTransfer.FIXED_HEAD_SIZE + fixedlen) {
						continue;
					}

					byte[] res_bin = new byte[fixedlen];
					System.arraycopy(dst.array(), ProtcolDataTransfer.FIXED_HEAD_SIZE, res_bin, 0, res_bin.length);
					return res_bin;
				}
			}
		} finally {
			SafeClose.close(ins);
			SafeClose.close(ous);
			SafeClose.close(socket);
		}
	}

	/**
	 * recvn
	 */
	private static int recvn(InputStream ins, byte[] buffer, int offset, int size) throws IOException {
		int io, total = 0;
		while (true) {
			io = ins.read(buffer, offset, size);
			if (io == 0) { return 0; }
			offset += io;
			size -= io;
			total += io;
			if (size == 0) break;
		}
		return total;
	}
}
