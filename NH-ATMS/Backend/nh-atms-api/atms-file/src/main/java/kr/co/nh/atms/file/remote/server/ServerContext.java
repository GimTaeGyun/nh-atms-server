package kr.co.nh.atms.file.remote.server;

import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.nh.atms.file.remote.protocol.ProtcolAesTransfer;
import kr.co.nh.atms.file.remote.util.Directory;

/**
 * ServerContext
 * 구 atmf.server.Client 의 정적(static) 상태(버퍼풀/저장소/파일명 카운터/최대 블럭·평문 사이즈)를
 * 서버 인스턴스 단위로 변환한 컨텍스트. 값 산출 로직은 구 Client.init 과 동일(무변경).
 */
final class ServerContext {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerContext.class);

	final ByteBufferPool pool;

	final Path repository;

	final AtomicLong fd_counter = new AtomicLong(0);
	final ReentrantLock fd_counter_lock = new ReentrantLock();
	LocalDate fd_counter_last = LocalDate.now();
	final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

	/**
	 * 로컬 서버에서 설정된 버퍼로 보낼수 있는 최대 블럭 개수
	 */
	final int max_block;

	/**
	 * 로컬 서버에서 설정된 버퍼로 한번에 보낼수 있는 최대 평문 사이즈
	 */
	final int max_plain;

	/**
	 * ServerContext (구 Client.init 로직 무변경 이식)
	 * @param bufferSize
	 * @param maxPoolSize
	 * @param repositoryRoot
	 */
	ServerContext(int bufferSize, int maxPoolSize, String repositoryRoot) {
		pool = new ByteBufferPool("net", bufferSize, maxPoolSize * 2/* net nio + file nio */);
		repository = Paths.get(repositoryRoot);
		try {
			Directory.createDirectories(repository);
		} catch (Exception e) {
			LOGGER.error("[runtime exception]", e);
		}

		max_block = pool.bufferSize() / ProtcolAesTransfer.BLOCK_SIZE;
		max_plain = pool.bufferSize() - ProtcolAesTransfer.Head.size - ProtcolAesTransfer.BLOCK_SIZE;

		LOGGER.info(String.format("\n"
			+ "[대칭키 교환 암복호화 스펙 : Handshake] \n"
			+ "RSA(2048BIT) \n"
			+ "[데이터 교환 암복호화 스펙] \n"
			+ "AES(128BIT)+CBC+PKCS7+HMACSHA256 \n"
			+ "BUFFER_SIZE      : %8d \n"
			+ "POOL_SIZE        : %8d \n"
			+ "AES_MAX_BLOCK    : %8d \n"
			+ "AES_MAX_PLAIN    : %8d \n"
			+ "AES_KEY_SIZE     : %8d \n"
			+ "AES_NONCE_SIZE   : %8d \n"
			+ "AES_HMAC_SIZE    : %8d \n"
			+ "AES_BLOCK_COUNT  : %8d \n"
			+ "AES_RESERVE_SIZE : %8d \n"
			+ "AES_BLOCK_SIZE   : %8d \n"
			+ "AES_TRANS_HEAD_SIZE = AES_NONCE_SIZE + AES_HMAC_SIZE + AES_BLOCK_COUNT + AES_RESERVE_SIZE : %d"
			, pool.bufferSize()
			, pool.poolSize()
			, max_block
			, max_plain
			, ProtcolAesTransfer.KEY_SIZE
			, ProtcolAesTransfer.Head._00_NONCE_SIZE
			, ProtcolAesTransfer.Head._01_HMAC_SIZE
			, ProtcolAesTransfer.Head._02_BLOCK_COUNT
			, ProtcolAesTransfer.Head._03_RESERVE_SIZE
			, ProtcolAesTransfer.BLOCK_SIZE
			, ProtcolAesTransfer.Head.size
		));
	}
}
