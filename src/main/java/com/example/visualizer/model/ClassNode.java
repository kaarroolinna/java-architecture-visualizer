package com.example.visualizer.model;

import java.util.*;

public class ClassNode {
    private final String name;
    private final boolean isInterface;
    private final String packageName;
    private final List<String> methods = new ArrayList<>();
    private final List<String> fields  = new ArrayList<>();

    public ClassNode(String name, boolean isInterface, String packageName) {
        this.name = name;
        this.isInterface = isInterface;
        this.packageName = packageName;
    }
    public void addMethod(String signature) {
        methods.add(signature);
    }
    public void addField(String fieldName) {
        fields.add(fieldName);
    }
    public List<String> getMethods() {
        return Collections.unmodifiableList(methods);
    }
    public List<String> getFields() {
        return Collections.unmodifiableList(fields);
    }
    public String getName() {
        return name;
    }
    public boolean isInterface() {
        return isInterface;
    }
    public String getPackageName() {
        return packageName;
    }
}
