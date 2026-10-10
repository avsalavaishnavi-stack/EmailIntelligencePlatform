package com.emailintel.backend.dsa;

public class RabinKarp {
    private static final long MOD = 1_000_000_007L;
    private static final long BASE = 257L;
    public static long hash(String s) {
        long h=0; for(char c:s.toLowerCase().toCharArray()) h=(h*BASE+c)%MOD; return h;
    }
    public static boolean contains(String text, String pattern) {
        if (pattern.isEmpty()) return true; if (pattern.length()>text.length()) return false;
        text=text.toLowerCase(); pattern=pattern.toLowerCase();
        long ph=hash(pattern), th=hash(text.substring(0,pattern.length())), pow=1;
        for(int i=1;i<pattern.length();i++) pow=pow*BASE%MOD;
        for(int i=0;i<=text.length()-pattern.length();i++) {
            if(th==ph && text.regionMatches(i,pattern,0,pattern.length())) return true;
            if(i<text.length()-pattern.length()) {
                th=(th-(long)text.charAt(i)*pow%MOD+MOD)%MOD;
                th=(th*BASE+text.charAt(i+pattern.length()))%MOD;
            }
        }
        return false;
    }
}
