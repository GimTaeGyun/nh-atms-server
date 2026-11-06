package kr.co.nh.atms.file.remote.server;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.nh.atms.file.remote.protocol.ByteBufferLE;
import kr.co.nh.atms.file.remote.protocol.ProtcolAesTransfer;
import kr.co.nh.atms.file.remote.protocol.ProtcolDataTransfer;
import kr.co.nh.atms.file.remote.protocol.ProtcolRsaTransfer;
import kr.co.nh.atms.file.remote.protocol.Stream;
import kr.co.nh.atms.file.remote.util.Directory;
import kr.co.nh.atms.file.remote.util.Hex;
import kr.co.nh.atms.file.remote.util.Json;

/**
 * Client
 * 구 새마을금고 atmf.lib (atmf.server.Client) 원본 이식.
 * 정적(static) 상태만 ServerContext 인스턴스 참조로 변환. 프로토콜 처리 로직 무변경.
 * (.NET Agent 클라이언트와 바이트 단위 프로토콜 호환 유지 필수)
 */
final class Client extends AsyncSocketChannel {
	private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

	/**
	 * CloseHandler
	 */
	@FunctionalInterface
	interface CloseHandler {
		void on(Client client);
	}

	/**
	 * RecvPlainDataHandler
	 */
	@FunctionalInterface
	interface RecvPlainDataHandler {
		void on(boolean succ, byte[] data);
	}

	/**
	 * RecvChunkDataHandler
	 */
	@FunctionalInterface
	interface RecvChunkDataHandler {
		void on(boolean succ);
	}

	/**
	 * SendPlainDataHandler
	 */
	@FunctionalInterface
	interface SendPlainDataHandler {
		void on(boolean succ);
	}

	/**
	 * SendChunkDataHandler
	 */
	@FunctionalInterface
	interface SendChunkDataHandler {
		void on(boolean succ);
	}

	/**
	 * attach
	 * @param channel
	 * @param ctx
	 * @param onclose
	 * @return
	 * @throws Exception
	 */
	public static Client attach(AsynchronousSocketChannel channel, ServerContext ctx, CloseHandler onclose) throws Exception {
		return new Client(channel, ctx, onclose);
	}

	private final ServerContext ctx;

	private CloseHandler onclose;
	private String addr;
	private String ip;
	private String ip_fix;
	private int port;
	private boolean loopback;
	private SecretKey key = ProtcolAesTransfer.generateKey();
	private Cipher aes = ProtcolAesTransfer.create();
	private Mac mac = ProtcolAesTransfer.mac();

	/**
	 * Client
	 * @param channel
	 * @param ctx
	 * @param closeHandler
	 * @throws Exception
	 */
	private Client(AsynchronousSocketChannel channel, ServerContext ctx, CloseHandler closeHandler) throws Exception {
		super(channel);
		this.ctx = ctx;
		onclose = closeHandler;
		{
			InetSocketAddress _addr = (InetSocketAddress) channel.getRemoteAddress();
			ip = _addr.getAddress().getHostAddress();
			port = _addr.getPort();
			loopback = _addr.getAddress().isLoopbackAddress();

			String[] octets = ip.split("\\.");
			ip_fix = String.format("%03d%03d%03d%03d",
				Integer.parseInt(octets[0]),
				Integer.parseInt(octets[1]),
				Integer.parseInt(octets[2]),
				Integer.parseInt(octets[3]));

			addr = ip + ":" + port;
		}
		LOGGER.info(String.format("[connect][%s]", addr));
	}

	/**
	 * address
	 * @return
	 */
	public String address() {
		return addr;
	}

	/**
	 * onclose_fire
	 */
	void onclose_fire() {
		if (onclose != null) {
			onclose.on(this);
		}
	}

