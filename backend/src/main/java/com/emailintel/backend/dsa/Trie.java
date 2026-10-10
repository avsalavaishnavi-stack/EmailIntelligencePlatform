package com.emailintel.backend.dsa;

import java.util.HashMap;
import java.util.Map;

public class Trie {
    private static class Node { Map<Character, Node> next = new HashMap<>(); boolean end; }
    private final Node root = new Node();
    public void insert(String word) {
        Node cur = root;
        for (char c : word.toLowerCase().toCharArray()) cur = cur.next.computeIfAbsent(c, k -> new Node());
        cur.end = true;
    }
    public boolean startsWith(String prefix) {
        Node cur = root;
        for (char c : prefix.toLowerCase().toCharArray()) { cur = cur.next.get(c); if (cur == null) return false; }
        return true;
    }
}
