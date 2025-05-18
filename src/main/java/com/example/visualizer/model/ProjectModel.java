package com.example.visualizer.model;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectModel {
    private final Map<String, PackageNode> packages = new LinkedHashMap<>();
    private final List<Dependency> classDeps = new ArrayList<>();
    private final Set<Set<String>> cycles = new HashSet<>();
    private final Map<String, Set<String>> pkgDeps = new LinkedHashMap<>();

    public void addClass(String pkgName, String className, boolean isInterface) {
        packages.computeIfAbsent(pkgName, PackageNode::new)
                .addClass(new ClassNode(className, isInterface, pkgName));
    }

    public void addClassFromBytecode(InputStream in) {
    }

    public void addDependency(String fromPkg, String from, String toPkg, String to, Dependency.Type type) {
        classDeps.add(new Dependency(fromPkg, from, toPkg, to, type));
    }

    public void computePackageDependencies() {
        classDeps.forEach(d -> {
            pkgDeps.computeIfAbsent(d.getFromPkg(), k -> new HashSet<>())
                    .add(d.getToPkg());
        });
    }

    public void computeCycles() {
        DefaultDirectedGraph<String, DefaultEdge> graph =
                new DefaultDirectedGraph<>(DefaultEdge.class);
        packages.keySet().forEach(graph::addVertex);

        pkgDeps.forEach((from, tos) -> {
            tos.stream()
                    .filter(to -> graph.containsVertex(to))
                    .forEach(to -> graph.addEdge(from, to));
        });

        KosarajuStrongConnectivityInspector<String, DefaultEdge> inspector =
                new KosarajuStrongConnectivityInspector<>(graph);
        inspector.stronglyConnectedSets().stream()
                .filter(scc -> scc.size() > 1)
                .forEach(cycles::add);
    }
    public Map<String, Integer> getOutgoingCount() {
        return pkgDeps.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().size()
                ));
    }

    public Map<String, Integer> getIncomingCount() {
        Map<String, Integer> incoming = new HashMap<>();
        packages.keySet().forEach(pkg -> incoming.put(pkg, 0));
        pkgDeps.forEach((from, tos) -> {
            for (String to : tos) {
                if (incoming.containsKey(to)) {
                    incoming.put(to, incoming.get(to) + 1);
                }
            }
        });
        return incoming;
    }

    public int getMaxDependencyDepth() {
        Map<String, Integer> memo = new HashMap<>();
        return packages.keySet().stream()
                .mapToInt(pkg -> depthFrom(pkg, memo))
                .max().orElse(0);
    }
    private int depthFrom(String pkg, Map<String, Integer> memo) {
        if (memo.containsKey(pkg)) return memo.get(pkg);
        int best = 0;
        for (String to : pkgDeps.getOrDefault(pkg, Collections.emptySet())) {
            best = Math.max(best, 1 + depthFrom(to, memo));
        }
        memo.put(pkg, best);
        return best;
    }

    public Set<Set<String>> getCycles() {
        return cycles;
    }

    public Set<String> getPackageNames() {
        return packages.keySet();
    }

    public Collection<PackageNode> getPackages() {
        return packages.values();
    }

    public List<Dependency> getClassDependencies() {
        return classDeps;
    }

    public Map<String, Set<String>> getPackageDependencies() {
        return pkgDeps;
    }

    public int getClassCount(String pkg) {
        return packages.getOrDefault(pkg, new PackageNode(pkg)).getClasses().size();
    }
}