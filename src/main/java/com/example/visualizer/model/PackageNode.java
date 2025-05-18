package com.example.visualizer.model;

import java.util.*;

public class PackageNode {
    private final String name;
    private final List<ClassNode> classes = new ArrayList<>();
    public PackageNode(String name) {
        this.name = name;
    }
    public void addClass(ClassNode cls) {
        classes.add(cls);
    }
    public String getName() {
        return name;
    }
    public List<ClassNode> getClasses() {
        return classes;
    }
}