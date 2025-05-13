package me.monoto.customseeds.utils;

import java.security.SecureRandom;

public class Utilities {
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RND = new SecureRandom();

    public static String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RND.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
