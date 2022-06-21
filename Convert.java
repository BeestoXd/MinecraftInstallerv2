package jdk.test.lib;

import java.math.BigInteger;
import java.util.HexFormat;
import java.security.spec.EdECPoint;

/**
 * Utility class containing conversions between strings, arrays, numeric
 * values, and other types.
 */

public class Convert {

    // Expand a single byte to a byte array
    public static byte[] byteToByteArray(byte v, int length) {
        byte[] result = new byte[length];
        result[0] = v;
        return result;
    }

    /*
     * Convert a hexadecimal string to the corresponding little-ending number
     * as a BigInteger. The clearHighBit argument determines whether the most
     * significant bit of the highest byte should be set to 0 in the result.
     */
    public static
    BigInteger hexStringToBigInteger(boolean clearHighBit, String str) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < str.length() / 2; i++) {
            int curVal = Character.digit(str.charAt(2 * i), 16);
            curVal <<= 4;
            curVal += Character.digit(str.charAt(2 * i + 1), 16);
            if (clearHighBit && i == str.length() / 2 - 1) {
                curVal &= 0x7F;
            }
            result = result.add(BigInteger.valueOf(curVal).shiftLeft(8 * i));
        }
        return result;
    }

    private static EdECPoint byteArrayToEdPoint(byte[] arr) {
        byte msb = arr[arr.length - 1];
        boolean xOdd = (msb & 0x80) != 0;
        arr[arr.length - 1] &= (byte) 0x7F;
        reverse(arr);
        BigInteger y = new BigInteger(1, arr);
        return new EdECPoint(xOdd, y);
    }

    public static EdECPoint hexStringToEdPoint(String str) {
        return byteArrayToEdPoint(HexFormat.of().parseHex(str));
    }

    private static void swap(byte[] arr, int i, int j) {
        byte tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    private static void reverse(byte [] arr) {
        int i = 0;
        int j = arr.length - 1;

        while (i < j) {
            swap(arr, i, j);
            i++;
            j--;
        }
    }
}


