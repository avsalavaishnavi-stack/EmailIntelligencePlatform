package com.emailintel.backend.dsa;

import java.util.*;

public class SuffixArray {
    public static int[] build(String s) {
        int n=s.length(); Integer[] a=new Integer[n]; for(int i=0;i<n;i++) a[i]=i;
        Arrays.sort(a, (i,j)->s.substring(i).compareTo(s.substring(j)));
        return Arrays.stream(a).mapToInt(Integer::intValue).toArray();
    }
    public static boolean contains(String text,String pattern) {
        if(pattern.isEmpty()) return true; int[] sa=build(text.toLowerCase()); String p=pattern.toLowerCase();
        int lo=0,hi=sa.length-1;
        while(lo<=hi){int mid=(lo+hi)>>>1; String suffix=text.toLowerCase().substring(sa[mid]); int c=suffix.compareTo(p); if(suffix.startsWith(p)) return true; if(c<0)lo=mid+1;else hi=mid-1;}
        return false;
    }
}
