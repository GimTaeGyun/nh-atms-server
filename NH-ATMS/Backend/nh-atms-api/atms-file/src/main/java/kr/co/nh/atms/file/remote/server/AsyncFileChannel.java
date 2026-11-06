package kr.co.nh.atms.file.remote.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.zip.*;

/**
 * AsyncFileChannel
 * 구 새마을금고 atmf.lib (atmf.server.AsyncFileChannel) 원본 이식. 패키지만 변경.
 */
class AsyncFileChannel {
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncFileChannel.class);

	/**
	 * WriteCompletionHandler
	 */
	class WriteCompletionHandler implements CompletionHandler<Integer, Object> {
		private AsyncFileChannel as;
		private AysncFileEventHandler oncomp;

		/**
		 * WriteCompletionHandler
		 *
		 * @param as
		 */
		public WriteCompletionHandler(AsyncFileChannel as) {
			this.as = as;
		}

		/**
		 * completed
		 *
		 * @param io The result of the I/O operation.
		 * @param v  The object attached to the I/O operation when it was initiated.
		 */
		public void completed(Integer io, Object v) {
			getOncomp().invoke(new AysncFileEventArgs(as, AysncFileOper.write, io, null, v));
		}

		/**
		 * failed
		 *
		 * @param t The exception to indicate why the I/O operation failed
		 * @param v The object attached to the I/O operation when it was initiated.
		 */
		public void failed(Throwable t, Object v) {
			getOncomp().invoke(new AysncFileEventArgs(as, AysncFileOper.write, -1, t, v));
		}

		public AysncFileEventHandler getOncomp() {
			return oncomp;
		}

		public void setOncomp(AysncFileEventHandler oncomp) {
			this.oncomp = oncomp;
		}
	}

	/**
	 * ReadCompletionHandler
	 */
	class ReadCompletionHandler implements CompletionHandler<Integer, Object> {
		private AsyncFileChannel as;
		private AysncFileEventHandler oncomp;

		/**
		 * ReadCompletionHandler
		 *
		 * @param as
		 */
		public ReadCompletionHandler(AsyncFileChannel as) {
			this.as = as;
		}

		/**
		 * completed
		 *
		 * @param io The result of the I/O operation.
		 * @param v  The object attached to the I/O operation when it was initiated.
		 */
		public void completed(Integer io, Object v) {
			getOncomp().invoke(new AysncFileEventArgs(as, AysncFileOper.read, io, null, v));
		}

		/**
		 * failed
		 *
		 * @param t The exception to indicate why the I/O operation failed
		 * @param v The object attached to the I/O operation when it was initiated.
		 */
		public void failed(Throwable t, Object v) {
			getOncomp().invoke(new AysncFileEventArgs(as, AysncFileOper.read, -1, t, v));
		}

		public AysncFileEventHandler getOncomp() {
			return oncomp;
		}

		public void setOncomp(AysncFileEventHandler oncomp) {
			this.oncomp = oncomp;
		}
	}

	private Path path;
	private File file;
	private AsynchronousFileChannel channel;
	private WriteCompletionHandler writeHanlder = new WriteCompletionHandler(this);
	private ReadCompletionHandler readHanlder = new ReadCompletionHandler(this);
	private long position = 0;


	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public AsynchronousFileChannel getChannel() {
		return channel;
	}

	public void setChannel(AsynchronousFileChannel channel) {
		this.channel = channel;
	}

	public WriteCompletionHandler getWriteHanlder() {
		return writeHanlder;
	}

	public void setWriteHanlder(WriteCompletionHandler writeHanlder) {
		this.writeHanlder = writeHanlder;
	}

	public ReadCompletionHandler getReadHanlder() {
		return readHanlder;
	}

	public void setReadHanlder(ReadCompletionHandler readHanlder) {
		this.readHanlder = readHanlder;
	}

	public long getPosition() {
		return position;
	}

	public void setPosition(long position) {
		this.position = position;
	}

	/**
	 * open
	 *
	 * @param path
	 * @param token
	 * @param options
	 * @return
	 * @throws IOException
	 */
	public static AsyncFileChannel open(Path path, Object token, OpenOption... options) throws IOException {
		return new AsyncFileChannel(path, token, options);
	}

	/**
	 * AsyncFileChannel
	 *
	 * @param path
	 * @param token
	 * @param options
	 * @throws IOException
	 */
	private AsyncFileChannel(Path path, Object token, OpenOption... options) throws IOException {
		try {
			if (getChannel() != null && getChannel().isOpen()) {
				getChannel().close();
			}
		} catch (Exception e) {
			LOGGER.error("[runtime exception]", e);
		}
		setChannel(null);
		this.setPath(path);
		File _file = path.toFile();
		AsynchronousFileChannel _channel = AsynchronousFileChannel.open(path, options);
		this.setFile(_file);
		this.setChannel(_channel);
	}

	/**
	 * writen
	 *
	 * @param bb
	 * @param token
	 * @param oncomp
	 */
	public void writen(ByteBuffer bb, Object token, AysncFileEventHandler oncomp) {
		getWriteHanlder().setOncomp((e) -> {
			setPosition(getPosition() + e.getTransfer());
			if (bb.hasRemaining()) {
				getChannel().write(bb, getPosition(), token, getWriteHanlder());
				return;
			}
			getWriteHanlder().setOncomp(null);
			if (oncomp != null) {
				oncomp.invoke(e);
			}
		});
		getChannel().write(bb, getPosition(), token, getWriteHanlder());
	}

	/**
	 * readn
	 *
	 * @param bb
	 * @param token
	 * @param oncomp
	 */
	public void readn(ByteBuffer bb, Object token, AysncFileEventHandler oncomp) {
		getReadHanlder().setOncomp((e) -> {
			if (e.getTransfer() != -1) {
				setPosition(getPosition() + e.getTransfer());
				if (bb.hasRemaining()) {
					getChannel().read(bb, getPosition(), token, getReadHanlder());
					return;
				}
			}
			getReadHanlder().setOncomp(null);
			if (oncomp != null) {
				oncomp.invoke(e);
			}
		});
		getChannel().read(bb, getPosition(), token, getReadHanlder());
	}

	/**
	 * write
	 *
	 * @param bb
	 * @param token
	 * @param oncomp
	 */
	public void write(ByteBuffer bb, Object token, AysncFileEventHandler oncomp) {
		getWriteHanlder().setOncomp((e) -> {
			setPosition(getPosition() + e.getTransfer());
			getWriteHanlder().setOncomp(null);
			if (oncomp != null) {
				oncomp.invoke(e);
			}
		});
		getChannel().write(bb, getPosition(), token, getWriteHanlder());
	}

	/**
	 * read
	 *
	 * @param bb
	 * @param token
	 * @param oncomp
	 */
	public void read(ByteBuffer bb, Object token, AysncFileEventHandler oncomp) {
		getReadHanlder().setOncomp((e) -> {
			if (e.getTransfer() != -1) {
				setPosition(getPosition() + e.getTransfer());
			}
			getReadHanlder().setOncomp(null);
			if (oncomp != null) {
				oncomp.invoke(e);
			}
		});
		getChannel().read(bb, getPosition(), token, getReadHanlder());
	}

	/**
	 * crc32
	 *
	 * @param bb
	 * @param token
	 * @param oncomp
	 */
	public void crc32(ByteBuffer bb, Object token, AysncFileEventHandler oncomp) {
		ByteBuffer[] bbb = new ByteBuffer[]{bb};
		if (!bbb[0].hasArray()) {
			bbb[0] = ByteBuffer.wrap(new byte[1024 * 64]);
		}
		CRC32 crc = new CRC32();
		getReadHanlder().setOncomp((e) -> {
			if (e.getTransfer() == -1) {
				setPosition(0);
				e.setTransfer(0);
				e.setLtransfer(crc.getValue());
				getReadHanlder().setOncomp(null);
				if (oncomp != null) {
					oncomp.invoke(e);
				}
				return;
			}
			setPosition(getPosition() + e.getTransfer());
			crc.update(bbb[0].array(), 0, e.getTransfer());

			bbb[0].clear();
			getChannel().read(bbb[0], getPosition(), token, getReadHanlder());
		});
		setPosition(0);
		bbb[0].clear();
		getChannel().read(bbb[0], getPosition(), token, getReadHanlder());
	}

	/**
	 * length
	 *
	 * @return
	 */
	public long length() {
		if (getFile() == null) {
			try {
				return Files.size(getPath());
			} catch (Exception e) {
				return -1;
			}
		}
		return getFile().length();
	}

	public Path path() {
		return getPath();
	}

	public File file() {
		return getFile();
	}

	/**
	 * close
	 */
	public void close() {
		try {
			if (getChannel().isOpen()) {
				getChannel().close();
			}
		} catch (Exception e) {
			LOGGER.error("[runtime exception]", e);
		}
	}

	/**
	 * delete
	 */
	public void delete() {
		try {
			if (getFile() == null) {
				Files.delete(getPath());
				return;
			}
			if (getFile().exists()) {
				getFile().delete();
			}
		} catch (Exception e) {
			LOGGER.error("[runtime exception]", e);
		}
	}


	public void position(long position) {
		this.setPosition(position);
	}

	public long position() {
		return getPosition();
	}

	/**
	 * available
	 *
	 * @return
	 */
	public long available() {
		if (getFile() == null) {
			return -1;
		}
		return getFile().length() - getPosition();
	}
}


