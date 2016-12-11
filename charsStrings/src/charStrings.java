/**
 * Created by Jose Rogado on 13-01-2016.
 */

import java.io.*;

public class charStrings {

    public static void main(String[] args) {

        // String symbols = "abcdefghijklmnopqrstuvwxyz*#";
        char[] passHash = new char[]{0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x6f, 0xbb, 0xad, 0xe8, 0x33, 0x96, 0xa4, 0x9b, 0x1a, 0xfc, 0x9a};
        char[] foundPass = new char[]{0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x6f};

        System.out.print("\nfoundPass as chars: ");
        for (int i = 0; i < foundPass.length; i++)
            System.out.print(foundPass[i]);

        System.out.print("\npassHash in hex: ");
        for (int i = 0; i < passHash.length; i++)
            System.out.print(String.format("%02x", (int) passHash[i])+" ");

/*
        System.out.println();
        for (int i = 0; i < passHash.length; i++)
            System.out.print(Integer.toHexString((int) passHash[i])+" ");
*/

        System.out.print("\npassHash as ints: ");
        int[] passInt = chartoInt(passHash, true);
        for (int i = 0; i < passInt.length; i++) {
            System.out.print(Integer.toHexString(passInt[i]) + " ");
        }
        char[] passChar;
        System.out.print("\npassHash as chars: ");
        passChar = intoChar(passInt, true);
        // System.out.println(passChar.length);
        for (int i = 0; i < 6; i++) {
            System.out.print(passChar[i]);
        }
        System.out.print("\nSizes: " + Integer.BYTES + " " + Character.BYTES);

    }
    // Convert chars to int (Little or Big Endian)
    private static int[] chartoInt(char[] pass, boolean LittleEndian) {
        if (pass.length > 16)
            return null;

        int[] value = new int[4];
        int intSize = Integer.BYTES;
        int add = ((pass.length % intSize) != 0) ? 1 : 0;
        int nints = pass.length/intSize + add;
        for (int i = 0; i < nints; i++) { // Big Endian
            if (LittleEndian)
                value[i] = pass[4*i+3] << 24 | pass[4*i+2]<< 16 | pass[4*i+1] << 8 | pass[4*i];
            else
                value[i] = pass[4*i] << 24 | pass[4*i+1]<< 16 | pass[4*i+2] << 8 | pass[4*i+3];
        }
//        value[0] = pass[0] << 24 | pass[1]<< 16 | pass[2] << 8 | pass[3];
//        value[1] = pass[4] << 24 | pass[5]<< 16 | pass[6] << 8 | pass[7];
//        value[2] = pass[8] << 24 | pass[9]<< 16 | pass[10] << 8 | pass[11];
//        value[3] = pass[12] << 24 | pass[13]<< 16 | pass[14] << 8 | pass[15];
        return value;
    }

	// Convert int to chars (Little or Big Endian)
	private static char[] intoChar(int[] pass, boolean LittleEndian) {
        if (pass.length > 4)
            return null;
        int intSize = Integer.BYTES;
        int nchars = pass.length*intSize;

        char[] value = new char[nchars];
        char mask = 0xFF;
        for (int i = 0; i < nchars; i++) {
            int j = i%intSize;
            int k = i/intSize+1;
            if (LittleEndian) {
                value[(k*intSize-1) - j] = (char) ((char) (pass[i / intSize] >>> (Integer.SIZE - (j + 1) * Byte.SIZE)) & mask);
                // System.out.print((k*intSize-1) - j + " ");
            } else
                value[i] = (char) ((char)(pass[i/intSize] >>> (Integer.SIZE-(j+1)*Byte.SIZE)) & mask);
        }

//        value[0] = (char) ((char)(pass[0] >>> 24) & mask);
//        value[1] = (char) ((char)(pass[0] >>> 16) & mask);
//        value[2] = (char) ((char)(pass[0] >>> 8) & mask);
//        value[3] = (char) ((char)(pass[0]) & mask);
//
//        value[4] = (char) ((char)(pass[1] >>> 24) & mask);
//        value[5] = (char) ((char)(pass[1] >>> 16) & mask);
//        value[6] = (char) ((char)(pass[1] >>> 8) & mask);
//        value[7] = (char) ((char)(pass[1]) & mask);

        return value;

    }
}
