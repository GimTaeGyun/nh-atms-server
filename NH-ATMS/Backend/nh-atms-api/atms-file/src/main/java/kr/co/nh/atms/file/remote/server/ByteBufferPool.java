package kr.co.nh.atms.file.remote.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.locks.*;

import kr.co.nh.atms.file.remote.protocol.ByteBufferLE;

/**
 * ByteBufferPool
 * 구 새마을금고 atmf.lib (atmf.server.ByteBufferPool) 원본 이식. 패키지만 변경.
 */
final class ByteBufferPool {
	private static Logger LOGGER = LoggerFactory.getLogger(ByteBufferPool.class);

	private String name;
	private int size;
	private int max;
	private int min;
	private Queue<ByteBuffer> pool;
	private ReentrantLock lock = new ReentrantLock();

	/**
	 * ByteBufferPool
	 * @param name
	 * @param bufferSize
	 * @param maxPoolSize
	 */
	public ByteBufferPool(String name, int bufferSize, int maxPoolSize) {
		this.setName(name);
		this.setSize(bufferSize);
		this.setMax(maxPoolSize);
		this.setMin(Math.max(1, getMax() / 2));
		this.setPool(new LinkedList<>());
		for (int i = 0; i < getMin(); i++) {
			getPool().add(ByteBufferLE.allocateDirect(getSize(), true));
		}
	}

	/**
	 * poolSize
	 * @return
	 */
	public int poolSize() {
		try {
			getLock().lock();
			return getPool().size();
		} finally {
			getLock().unlock();
		}

	}

	/**
	 * bufferSize
	 * @return
	 */
	public int bufferSize() {
		return this.getSize();
	}

	/**
	 * acquire
	 * @return
	 */
	public ByteBuffer acquire() {
		try {
			getLock().lock();
			ByteBuffer buffer = null;
			if (getPool().isEmpty()) {
				buffer = ByteBufferLE.allocateDirect(getSize(), true);
			}else {
				buffer = getPool().poll();
			}
			buffer.clear();
			if(LOGGER.isTraceEnabled()) {
				LOGGER.trace(String.format("[ByteBuffer.%s.acquire:%d]", getName(), getPool().size()));
			}
			return buffer;
		} finally {
			getLock().unlock();
		}
	}

	/**
	 * acquire
	 * @param count
	 * @return
	 */
	public ByteBuffer[] acquire(int count) {
		try {
			getLock().lock();
			ByteBuffer[] bus = new ByteBuffer[count];
			for (int i = 0; i < bus.length; i++) {
				ByteBuffer buffer = null;
				if (getPool().isEmpty()) {
					buffer = ByteBufferLE.allocateDirect(getSize(), true);
				}else {
					buffer = getPool().poll();
				}
				buffer.clear();
				bus[i] = buffer;
			}
			if(LOGGER.isTraceEnabled()) {
				LOGGER.trace(String.format("[ByteBuffer.%s.acquire:%d/%d]", getName(), count, getPool().size()));
			}
			return bus;
		} finally {
			getLock().unlock();
		}
	}

	/**
	 * release
	 * @param buffers
	 */
	public void release(ByteBuffer... buffers) {
		try {
			getLock().lock();
			for (ByteBuffer buffer : buffers) {
				if (buffer == null || !buffer.isDirect()) continue;
				buffer.clear();
				if (getPool().size() < getMin()) {
					getPool().offer(buffer);
				}else{
					try {
						Method cleanerMethod = buffer.getClass().getMethod("cleaner");
						Object cleaner = cleanerMethod.invoke(buffer);
						Method cleanMethod = cleaner.getClass().getMethod("clean");
						cleanMethod.invoke(cleaner);
					}catch(Exception e) {
						;
					}
				}
				if(LOGGER.isTraceEnabled()) {
					LOGGER.trace(String.format("[ByteBuffer.%s.release:%d]", getName(), getPool().size()));
				}
			}
		} finally {
			getLock().unlock();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public Queue<ByteBuffer> getPool() {
		return pool;
	}

	public void setPool(Queue<ByteBuffer> pool) {
		this.pool = pool;
	}

	public ReentrantLock getLock() {
		return lock;
	}

	public void setLock(ReentrantLock lock) {
		this.lock = lock;
	}
}
