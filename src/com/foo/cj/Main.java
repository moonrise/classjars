package com.foo.cj;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.print("Provide an input file that contains the output of JVM -verbose:class option\n");
            System.exit(1);
        }

        try {
            new Main().processFile(args[0]);
        }
        catch (IOException exception) {
            System.out.println(exception);
        }
    }

    private void processFile(String filePath) throws IOException {
        final String OPENED = "[Opened ";
        final String LOADED = "[Loaded ";
        final String UNLOADING = "[Unloading class ";
        final String FROM = " from ";

        final int OPENED_LEN = OPENED.length();
        final int LOADED_LEN = LOADED.length();
        final int UNLOADING_LEN = UNLOADING.length();
        final int FROM_LEN = FROM.length();

        TreeMap<String, TreeSet<String>> jarFiles = new TreeMap<String, TreeSet<String>>();
        TreeSet<String> jarPaths = new TreeSet<String>();
        TreeMap<String, String> classes = new TreeMap<String, String>();

        int duplicateJarFiles = 0;

        // Open the file
        FileInputStream inputStream = new FileInputStream(filePath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        // stat
        int totalLines = 0;
        int skippedLines = 0;

        //Read File Line By Line
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            ++totalLines;
            if (line.startsWith("[") == false || line.endsWith("]") == false) {
                ++skippedLines;
                continue;
            }

            if (line.contains("zeroturnaround") || line.contains("jrebel")) {
                ++skippedLines;
                System.out.printf("skipping JRebel libs: %s\n", line);
                continue;
            }

            if (line.startsWith(LOADED)) {
                String loadedClass = line.substring(LOADED_LEN, line.length() - 1);

                int fromMarker = loadedClass.indexOf(FROM);
                if (fromMarker < 0) {
                    ++skippedLines;
                    System.out.printf("unexpected token encountered: %s\n", line);
                    continue;
                }

                if (loadedClass.endsWith("/")) {
                    ++skippedLines;
                    System.out.printf("unexpected ending slash encountered: %s\n", line);
                    continue;
                }

                int lastDelimiter = loadedClass.lastIndexOf("/");
                if (lastDelimiter < 0) {
                    ++skippedLines;
                    System.out.printf("unexpected token encountered: %s\n", line);
                    continue;
                }

                String classPath = loadedClass.substring(0, fromMarker);
                String jarPath = loadedClass.substring(loadedClass.indexOf("/"));
                String jarPathOnly = jarPath.substring(0, jarPath.lastIndexOf("/"));
                String jarFile = loadedClass.substring(lastDelimiter+1);

                classes.put(classPath, String.format("%s : %s", jarFile, jarPath));
                jarPaths.add(jarPath);

                // jar file entry with no path info (for a quick lookup not cluttered with lengthy paths)
                TreeSet<String> paths = jarFiles.get(jarFile);
                if (paths == null) {
                    paths = new TreeSet<String>();
                    jarFiles.put(jarFile, paths);
                }
                else if (paths.contains(jarPathOnly) == false) {
                    duplicateJarFiles++;
                }
                paths.add(jarPathOnly);
            }
            else if (line.startsWith(OPENED)) {
                /*
                String openedJar = line.substring(OPENED_LEN, line.length() - 1);

                int lastDelimiter = openedJar.lastIndexOf("/");
                String jarPath = openedJar.substring(0, lastDelimiter);
                String jarFile = openedJar.substring(lastDelimiter+1);

                jarPaths.add(openedJar);
                jarFiles.put(jarFile, jarPath);
                System.out.printf("opened %s\n", openedJar);
                */
            }
            else if (line.startsWith(UNLOADING)) {
                /*
                String unloading = line.substring(UNLOADING_LEN, line.length() - 1);
                System.out.printf("unloading %s\n", unloading);
                */
            }
            else {
                ++skippedLines;
                System.out.printf("unexpected token encountered: %s\n", line);
            }
        }

        //Close the input stream
        bufferedReader.close();

        System.out.printf("\n===== CLASS JARS REPORT\n");
        System.out.printf("total lines: %d, skipped: %d, processed: %d\n", totalLines, skippedLines, totalLines - skippedLines);

        System.out.printf("\n===== CLASSES (%d)\n", classes.size());
        for (Map.Entry<String, String> classEntry : classes.entrySet()) {
            System.out.printf("%s -> %s\n", classEntry.getKey(), classEntry.getValue());
        }


        System.out.printf("\n===== JARS (%d : %d duplicates)\n", jarFiles.size(), duplicateJarFiles);
        for (Map.Entry<String, TreeSet<String>> jarFile : jarFiles.entrySet()) {
            String[] paths = jarFile.getValue().toArray(new String[0]);
            System.out.printf("%s -> %s\n", jarFile.getKey(), paths[0]);
            for (int i=1; i<paths.length; i++) {
                System.out.printf("%s -> %s\n", jarFile.getKey().replaceAll(".", " "), paths[i]);
            }
        }

        System.out.printf("\n===== JAR PATHS (%d)\n", jarPaths.size());
        for (String jarPath : jarPaths) {
            System.out.println(jarPath);
        }

        System.out.printf("\n===== JAR PATHS FOR IDEA PROJECTS\n");
        for (String jarPath : jarPaths) {
            int midPathIndex = jarPath.indexOf("/build/");
            if (midPathIndex < 0) {
                continue;
            }

            String midPath = jarPath.substring(midPathIndex);
            //      <root url="jar://$PROJECT_DIR$/build/apps/admin/lib/csadmin.jar!/" />
            System.out.printf("      <root url=\"jar://$PROJECT_DIR$%s!/\" />\n", midPath);
        }
    }
}
