package io.geekya215.nyarpc.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public final class ClassUtil {
    private ClassUtil() {
    }

    public static @NotNull List<@NotNull Class<?>> scanClassesWithAnnotation(
            @NotNull String packageName,
            @NotNull Class<? extends Annotation> annotationClass)
            throws IOException, ClassNotFoundException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final String path = packageName.replace('.', '/');
        final Enumeration<URL> resources = classLoader.getResources(path);

        final List<@NotNull Class<?>> classes = new ArrayList<>();

        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();

            if (resource.getProtocol().equals("file")) {
                final File directory = new File(resource.getFile());

                if (directory.exists()) {
                    final File[] files = directory.listFiles();

                    if (files != null) {
                        for (final File file : files) {
                            if (file.isDirectory()) {
                                classes.addAll(scanClassesWithAnnotation(packageName + "." + file.getName(), annotationClass));
                            } else if (file.getName().endsWith(".class")) {
                                final String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                                final Class<?> clazz = Class.forName(className);
                                if (clazz.isAnnotationPresent(annotationClass)) {
                                    classes.add(clazz);
                                }
                            }
                        }
                    }
                }
            }
        }
        return classes;
    }
}
