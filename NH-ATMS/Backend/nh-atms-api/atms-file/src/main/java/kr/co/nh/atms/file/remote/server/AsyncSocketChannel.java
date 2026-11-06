package kr.co.nh.atms.file.remote.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;

/**
 * AsyncSocketChannel
 * 구 새마을금고 atmf.lib (atmf.server.AsyncSocketChannel) 원본 이식. 패키지만 변경.
 */
class AsyncSocketChannel {
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncSocketChannel.class);

	/**
	 * ConnectCompletionHandler
	 */
	class ConnectCompletionHandler implements CompletionHandler<Void, Object> {
		private AsyncSocketChannel as;
		private AysncSocketEventHandler oncomp;

		public ConnectCompletionHandler(AsyncSocketChannel as) {
			this.as = as;
		}

		/**
		 * completed
		 *
		 * @param v0 The result of the I/O operation.
		 * @param v  The object attached to the I/O operation when it was initiated.
		 */
		public void completed(Void v0, Object v) {
			getOncomp().invoke(new AysncSocketEventArgs(as, AysncSocketOper.open, 0, null, v));
		}

		/**
		 * failed
		 *
		 * @param t The exception to indicate why the I/O operation failed
		 * @param v The object attached to the I/O operation when it was initiated.
		 */
		public void failed(Throwable t, Object v) {
			getOncomp().invoke(new AysncSocketEventArgs(as, AysncSocketOper.open, -1, t, v));
		}

		public AysncSocketEventHandler getOncomp() {
			return oncomp;
		}

		public void setOncomp(AysncSocketEventHandler oncomp) {
			this.oncomp = oncomp;
		}
	}

	/**
	 * SendCompletionHandler
	 */
	class SendCompletionHandler implements CompletionHandler<Integer, Object> {
		private AsyncSocketChannel as;
		private AysncSocketEventHandler oncomp;

		public SendCompletionHandler(AsyncSocketChannel as) {
			this.as = as;
		}

		/**
		 * completed
		 *
		 * @param io The result of the I/O operation.
		 * @param v  The object attached to the I/O operation when it was initiated.
		 */
		public void completed(Integer io, Object v) {
			getOncomp().invoke(new AysncSocketEventArgs(as, AysncSocketOper.send, io, null, v));
		}

		/**
		 * failed
		 *
		 * @param t The exception to indicate why the I/O operation failed
		 * @param v The object attached to the I/O operation when it was initiated.
		 */
		public void failed(Throwable t, Object v) {
			getOncomp().invoke(new AysncSocketEventArgs(as, AysncSocketOper.send, -1, t, v));
		}

		public AysncSocketEventHandler getOncomp() {
			return oncomp;
		}

		public void setOncomp(AysncSocketEventHandler oncomp) {
			this.oncomp = oncomp;
		}
	}

	/**
	 * RecvCompletionHandler
	 */
	class RecvCompletionHandler implements CompletionHandler<Integer, Object> {
		private AsyncSocketChannel as;
		private AysncSocketEventHandler oncomp;

		public RecvCompletionHandler(AsyncSocketChannel as) {
			this.as = as;
		}

		/**
		 * completed
		 *
		 * @param io The result of the I/O operation.
		 * @param v  The object attached to the I/O operation when it was initiated.
		 */
		public void completed(Integer io, Object v) {
			getOncomp().invoke(new AysncSocketEventArgs(as, AysncSocketOper.recv, io, null, v));
		}

		/**
		 * failed
		 *
		 * @param t The exception to indicate why the I/O operation failed
		 * @param v The object attached to the I/O operation when it was initiated.
		 */
		public void failed(Throwable t, Object v) {
			getOncomp().invoke(new AysncSocketEventArgs(as, AysncSocketOper.recv, -1, t, v));
		}

		public AysncSocketEventHandler getOncomp() {
			return oncomp;
		}

		public void setOncomp(AysncSocketEventHandler oncomp) {
			this.oncomp = oncomp;
		}
	}

	private AsynchronousSocketChannel channel;
	private ConnectCompletionHandler openHanlder = new ConnectCompletionHandler(this);
	private SendCompletionHandler sendHanlder = new SendCompletionHandler(this);
	private RecvCompletionHandler recvHanlder = new RecvCompletionHandler(this);

	public AsynchronousSocketChannel getChannel() {
		return channel;
	}

	public void setChannel(AsynchronousSocketChannel channel) {
		this.channel = channel;
	}

	/**
	 * open
	 *
	 * @param ip
	 * @param port
	 * @param token
	 * @param oncomp
	 * @return
	 */
	public static AsyncSocketChannel open(String ip, int port, Object token, AysncSocketEventHandler oncomp) {
		return new AsyncSocketChannel(ip, port, token, oncomp);
	}

	/**
	 * AsyncSocketChannel
	 *
	 * @param channel
	 */
	protected AsyncSocketChannel(AsynchronousSocketChannel channel) {
		this.setChannel(channel);
	}


	/**
	 * AsyncSocketChannel
	 *
	 * @param ip
	 * @param port
	 * @param token
	 * @param oncomp
	 */
	protected AsyncSocketChannel(String ip, int port, Object token, AysncSocketEventHandler oncomp) {
		try {
			if (getChannel() != null && getChannel().isOpen()) {
				getChannel().close();
			}
		} catch (Exception e) {
			LOGGER.error("[runtime exception]", e);
		}
		setChannel(null);
		try {
			setChannel(AsynchronousSocketChannel.open());
			getChannel().setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			//channel = AsynchronousSocketChannel.open();
			getChannel().connect(new InetSocketAddress(ip, port), token, openHanlder);
			openHanlder.setOncomp((e) -> {
				openHanlder.setOncomp(null);
				oncomp.invoke(e);
			});
		} catch (Exception t) {
			CompletableFuture.runAsync(() -> {
				if (oncomp != null) {
					oncomp.invoke(new AysncSocketEventArgs(this, AysncSocketOper.open, -1, t, token));
				}
			});
		}
	}

	/**
	 * sendn
	 *
	 * @param bb
	 * @param token
	 * @param oncomp
	 */
	public void sendn(ByteBuffer bb, Object token, AysncSocketEventHandler oncomp) {
		sendHanlder.setOncomp((e) -> {
			if (e.getTransfer() == -1) {
				;
			} else if (bb.hasRemaining()) {
				getChannel().write(bb, token, sendHanlder);
				return;
			}
			sendHanlder.setOncomp(null);
			if (oncomp != null) {
				oncomp.invoke(e);
			}
		});
		getChannel().write(bb, token, sendHanlder);
	}

	/**
	 * recvn
	 *
	 * @param bb
	 * @param token
	 * @param oncomp
	 */
	public void recvn(ByteBuffer bb, Object token, AysncSocketEventHandler oncomp) {
		recvHanlder.setOncomp((e) -> {
			if (e.getTransfer() == -1) {
				;
			} else if (bb.hasRemaining()) {
				getChannel().read(bb, token, recvHanlder);
				return;
			}
			recvHanlder.setOncomp(null);
			if (oncomp != null) {
				oncomp.invoke(e);
			}
		});
		getChannel().read(bb, token, recvHanlder);
	}

	/**
	 * send
	 *
	 * @param bb
	 * @param token
	 * @param oncomp
	 */
	public void send(ByteBuffer bb, Object token, AysncSocketEventHandler oncomp) {
		sendHanlder.setOncomp((e) -> {
			sendHanlder.setOncomp(null);
			if (oncomp != null) {
				oncomp.invoke(e);
			}
		});
		getChannel().write(bb, token, sendHanlder);
	}

	/**
	 * recv
	 *
	 * @param bb
	 * @param token
	 * @param oncomp
	 */
	public void recv(ByteBuffer bb, Object token, AysncSocketEventHandler oncomp) {
		recvHanlder.setOncomp((e) -> {
			recvHanlder.setOncomp(null);
			if (oncomp != null) {
				oncomp.invoke(e);
			}
		});
		getChannel().read(bb, token, recvHanlder);
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
}

/**
 * AysncSocketEventHandler
 */
@FunctionalInterface
interface AysncSocketEventHandler{
	void invoke(AysncSocketEventArgs args);
}

enum AysncSocketOper{ none, open, send, recv };

/**
 * AysncSocketEventArgs
 */
class AysncSocketEventArgs {
	private AsyncSocketChannel sock;
	private AysncSocketOper oper;
	private int transfer;
	private Throwable exception;
	private Object token;

	public AysncSocketEventArgs(AsyncSocketChannel sock, AysncSocketOper oper, int transfer, Throwable exception, Object token) {
		this.setSock(sock);
		this.setOper(oper);
		this.setTransfer(transfer);
		this.setException(exception);
		this.setToken(token);
	}

	public AsyncSocketChannel getSock() {
		return sock;
	}

	public void setSock(AsyncSocketChannel sock) {
		this.sock = sock;
	}

	public AysncSocketOper getOper() {
		return oper;
	}

	public void setOper(AysncSocketOper oper) {
		this.oper = oper;
	}

	public int getTransfer() {
		return transfer;
	}

	public void setTransfer(int transfer) {
		this.transfer = transfer;
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
