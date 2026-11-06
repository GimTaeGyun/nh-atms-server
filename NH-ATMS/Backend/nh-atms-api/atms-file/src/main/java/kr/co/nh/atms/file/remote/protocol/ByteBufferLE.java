package kr.co.nh.atms.file.remote.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 구 새마을금고 atmf.lib (atmf.server.ByteBufferLE) 원본 이식. 패키지만 변경.
 */
final public class ByteBufferLE {

	/**
	 * putBuffer
	 */
	public static int putBuffer(ByteBuffer src, ByteBuffer dst) {
		if (!src.hasRemaining()) { return 0; }
		int copy = Math.min(src.remaining(), dst.remaining());
		int limit = src.limit();
		src.limit(src.position() + copy);
		dst.put(src);
		src.limit(limit);
		return copy;
	}

	/**
	 * allocateDirect
	 */
	public static ByteBuffer allocateDirect(int capacity, boolean throwUseHeapBuffer) {
		if (throwUseHeapBuffer) {
			try {
				ByteBuffer bb = ByteBuffer.allocateDirect(capacity);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				return bb;
			} catch (Throwable t) {
				ByteBuffer bb = ByteBuffer.allocate(capacity);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				return bb;
			}
		} else {
			ByteBuffer bb = ByteBuffer.allocateDirect(capacity);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			return bb;
		}
	}

	/**
	 * allocate
	 */
	public static ByteBuffer allocate(int capacity) {
		ByteBuffer bb = ByteBuffer.allocate(capacity);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb;
	}

	/**
	 * wrap
	 */
	public static ByteBuffer wrap(byte[] array) {
		ByteBuffer bb = ByteBuffer.wrap(array);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb;
	}

	/**
	 * wrap
	 */
	public static ByteBuffer wrap(byte[] array, int offset, int length) {
		ByteBuffer bb = ByteBuffer.wrap(array, offset, length);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb;
	}
}