	/**
	 * closed
	 * @param e
	 * @return
	 */
	private boolean closed(AysncSocketEventArgs e) {
		if (e.getTransfer() <= 0) {
			close();
			if (e.getException() != null) {
				LOGGER.info(String.format("[close][%s][%s][e:(%s)%s]", addr, e.getOper(), e.getException().getClass().getName(), e.getException().getMessage()));
			} else {
				LOGGER.info(String.format("[close][%s][%s]", addr, e.getOper()));
			}
			onclose_fire();
			return true;
		}
		return false;
	}

	/**
	 * closed
	 * @param msg
	 * @param arg
	 */
	private void closed(String msg, Object... arg) {
		close();
		LOGGER.info(String.format("[close]%s", String.format(msg, arg)));
		onclose_fire();
	}

	/**
	 * closed
	 * @param e
	 * @param msg
	 * @param arg
	 */
	private void closed(Throwable e, String msg, Object... arg) {
		close();
		if (e != null) {
			LOGGER.error(String.format("[close]%s", String.format(msg, arg)), e);
		} else {
			LOGGER.info(String.format("[close]%s", String.format(msg, arg)));
		}
		onclose_fire();
	}

	/**
	 * handshakeAsync
	 */
	public void handshakeAsync() {
		CompletableFuture.runAsync(() -> {
			ByteBuffer[] net = new ByteBuffer[]{ctx.pool.acquire()};
			net[0].limit(ProtcolRsaTransfer.ReqHead.size);
			recvn(net[0], null, (e) -> {
				if (closed(e)) {
					ctx.pool.release(net);
					return;
				}
				net[0].flip();
				int msize = Short.toUnsignedInt(net[0].getShort());
				int esize = Short.toUnsignedInt(net[0].getShort());
				if (
					(msize + esize) <= 0
					||
					(msize + esize) > (ProtcolRsaTransfer.MODULUS_MAX_SIZE + ProtcolRsaTransfer.EXPONENT_MAX_SIZE)
				) {
					ctx.pool.release(net);
					closed("[%s][rsa 공개키 사이즈 오류 : m(%d)+e(%d) >= %d]", addr, msize, esize, ProtcolRsaTransfer.MODULUS_MAX_SIZE + ProtcolRsaTransfer.EXPONENT_MAX_SIZE);
					return;
				}

				net[0].clear();
				net[0].limit(msize + esize);
				recvn(net[0], null, (ee) -> {
					if (closed(ee)) {
						ctx.pool.release(net);
						return;
					}
					net[0].flip();
					byte[] modulus = new byte[msize];
					byte[] exponent = new byte[esize];
					net[0].get(modulus);
					net[0].get(exponent);

					Cipher cipher;
					try {
						cipher = ProtcolRsaTransfer.createPublic(modulus, exponent);
						byte[] aes_key = key.getEncoded();
						byte[] rsa_aes_key = cipher.doFinal(aes_key);

						byte[] mac_key = ProtcolAesTransfer.hmac();
						mac.init(new SecretKeySpec(mac_key, "HmacSHA256"));
						byte[] rsa_mac_key = cipher.doFinal(mac_key);

						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace(String.format("[%s]\n"
								+ "[aeskey]\n"
								+ "%s\n"
								+ "[hmackey]\n"
								+ "%s\n"
								, addr
								, Hex.dump(aes_key)
								, Hex.dump(mac_key)
							));
						}
						net[0].clear();
						net[0].putInt(ctx.pool.bufferSize());
						net[0].putShort((short) rsa_aes_key.length);
						net[0].putShort((short) rsa_mac_key.length);
						net[0].put(rsa_aes_key);
						net[0].put(rsa_mac_key);
						net[0].flip();
						sendn(net[0], null, (eee) -> {
							ctx.pool.release(net);
							if (closed(eee)) {
								return;
							}
							recvPlainAsync();
						});
					} catch (Exception ex) {
						ctx.pool.release(net);
						closed(ex, "[%s][rsa 공개키 암호화 실패]", addr);
					}
				});
			});
		});
	}

	/**
	 * recvCipherAsync
	 * @param net
	 * @param onrecv
	 */
	private void recvCipherAsync(ByteBuffer net, RecvPlainDataHandler onrecv) {
		net.clear();
		net.limit(ProtcolAesTransfer.Head.size);
		recvn(net, null, (e) -> {
			if (closed(e)) {
				onrecv.on(false, null);
				return;
			}
			net.flip();
			int block = 0;
			byte[] nonce = new byte[ProtcolAesTransfer.Head._00_NONCE_SIZE];
			byte[] hmac = new byte[ProtcolAesTransfer.Head._01_HMAC_SIZE];
			net.get(nonce);
			net.get(hmac);
			block = net.getShort();
			if (block > ctx.max_block) {
				closed("[%s][aes 암호화 가능한 최대 길이 초과 (Buffer Overflow) : block(%d) >= max block(%d)]", addr, block, ctx.max_block);
				onrecv.on(false, null);
				return;
			}

			net.clear();
			net.limit(block * ProtcolAesTransfer.BLOCK_SIZE);
			recvn(net, null, (ee) -> {
				if (closed(ee)) {
					onrecv.on(false, null);
					return;
				}
				net.flip();
				byte[] enc = new byte[net.limit()];
				net.get(enc);
				try {
					byte[] hmac0 = hmac;
					byte[] hmac1 = mac.doFinal(enc);
					if (!Arrays.equals(hmac0, hmac1)) {
						closed("[%s][aes hmac 검증 실패 : value]", addr);
						onrecv.on(false, null);
						return;
					}

					aes.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(nonce));
					byte[] dec = aes.doFinal(enc);

					onrecv.on(true, dec);
				} catch (Exception ex) {
					closed(ex, "[%s][aes 복호화 실패]", addr);
					onrecv.on(false, null);
				}
			});
		});
	}

	/**
	 * recvChunkReadOnly
	 * @param input
	 * @param onrecv
	 */
	private void recvChunkReadOnlyAsync(Stream input, RecvChunkDataHandler onrecv) {
		ByteBuffer[] net = new ByteBuffer[]{ctx.pool.acquire()};
		Runnable[] func = new Runnable[1];
		func[0] = () -> {
			if (input.size() < ProtcolDataTransfer.CHUNK_HEAD_SIZE) {
				recvCipherAsync(net[0], (succ, dec) -> {
					if (!succ) {
						ctx.pool.release(net);
						onrecv.on(false);
						return;
					}
					input.write(dec, 0, dec.length);
					func[0].run();
				});
				return;
			}
			int chunk_size = input.wrap().getInt();
			if (chunk_size == 0) {
				ctx.pool.release(net);
				onrecv.on(true);
				return;
			}
			if (input.size() < ProtcolDataTransfer.CHUNK_HEAD_SIZE + chunk_size) {
				recvCipherAsync(net[0], (succ, dec) -> {
					if (!succ) {
						onrecv.on(false);
						return;
					}
					input.write(dec, 0, dec.length);
					func[0].run();
				});
				return;
			}
			input.position(ProtcolDataTransfer.CHUNK_HEAD_SIZE + chunk_size);
			input.compact();
			func[0].run();
		};
		func[0].run();
	}

	/**
	 * recvChunk
	 * @param input
	 * @param localFileSize
	 * @param file
	 * @param onrecv
	 */
	private void recvChunkAsync(Stream input, long localFileSize, AsyncFileChannel file, RecvChunkDataHandler onrecv) {
		ByteBuffer[] net = new ByteBuffer[]{ctx.pool.acquire()};
		Runnable[] func = new Runnable[1];
		func[0] = () -> {
			if (input.size() < ProtcolDataTransfer.CHUNK_HEAD_SIZE) {
				recvCipherAsync(net[0], (succ, dec) -> {
					if (!succ) {
						ctx.pool.release(net);
						file.close();
						onrecv.on(false);
						return;
					}
					input.write(dec, 0, dec.length);
					func[0].run();
				});
				return;
			}
			int chunk_size = input.wrap().getInt();
			if (chunk_size == 0) {
				ctx.pool.release(net);
				file.close();
				long len0 = localFileSize;
				long len1 = file.length();
				if (len0 >= 0 && len0 != len1) {
					closed("[%s][aes file chunk 수신 사이즈 오류 : len(%d) != (rcv)%d]", addr, len0, len1);
					onrecv.on(false);
					return;
				}
				onrecv.on(true);
				return;
			}
			if (input.size() < ProtcolDataTransfer.CHUNK_HEAD_SIZE + chunk_size) {
				recvCipherAsync(net[0], (succ, dec) -> {
					if (!succ) {
						ctx.pool.release(net);
						file.close();
						onrecv.on(false);
						return;
					}
					input.write(dec, 0, dec.length);
					func[0].run();
				});
				return;
			}
			ByteBuffer buf = input.wrap();
			buf.limit(ProtcolDataTransfer.CHUNK_HEAD_SIZE + chunk_size);
			buf.position(ProtcolDataTransfer.CHUNK_HEAD_SIZE);
			file.write(buf, null, (fe) -> {
				if (fe.getTransfer() == -1) {
					closed(fe.getException(), "[%s][aes file chunk 수신 전문처리 실패 : AysncFileChannel read fail]", addr);
					ctx.pool.release(net);
					file.close();
					onrecv.on(false);
					return;
				}
				input.position(buf.limit());
				input.compact();
				func[0].run();
			});
		};
		func[0].run();
	}

	/**
	 * recvPlain
	 */
	private void recvPlainAsync() {
		ByteBuffer[] net = new ByteBuffer[]{ctx.pool.acquire()};
		Stream input = new Stream();
		Runnable[] func = new Runnable[1];
		func[0] = () -> {
			try {
				if (input.size() < ProtcolDataTransfer.FIXED_HEAD_SIZE) {
					recvCipherAsync(net[0], (succ, dec) -> {
						if (!succ) {
							ctx.pool.release(net);
							return;
						}
						input.write(dec, 0, dec.length);
						func[0].run();
					});
					return;
				}
				ByteBuffer plainbb = input.wrap();
				int jlen = plainbb.getInt();
				if (!(1 <= jlen && jlen <= ProtcolDataTransfer.MAX_FIXED_SIZE)) {
					closed("[%s][json header 수신 사이즈 에러 : !(1 <= %d <= %d(max))]", addr, jlen, ProtcolDataTransfer.MAX_FIXED_SIZE);
					ctx.pool.release(net);
					return;
				}
				if (plainbb.limit() < (ProtcolDataTransfer.FIXED_HEAD_SIZE + jlen)) {
					recvCipherAsync(net[0], (succ, dec) -> {
						if (!succ) {
							ctx.pool.release(net);
							return;
						}
						input.write(dec, 0, dec.length);
						func[0].run();
					});
					return;
				}
				ctx.pool.release(net);
				net[0] = null;

				byte[] bin = new byte[jlen];
				plainbb.get(bin);
				input.position(ProtcolDataTransfer.FIXED_HEAD_SIZE + jlen);
				input.compact();

				String jtxt = new String(bin, ProtcolDataTransfer.CHATSET);
				JsonObject jreq = Json.toJsonObject(jtxt);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(String.format("[%s]\n%s", addr, Json.toJsonText(jreq)));
				}

				JsonObject jsnd = new JsonObject();
				JsonObject jres = new JsonObject();
				jsnd.add("req", jreq);
				jsnd.add("res", jres);

				SendPlainDataHandler recvContinuePlain = (succ) -> {
					if (input.available() > 0) {
						func[0].run();
						return;
					}
					recvPlainAsync();
				};
				switch (Json.str(jreq, "act")) {
					case "get": {
						Path[] file = new Path[1];
						String path_org = Json.str(jreq, "src");
						String path = path_org.replace("\\", "/");
						if (path.contains("..")) {
							jres.addProperty("err", String.format("req.src 오류 : dot(.) used : %s", path_org));
						} else {
							if (path.startsWith("/")) {
								path = path.substring(1);
							}
							if (path.endsWith("/")) {
								path = path.substring(0, path.length() - 1);
							}
							file[0] = ctx.repository.resolve(path);
							if (!Files.exists(file[0])) {
								jres.addProperty("err", String.format("req.src 오류 : not found : %s", path_org));
							}
							if (Files.isDirectory(file[0])) {
								jres.addProperty("err", String.format("req.src 오류 : required file path : %s", path_org));
							}
						}
						jres.addProperty("len", file[0] == null ? -1 : file[0].toFile().length());
						if (jres.has("err")) {
							file[0] = null;
						} else {
							jres.addProperty("err", "");
						}

						String jsnd_txt = Json.toJsonText(jsnd);
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace(String.format("[%s]\n%s", addr, jsnd_txt));
						}
						byte[] jbin = jsnd_txt.getBytes(ProtcolDataTransfer.CHATSET);
						ByteBuffer jbuf = ByteBufferLE.allocate(ProtcolDataTransfer.FIXED_HEAD_SIZE + jbin.length);
						jbuf.putInt(jbin.length);
						jbuf.put(jbin);
						jbuf.flip();
						sendPlainAsync(jbuf, (succ) -> {
							if (file[0] == null) {
								recvContinuePlain.on(false);
								return;
							}
							if (!succ) {
								recvContinuePlain.on(false);
								return;
							}
							try {
								AsyncFileChannel afc = AsyncFileChannel.open(file[0], null, StandardOpenOption.READ);
								long off = Json.int64(jreq, "off");
								afc.position(off <= 0 ? 0 : off);
								sendChunkAsync(afc, (succ0) -> {
									afc.close();
									recvContinuePlain.on(true);
								});
							} catch (Exception ex) {
								closed(ex, "[%s][송신 파일 open 실패][%s]", addr, file[0]);
								return;
							}
						});
						break;
					}
					case "put": {
						Path dst = null;
						{
							String path_org = Json.str(jreq, "dst");
							String path = path_org.replace("\\", "/");
							if (path.contains("..")) {
								jres.addProperty("err", String.format("req.dst 오류 : dot(.) used : %s", path_org));
							} else {
								if (path.startsWith("/")) {
									path = path.substring(1);
								}
								if (path.endsWith("/")) {
									path = path.substring(0, path.length() - 1);
								}
								Path v = ctx.repository.resolve(path);
								if (!Files.exists(v)) {
									try {
										Directory.createDirectories(v);
									} catch (Exception e) {
										jres.addProperty("err", String.format("req.dst 오류 : fail create directory : %s", path_org));
									}
								}
								if (!Files.isDirectory(v)) {
									jres.addProperty("err", String.format("req.dst 오류 : required file path : %s", path_org));
								}

								LocalDateTime now = LocalDateTime.now();
								LocalDate today = LocalDate.now();
								if (!today.equals(ctx.fd_counter_last)) {
									try {
										ctx.fd_counter_lock.lock();
										if (!today.equals(ctx.fd_counter_last)) {
											ctx.fd_counter.set(0);
											ctx.fd_counter_last = today;
										}
									} finally {
										ctx.fd_counter_lock.unlock();
									}
								}
								dst = v.resolve(String.format(
									"%s.%012d.%s.bin",
									now.format(ctx.df),
									ctx.fd_counter.incrementAndGet(),
									ip_fix
								));
							}
						}
						Runnable sendResponceAsync = () -> {
							try {
								String jsnd_txt = Json.toJsonText(jsnd);
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace(String.format("[%s]\n%s", addr, jsnd_txt));
								}
								byte[] jbin = jsnd_txt.getBytes(ProtcolDataTransfer.CHATSET);
								ByteBuffer jbuf = ByteBufferLE.allocate(ProtcolDataTransfer.FIXED_HEAD_SIZE + jbin.length);
								jbuf.putInt(jbin.length);
								jbuf.put(jbin);
								jbuf.flip();
								sendPlainAsync(jbuf, recvContinuePlain);
							} catch (UnsupportedEncodingException e) {
								;
							}
						};
						if (jres.has("err")) {
							jres.addProperty("dst", "");
							sendResponceAsync.run();
							return;
						}
						jres.addProperty("err", "");
						jres.addProperty("dst", "/" + ctx.repository.relativize(dst).toString().replaceAll(Pattern.quote("\\"), "/"));
						try {
							AsyncFileChannel afc = AsyncFileChannel.open(dst, null, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
							recvChunkAsync(input, Json.int64(jreq, "len"), afc, (succ) -> {
								afc.close();
								if (!succ) {
									afc.delete();
									return;
								}
								sendResponceAsync.run();
							});
						} catch (IOException ex) {
							jres.addProperty("err", String.format("req.dst 오류 : required open fail : %s", "/" + ctx.repository.relativize(dst)));
							recvChunkReadOnlyAsync(input, (succ) -> {
								sendResponceAsync.run();
							});
						}
						break;
					}
					default: break;
				}
			} catch (Exception ex) {
				closed(ex, "[%s][aes 수신 전문처리 실패]", addr);
				ctx.pool.release(net);
				return;
			}
		};
		func[0].run();
	}

	/**
	 * sendCipherAsync
	 * @param src
	 * @param net
	 * @param onsend
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws ShortBufferException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private void sendCipherAsync(ByteBuffer src, ByteBuffer net, SendPlainDataHandler onsend)
			throws InvalidKeyException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		byte[] nonce = ProtcolAesTransfer.nonce();
		aes.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));

		net.clear();
		net.position(ProtcolAesTransfer.Head.size);
		int enc_size = aes.doFinal(src, net);
		net.limit(net.position());

		byte[] enc = new byte[enc_size];
		net.position(ProtcolAesTransfer.Head.size);
		net.get(enc);
		byte[] hmac = mac.doFinal(enc);

		int enc_block = (enc_size - 1) / ProtcolAesTransfer.BLOCK_SIZE + 1;
		@SuppressWarnings("unused")
		int enc_remain = enc_size % ProtcolAesTransfer.BLOCK_SIZE; //반드시 Zero
		net.position(0);
		net.put(nonce);
		net.put(hmac);
		net.putShort((short) enc_block);
		net.putShort((short) 0);
		net.putInt(0);
		net.putInt(0);
		net.putInt(0);
		net.position(0);
		sendn(net, null, (e) -> {
			if (closed(e)) {
				onsend.on(false);
				return;
			}
			onsend.on(true);
		});
	}

	/**
	 * sendPlainAsync
	 * @param src
	 * @param onsend
	 */
	private void sendPlainAsync(ByteBuffer src, SendPlainDataHandler onsend) {
		ByteBuffer[] net = new ByteBuffer[]{ctx.pool.acquire()};
		ByteBuffer bin = ByteBufferLE.allocate(ctx.max_plain);
		Runnable[] func = new Runnable[1];
		func[0] = () -> {
			try {
				if (!src.hasRemaining()) {
					ctx.pool.release(net);
					onsend.on(true);
					return;
				}
				bin.clear();
				bin.limit(Math.min(src.remaining(), ctx.max_plain));
				src.get(bin.array(), 0, bin.limit());
				bin.position(bin.limit());
				bin.flip();
				sendCipherAsync(bin, net[0], (succ) -> {
					if (!succ) {
						ctx.pool.release(net);
						onsend.on(false);
						return;
					}
					func[0].run();
				});
			} catch (Exception ex) {
				LOGGER.error(String.format("[%s][aes plain 송신 전문처리 실패 : ByteBuffer]", addr), ex);
				ctx.pool.release(net);
				onsend.on(false);
			}
		};
		func[0].run();
	}

	/**
	 * sendChunkAsync
	 * @param src
	 * @param onsend
	 */
	private void sendChunkAsync(AsyncFileChannel src, SendPlainDataHandler onsend) {
		ByteBuffer[] bus = ctx.pool.acquire(2);
		ByteBuffer net = bus[0];
		ByteBuffer bin = bus[1];
		Runnable[] func = new Runnable[1];
		func[0] = () -> {
			long available = src.available();
			if (available == 0) {
				try {
					bin.clear();
					bin.limit(ProtcolDataTransfer.CHUNK_HEAD_SIZE);
					bin.putInt(0);
					bin.position(bin.limit());
					bin.flip();
					sendCipherAsync(bin, net, (succ) -> {
						ctx.pool.release(bus);
						src.close();
						onsend.on(succ);
					});
				} catch (Exception ex) {
					LOGGER.error(String.format("[%s][aes file chunk 송신 전문처리 실패 : AysncFileChannel : last]", addr), ex);
					ctx.pool.release(bus);
					src.close();
					onsend.on(false);
				}
				return;
			}

			bin.clear();
			bin.limit(ctx.max_plain);
			bin.position(ProtcolDataTransfer.CHUNK_HEAD_SIZE);
			src.read(bin, null, (fe) -> {
				try {
					if (fe.getTransfer() == -1) {
						LOGGER.error(String.format("[%s][aes file  chunk 송신 전문처리 실패 : AysncFileChannel read fail]", addr), fe.getException());
						ctx.pool.release(bus);
						src.close();
						onsend.on(false);
						return;
					}
					bin.position(0);
					bin.putInt(fe.getTransfer());
					bin.position(ProtcolDataTransfer.CHUNK_HEAD_SIZE + fe.getTransfer());
					bin.flip();
					sendCipherAsync(bin, net, (succ) -> {
						if (!succ) {
							ctx.pool.release(bus);
							src.close();
							onsend.on(false);
							return;
						}
						func[0].run();
					});
				} catch (Exception ex) {
					LOGGER.error(String.format("[%s][aes file chunk 송신 전문처리 실패 : AysncFileChannel]", addr), ex);
					ctx.pool.release(bus);
					src.close();
					onsend.on(false);
				}
			});
		};
		func[0].run();
	}

	/**
	 * sendChunkAsync
	 * @param src
	 * @param onsend
	 */
	@SuppressWarnings("unused")
	private void sendChunkAsync(Stream src, SendChunkDataHandler onsend) {
		ByteBuffer[] net = new ByteBuffer[]{ctx.pool.acquire()};
		ByteBuffer bin = ByteBufferLE.allocate(ctx.max_plain);
		Runnable[] func = new Runnable[1];
		func[0] = () -> {
			try {
				int available = src.available();
				switch (available) {
					case -1: //never
						return;
					case -2:
						LOGGER.error(String.format("[%s][aes stream chunk 송신 전문처리 실패 : InputOutputStream is IOException]", addr), src.getIOException());
						ctx.pool.release(net);
						onsend.on(false);
						return;
					default: break;
				}
				bin.clear();
				if (available > 0) {
					bin.limit(Math.min(ProtcolDataTransfer.CHUNK_HEAD_SIZE + available, ctx.max_plain));
					bin.putInt(bin.limit() - ProtcolDataTransfer.CHUNK_HEAD_SIZE);
					src.read(bin.array(), ProtcolDataTransfer.CHUNK_HEAD_SIZE, bin.limit() - ProtcolDataTransfer.CHUNK_HEAD_SIZE);
					bin.position(bin.limit());
				} else {
					bin.putInt(0);
				}
				bin.flip();
				sendCipherAsync(bin, net[0], (succ) -> {
					if (!succ) {
						ctx.pool.release(net);
						onsend.on(false);
						return;
					}
					if (available == 0) {
						ctx.pool.release(net);
						onsend.on(true);
						return;
					}
					func[0].run();
				});
			} catch (Exception ex) {
				LOGGER.error(String.format("[%s][aes stream chunk 송신 전문처리 실패 : Stream]", addr), ex);
				ctx.pool.release(net);
				onsend.on(false);
			}
		};
		func[0].run();
	}
}