/**
 * AysncFileEventHandler
 */
@FunctionalInterface
interface AysncFileEventHandler{
	void invoke(AysncFileEventArgs args);
}

enum AysncFileOper{ none, open, write, read };

/**
 * AysncFileEventArgs
 */
class AysncFileEventArgs {
	private AsyncFileChannel file;
	private AysncFileOper oper;
	private int transfer;
	private long ltransfer;
	private Throwable exception;
	private Object token;

	public AysncFileEventArgs(AsyncFileChannel file, AysncFileOper oper, int transfer, Throwable exception, Object token) {
		this.setFile(file);
		this.setOper(oper);
		this.setTransfer(transfer);
		this.setLtransfer(transfer);
		this.setException(exception);
		this.setToken(token);
	}

	public AsyncFileChannel getFile() {
		return file;
	}

	public void setFile(AsyncFileChannel file) {
		this.file = file;
	}

	public AysncFileOper getOper() {
		return oper;
	}

	public void setOper(AysncFileOper oper) {
		this.oper = oper;
	}

	public int getTransfer() {
		return transfer;
	}

	public void setTransfer(int transfer) {
		this.transfer = transfer;
	}

	public long getLtransfer() {
		return ltransfer;
	}

	public void setLtransfer(long ltransfer) {
		this.ltransfer = ltransfer;
	}

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}

	public Object getToken() {
		return token;
	}

	public void setToken(Object token) {
		this.token = token;
	}
}
