package main;

import java.util.List;

public class Utils {
	public static void copy1d(Object src, Object dest, int s) {
		//noinspection SuspiciousSystemArraycopy
		System.arraycopy(src, 0, dest, 0, s);
	}

	public static void copy2d(int[][] src, int[][] dest, int s1, int s2) {
		for (int i = 0; i < s1; i++) {
			copy1d(src[i], dest[i], s2);
		}
	}

	public static void copy2dNullSafe(int[][] src, int[][] dest, int s1, int s2) {
		for (int i = 0; i < s1; i++) {
			if (src[i] == null) {
				dest[i] = null;
			}
			else if (dest[i] == null) {
				dest[i] = new int[s2];
				copy1d(src[i], dest[i], s2);
			}
			else {
				copy1d(src[i], dest[i], s2);
			}
		}
	}

	@SuppressWarnings("SameParameterValue")
	public static void copy3d(int[][][] src, int[][][] dest, int s1, int s2, int s3) {
		for (int i = 0; i < s1; i++) {
			copy2d(src[i], dest[i], s2, s3);
		}
	}

	public static void copy2d(byte[][] src, byte[][] dest, int s1, int s2) {
		for (int i = 0; i < s1; i++) {
			copy1d(src[i], dest[i], s2);
		}
	}

	public static void copy3d(byte[][][] src, byte[][][] dest, int s1, int s2, int s3) {
		for (int i = 0; i < s1; i++) {
			copy2d(src[i], dest[i], s2, s3);
		}
	}
	public static String repeat(String s, int count) {
		StringBuilder sb = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			sb.append(s);
		}
		return sb.toString();
	}

	public static <T> T swapEndAndRemove(List<T> list, int index) {
		// This rearranging approach selects from the middle but trims from the end for efficiency
		T removed = list.set(index, list.get(list.size() - 1));
		list.remove(list.size() - 1);
		return removed;
	}
}
