package com.example.visualizer.analyzer;

import com.example.visualizer.model.Dependency;
import com.example.visualizer.model.ProjectModel;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.IOException;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ProjectAnalyzer {
    public ProjectModel analyze(Path root) {
        ProjectModel model = new ProjectModel();
        JavaParser parser = new JavaParser();
        try {
            if (Files.isDirectory(root)) {
                Files.walk(root)
                        .filter(p -> p.toString().endsWith(".java"))
                        .forEach(p -> parseSource(p, parser, model));
            } else if (root.toString().endsWith(".jar")) {
                try (JarFile jar = new JarFile(root.toFile())) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            model.addClassFromBytecode(jar.getInputStream(entry));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        model.computePackageDependencies();
        return model;
    }

    private void parseSource(Path path, JavaParser parser, ProjectModel model) {
        try {
            CompilationUnit cu = parser.parse(path).getResult().orElse(null);
            if (cu == null) return;
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(decl -> {
                String pkg = cu.getPackageDeclaration()
                        .map(pd -> pd.getName().toString()).orElse("");
                model.addClass(pkg, decl.getNameAsString(), decl.isInterface());
                decl.getExtendedTypes().forEach(ext ->
                        model.addDependency(pkg, decl.getNameAsString(), pkgOf(ext.getNameAsString()), ext.getNameAsString(), Dependency.Type.INHERITANCE)
                );
                decl.getImplementedTypes().forEach(impl ->
                        model.addDependency(pkg, decl.getNameAsString(), pkgOf(impl.getNameAsString()), impl.getNameAsString(), Dependency.Type.INHERITANCE)
                );
                decl.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class)
                        .forEach(call ->
                                call.getScope().ifPresent(scope ->
                                        model.addDependency(pkg, decl.getNameAsString(), pkgOf(scope.toString()), scope.toString(), Dependency.Type.METHOD_CALL)
                                )
                        );
            });
        } catch (IOException ignored) {}
    }

    private String pkgOf(String className) {
        return className.contains(".") ? className.substring(0, className.lastIndexOf('.')) : "";
    }
}
