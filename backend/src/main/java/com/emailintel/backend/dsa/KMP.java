package com.emailintel.backend.dsa;

public class KMP {
    public static boolean contains(String text, String pattern) {
        if (pattern.isEmpty()) return true;
        text = text.toLowerCase(); pattern = pattern.toLowerCase();
        int[] lps = new int[pattern.length()];
        for (int i=1, len=0; i<pattern.length();) {
            if (pattern.charAt(i)==pattern.charAt(len)) lps[i++]=++len;
            else if (len>0) len=lps[len-1]; else lps[i++]=0;
        }
        for (int i=0,j=0; i<text.length();) {
            if (text.charAt(i)==pattern.charAt(j)) { i++; j++; if (j==pattern.length()) return true; }
            else if (j>0) j=lps[j-1]; else i++;
        }
        return false;
    }
}
