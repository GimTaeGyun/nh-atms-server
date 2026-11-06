package kr.co.nh.atms.file.remote.util;

/**
 * 구 새마을금고 atmf.lib (atmf.util.Hex) 원본 이식. 패키지만 변경.
 */
final public class Hex {
	/**
	 * dump
	 * @param bin
	 * @return
	 */
	public static String dump(byte[] bin) {
		if (bin == null || bin.length == 0) { return ""; }

		StringBuilder buf = new StringBuilder();
		int size = bin.length;
		byte[] data = bin;
		StringBuilder ascii = new StringBuilder(24);

		for (int i = 0; i < size; ++i) {
			if (i % 16 == 0) {
				buf.append(String.format("%08x  ", i));
			}

			buf.append(String.format("%02x ", data[i] & 0xFF));

			if (data[i] >= 32 && data[i] <= 126) {
				ascii.append((char) data[i]);
			} else {
				ascii.append('.');
			}

			if ((i + 1) % 8 == 0 || i + 1 == size) {
				buf.append(" ");
				if ((i + 1) % 16 == 0) {
					buf.append(String.format("|  %s  |\n", ascii.toString()));
					ascii.setLength(0);
				} else if (i + 1 == size) {
					if ((i + 1) % 16 <= 8) {
						buf.append(" ");
					}
					for (int j = (i + 1) % 16; j < 16; ++j) {
						buf.append("   ");
					}
					while (ascii.length() < 16) {
						ascii.append(" ");
					}
					buf.append(String.format("|  %s  |\n", ascii.toString()));
					ascii.setLength(0);
				}
			}
		}

		if (buf.length() > 0) {
			buf.setLength(buf.length() - 2);
		}

		return buf.toString();
	}
}
