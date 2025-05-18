package com.example.visualizer.model;

public class Dependency {
    public enum Type {
        INHERITANCE, METHOD_CALL
    }
    private final String fromPkg;
    private final String from;
    private final String toPkg;
    private final String to;
    private final Type type;
    public Dependency(String fromPkg, String from, String toPkg, String to, Type type) {
        this.fromPkg = fromPkg;
        this.from = from;
        this.toPkg = toPkg;
        this.to = to;
        this.type = type;
    }
    public String getFromPkg() {
        return fromPkg;
    }
    public String getToPkg() {
        return toPkg;
    }
    public String getFrom() {
        return from;
    }
    public String getTo() {
        return to;
    }
    public Type getType() {
        return type;
    }
}