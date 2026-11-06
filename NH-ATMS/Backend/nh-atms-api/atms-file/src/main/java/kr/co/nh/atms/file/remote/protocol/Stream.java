package kr.co.nh.atms.file.remote.protocol;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * 구 새마을금고 atmf.lib (atmf.server.Stream) 원본 이식. 패키지만 변경.
 */
final public class Stream implements Closeable {

	private byte[] buffer;
	private int position;
	private int length;

	private InputStream ins;
	private OutputStream ous;
	private boolean proxy = false;

	private IOException ioe;


	public Stream() {
		alloc();
	}

	public Stream(int capacity) {
		alloc(capacity);
	}

	public Stream(byte[] array) {
		alloc(array);
	}

	public Stream(InputStream stream) {
		setIns(stream);
		setProxy(true);
	}

	public Stream(OutputStream stream) {
		setOus(stream);
		setProxy(true);
	}

	public byte[] toArray() {
		if (isProxy()) { return null; }
		return Arrays.copyOf(getBuffer(), getLength());
	}

	public byte[] array() {
		if (isProxy()) { return null; }
		return getBuffer();
	}

	public ByteBuffer wrap() {
		if (isProxy()) { return null; }
		return ByteBufferLE.wrap(getBuffer(), 0, getLength());
	}

	public ByteBuffer wrapCapacity() {
		if (isProxy()) { return null; }
		return ByteBufferLE.wrap(getBuffer());
	}

	public IOException getIOException() {
		return getIoe();
	}

	public InputStream getInputStream() {
		return getIns();
	}

	public OutputStream getOutputStream() {
		return getOus();
	}

	public int read() {
		if (isProxy()) {
			if (getIns() != null) {
				try {
					return getIns().read();
				} catch (IOException e) {
					setIoe(e);
					return -2;
				}
			}
			return -1;
		}
		if (position >= getLength()) {
			return -1;
		}
		return getBuffer()[position++] & 0xFF;
	}

	public int read(byte[] b, int off, int len) {
		if (isProxy()) {
			if (getIns() != null) {
				try {
					return getIns().read(b, off, len);
				} catch (IOException e) {
					setIoe(e);
					return -2;
				}
			}
			return -1;
		}
		if (position >= getLength()) {
			return -1;
		}
		int io = Math.min(len, getLength() - position);
		System.arraycopy(getBuffer(), position, b, off, io);
		position += io;
		return io;
	}

	public int write(byte b) throws IOException {
		if (isProxy()) {
			if (getOus() != null) {
				try {
					getOus().write(b);
					return 1;
				} catch (IOException e) {
					setIoe(e);
					return -2;
				}
			}
			return -1;
		}
		capacity(position + 1);
		getBuffer()[position++] = b;
		if (position > getLength()) {
			setLength(position);
		}
		return 1;
	}

	public int write(byte[] b, int off, int len) {
		if (isProxy()) {
			if (getOus() != null) {
				try {
					getOus().write(b, off, len);
					return len;
				} catch (IOException e) {
					setIoe(e);
					return -2;
				}
			}
			return -1;
		}
		capacity(position + len);
		System.arraycopy(b, off, getBuffer(), position, len);
		position += len;
		if (position > getLength()) {
			setLength(position);
		}
		return len;
	}

	private void capacity(int newSize) {
		if (newSize > getBuffer().length) {
			int newCapacity = Math.max(getBuffer().length * 2, newSize);
			setBuffer(Arrays.copyOf(getBuffer(), newCapacity));
		}
	}

	public int size() {
		if (isProxy()) { return -1; }
		return getLength();
	}

	public void size(int length) {
		if (isProxy()) { return; }
		this.setLength(length > getBuffer().length ? getBuffer().length : length);
		if (position > this.getLength())
			position = length;
	}

	public int capacty() {
		if (isProxy()) { return -1; }
		return getBuffer().length;
	}

	public int available() {
		if (isProxy()) {
			if (getIns() != null) {
				try {
					return getIns().available();
				} catch (IOException e) {
					setIoe(e);
					return -2;
				}
			}
			return -1;
		}
		return getLength() - position;
	}

	public int position() {
		if (isProxy()) { return -1; }
		return position;
	}

	public int position(int newPosition) {
		if (isProxy()) { return -1; }
		if (newPosition < 0 || newPosition > getLength()) { return -1; }
		int old = position;
		position = newPosition;
		return old;
	}

	public int reset() {
		if (isProxy()) {
			if (getIns() != null) {
				try {
					getIns().reset();
					return 0;
				} catch (IOException e) {
					setIoe(e);
					return -2;
				}
			}
			return -1;
		}
		position = 0;
		setLength(0);
		return 0;
	}

	public int compact() {
		if (isProxy()) { return -1; }
		setLength(getLength() - position);
		if (getLength() > 0) {
			System.arraycopy(getBuffer(), position, getBuffer(), 0, getLength());
		}
		position = 0;
		return getLength();
	}

	public int alloc() {
		if (isProxy()) { return -1; }
		if (getBuffer() == null || getBuffer().length != 512) {
			setBuffer(new byte[512]);
		}
		position = 0;
		setLength(0);
		return getBuffer().length;
	}

	public int alloc(int capacity) {
		if (isProxy()) { return -1; }
		setBuffer(new byte[capacity]);
		position = 0;
		setLength(0);
		return getBuffer().length;
	}

	public int alloc(byte[] array) {
		if (isProxy()) { return -1; }
		setBuffer(array);
		position = 0;
		setLength(array.length);
		return getBuffer().length;
	}

	@Override
	public void close() {
		try { if (getIns() != null) getIns().close(); } catch (IOException e) { e.printStackTrace(); }
		try { if (getOus() != null) getOus().close(); } catch (IOException e) { e.printStackTrace(); }
		setBuffer(null);
	}

	public int flush() {
		if (isProxy()) {
			if (getOus() != null) {
				try {
					getOus().flush();
					return 0;
				} catch (IOException e) {
					setIoe(e);
					return -2;
				}
			}
			return -1;
		}
		return 0;
	}

	public byte[] getBuffer() {
		return buffer;
	}

	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public InputStream getIns() {
		return ins;
	}

	public void setIns(InputStream ins) {
		this.ins = ins;
	}

	public OutputStream getOus() {
		return ous;
	}

	public void setOus(OutputStream ous) {
		this.ous = ous;
	}

	public boolean isProxy() {
		return proxy;
	}

	public void setProxy(boolean proxy) {
		this.proxy = proxy;
	}

	public IOException getIoe() {
		return ioe;
	}

	public void setIoe(IOException ioe) {
		this.ioe = ioe;
	}
}
