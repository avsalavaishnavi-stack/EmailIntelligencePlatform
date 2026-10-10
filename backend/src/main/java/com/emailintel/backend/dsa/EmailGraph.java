package com.emailintel.backend.dsa;

import java.util.*;

public class EmailGraph {
    private final Map<String, Set<String>> adj = new HashMap<>();
    public void addEdge(String a,String b){adj.computeIfAbsent(a,k->new HashSet<>()).add(b);adj.computeIfAbsent(b,k->new HashSet<>()).add(a);}
    public List<String> bfs(String start){return traverse(start,false);}
    public List<String> dfs(String start){return traverse(start,true);}
    private List<String> traverse(String start,boolean dfs){List<String> out=new ArrayList<>(); if(!adj.containsKey(start))return out; Set<String> seen=new HashSet<>(); Deque<String> q=new ArrayDeque<>(); q.push(start); while(!q.isEmpty()){String x=dfs?q.pop():q.removeFirst(); if(!seen.add(x))continue; out.add(x); for(String y:adj.getOrDefault(x,Set.of())) if(!seen.contains(y)){if(dfs)q.push(y);else q.addLast(y);}} return out;}
    public int vertices(){return adj.size();}
    public int edges(){return adj.values().stream().mapToInt(Set::size).sum()/2;}
}
