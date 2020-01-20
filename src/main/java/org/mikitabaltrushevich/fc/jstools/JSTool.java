package org.mikitabaltrushevich.fc.jstools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.mikitabaltrushevich.fc.jstools.ui.JSToolMainFrame;

public class JSTool {

    private static String jarPath;

    private static final String JAR_FILE_PROVIDER = "jar:file:/";

    private static final String JS_ROBOT_PACKAGE_SOURCE = "/robot-wrapper/js/JSRobot";
    private static final String JS_WRAPPER = "/robot-wrapper/js/JSWrapper";

    private static final String JS_ROBOT_PACKAGE_TARGET = "/org/mikitabaltrushevich/fc/robocode";
    private static final String SECURITY_PACKAGE = "/net/sf/robocode/host/security";
    private static final String BACKUP_SECURITY_PACKAGE = "/META-INF/org/mikitabaltrushevich/fc/robocode/backup";

    private static final String JS_ROBOT_PATH = JS_ROBOT_PACKAGE_TARGET + "/JSRobot";

    private static final String ROBOCODE_HOST = "robocode.host-";
    private static final String ROBOCODE_JAR = "robocode.jar";
    private static final String LIBS = "libs/";
    private static final String ROBOTS = "robots";

    private static final String[] KEYS_NEEDED_ARR = {"robot.description", "robot.include.source", "robocode.version",
            "robot.version", "robot.author.name", "robot.classname", "robot.codesize"};

    private static File[] checkRobocodeDir(String path) {
        path += LIBS;

        final File libsDir = Paths.get(path).toFile();

        final String pathToRCJar = new String(path + ROBOCODE_JAR);

        final File rcJarFile = Paths.get(pathToRCJar).toFile();
        final File rcHostJarFile = getRobocodeSecurityJar(libsDir);

        if (!rcJarFile.exists() || !rcHostJarFile.exists()) {
            System.out.println("Incorrect directory specified");
            String os = System.getProperty("os.name");
            if (os.indexOf("Win") >= 0) {
                System.out.println("Please, specify FULL path like this: <Drive>:/<dir1>/<dir2>/robocode/");
            } else if (os.indexOf("Mac") >= 0 || os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
                System.out.println("Please, specify FULL path like this: /<dir1>/<dir2>/robocode/");
            }
            return null;
        }
        final File[] rcJars = {rcJarFile, rcHostJarFile};
        return rcJars;
    }

    private static File getRobocodeSecurityJar(File libsDir) {
        File hostFile = null;
        if (libsDir.listFiles() != null) {
            for (File file : libsDir.listFiles()) {
                if (file.getName().startsWith(ROBOCODE_HOST)) {
                    hostFile = file;
                    break;
                }
            }
        }
        return hostFile;
    }

    private static String getRobocodeVersion(String rcDirPath) {
        rcDirPath += LIBS;
        String robocodeVersion = null;

        final int rcHostNameLength = ROBOCODE_HOST.length();

        final File libsDir = Paths.get(rcDirPath).toFile();
        if (libsDir.listFiles() != null) {
            for (File file : libsDir.listFiles()) {
                if (file.getName().startsWith(ROBOCODE_HOST)) {
                    robocodeVersion = file.getName().substring(rcHostNameLength, rcHostNameLength + 7);
                }
            }
        }
        return robocodeVersion;
    }

    public static String changeSeparatorOrientation(String path) {
        final StringBuilder sb = new StringBuilder(path);
        for (int i = 0; i < sb.toString().length(); i++) {
            if (sb.charAt(i) == '\\') {
                sb.replace(i, i + 1, "/");
            }
        }
        return sb.toString();
    }

    private static String renameFileToBackup(String fileName) {
        final String fileBackupName = fileName.substring(0, fileName.length() - 5) + "bak";
        return fileBackupName;
    }

    private static void deleteDirectories(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (final DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectories(entry);
                }
            }
        }
        Files.delete(path);
    }

    private static List<String> getJarEntries(String neededPackage) {
        String entryName;
        final List<String> entryNameList = new ArrayList<>();

        try (final JarFile rcJar = new JarFile(jarPath.substring(5))) {
            Enumeration<JarEntry> entries = rcJar.entries();
            while (entries.hasMoreElements()) {
                entryName = entries.nextElement().getName();
                if (entryName.startsWith(neededPackage.substring(1))) {
                    entryNameList.add(entryName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryNameList;
    }

    private static boolean copyFilesIntoJar(FileSystem fsRcJar, URI uri, Map<String, String> env,
                                            List<String> entryNameList, String targetPackage) {
        try (final FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            for (String jstoolPath : entryNameList) {
                if (jstoolPath.substring(jstoolPath.lastIndexOf("/") + 1).isEmpty()) {
                    continue;
                }
                final Path target = fsRcJar.getPath(targetPackage,
                        jstoolPath.substring(jstoolPath.lastIndexOf("/") + 1));
                final Path source = fs.getPath(jstoolPath);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean patchRobocodeJar(URI uri, Map<String, String> env) {
        try (final FileSystem fsRcJar = FileSystems.newFileSystem(uri, env)) {

            final Path jsRobotPackagePath = fsRcJar.getPath(JS_ROBOT_PACKAGE_TARGET);
            if (!Files.exists(jsRobotPackagePath)) {
                Files.createDirectories(jsRobotPackagePath);
            }

            final List<String> entryNameList = getJarEntries(JS_ROBOT_PACKAGE_SOURCE);

            final URI jsToolUri = URI.create(JAR_FILE_PROVIDER + changeSeparatorOrientation(jarPath.substring(6)));

            if (!copyFilesIntoJar(fsRcJar, jsToolUri, env, entryNameList, JS_ROBOT_PACKAGE_TARGET)) {
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean backupSecurity(FileSystem fsRcJar, List<String> entryNameList) {
        for (String securityPath : entryNameList) {
            final String securityFile = securityPath.substring(securityPath.lastIndexOf("/") + 1);
            if (securityFile.isEmpty()) {
                continue;
            }
            final String backupSecurity = renameFileToBackup(securityFile);

            final Path backupFrom = fsRcJar.getPath(SECURITY_PACKAGE, securityFile);
            final Path backupTo = fsRcJar.getPath(BACKUP_SECURITY_PACKAGE, backupSecurity);

            try {
                if (Files.exists(backupFrom)) {
                    Files.move(backupFrom, backupTo);
                }
            } catch (FileAlreadyExistsException e) {
                System.out.println("backup files already exists");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private static boolean patchSecurity(URI uri, Map<String, String> env) {
        try (final FileSystem fs = FileSystems.newFileSystem(uri, env)) {

            final Path backupSecurityPath = fs.getPath(BACKUP_SECURITY_PACKAGE);
            if (!Files.exists(backupSecurityPath)) {
                Files.createDirectories(backupSecurityPath);
            }

            final List<String> entryNameList = getJarEntries(SECURITY_PACKAGE);

            if (!backupSecurity(fs, entryNameList)) {
                return false;
            }

            final URI jsToolUri = URI.create(JAR_FILE_PROVIDER + changeSeparatorOrientation(jarPath.substring(6)));

            if (!copyFilesIntoJar(fs, jsToolUri, env, entryNameList, SECURITY_PACKAGE)) {
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean install(String path) {
        final File[] robocodeDirs = checkRobocodeDir(path);
        if (robocodeDirs == null) {
            return false;
        }
        final File rcJarFile = robocodeDirs[0];
        final File rcHostJarFile = robocodeDirs[1];

        final Map<String, String> env = new HashMap<>();
        env.put("create", "false");

        URI uri = URI.create(JAR_FILE_PROVIDER + changeSeparatorOrientation(rcJarFile.getPath()));

        if (!patchRobocodeJar(uri, env)) {
            return false;
        }

        uri = URI.create(JAR_FILE_PROVIDER + changeSeparatorOrientation(rcHostJarFile.getPath()));

        if (!patchSecurity(uri, env)) {
            return false;
        }

        System.out.println("\nPatch was successfully installed!");

        return true;
    }

    private static boolean deleteRobocodeJarPatch(URI uri, Map<String, String> env) {
        try (final FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            deleteDirectories(fs.getPath("/org"));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean deleteSecurityPatch(URI uri, Map<String, String> env) {
        try (final FileSystem fs = FileSystems.newFileSystem(uri, env)) {

            final List<String> entryNameList = getJarEntries(SECURITY_PACKAGE);

            for (String securityPath : entryNameList) {
                final String securityFile = securityPath.substring(securityPath.lastIndexOf("/") + 1);
                if (securityFile.isEmpty()) {
                    continue;
                }

                final Path securityFileToDelete = fs.getPath(SECURITY_PACKAGE, securityFile);

                try (final DirectoryStream<Path> entries = Files.newDirectoryStream(fs.getPath(BACKUP_SECURITY_PACKAGE))) {
                    for (Path entry : entries) {
                        Files.move(entry, securityFileToDelete, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            deleteDirectories(fs.getPath("META-INF/org"));

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean uninstall(String path) {
        final File[] robocodeDirs = checkRobocodeDir(path);
        if (robocodeDirs == null) {
            return false;
        }
        final File rcJarFile = robocodeDirs[0];
        final File rcHostJarFile = robocodeDirs[1];

        final Map<String, String> env = new HashMap<>();
        env.put("create", "false");

        URI uri = URI.create(JAR_FILE_PROVIDER + changeSeparatorOrientation(rcJarFile.getPath()));

        if (!deleteRobocodeJarPatch(uri, env)) {
            return false;
        }

        uri = URI.create(JAR_FILE_PROVIDER + changeSeparatorOrientation(rcHostJarFile.getPath()));

        if (!deleteSecurityPatch(uri, env)) {
            return false;
        }

        System.out.println("\nPatch was successfully uninstalled!");

        return true;
    }

    private static boolean checkJar(JarFile jar, String target) {
        boolean isPatched = false;
        String entryName;
        final Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            entryName = entries.nextElement().getName();
            if (entryName.startsWith(target.substring(1))) {
                isPatched = true;
                break;
            }
        }

        if (isPatched) {
            System.out.println(jar.getName().substring(jar.getName().lastIndexOf(File.separator) + 1) + " is patched");
        } else {
            System.out.println(
                    jar.getName().substring(jar.getName().lastIndexOf(File.separator) + 1) + " is NOT patched");
        }
        return isPatched;

    }

    public static boolean check(String path) {
        final File[] robocodeDirs = checkRobocodeDir(path);
        if (robocodeDirs == null) {
            return false;
        }
        final File rcJarFile = robocodeDirs[0];
        final File rcHostJarFile = robocodeDirs[1];

        boolean isPatchedRCJar = false;
        boolean isPatchedRCHostJar = false;

        JarFile rcJar = null;
        JarFile rcHostJar = null;

        try {
            rcJar = new JarFile(rcJarFile);
            isPatchedRCJar = checkJar(rcJar, JS_ROBOT_PATH);

            rcHostJar = new JarFile(rcHostJarFile);
            isPatchedRCHostJar = checkJar(rcHostJar, BACKUP_SECURITY_PACKAGE);

            if (isPatchedRCJar && isPatchedRCHostJar) {
                System.out.println("\nYour robocode is already patched!");
            } else {
                System.out.println("\nNo patch is found!");
            }
            rcJar.close();
            rcHostJar.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isPatchedRCJar && isPatchedRCHostJar) {
            return true;
        } else {
            return false;
        }

    }

    private static HashMap<String, String> parseRobotProps(File propsFile) {
        final HashMap<String, String> robotProps = new HashMap<>();
        try (final BufferedReader br = new BufferedReader(new FileReader(propsFile));) {

            String str = null;
            while ((str = br.readLine()) != null) {
                if (str.startsWith("robo")) {
                    String[] kvProps = str.split("=");
                    robotProps.put(kvProps[0], kvProps[1]);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return robotProps;
    }

    public static File createPropertyFile(String rcPath, String robotPath, String authorName, String robotName,
                                          String[] description, String version) throws ParseException {
        System.out.println("CREATED PROPERTY");
        File propsFile = null;

        if (checkRobocodeDir(rcPath) == null) {
            return null;
        }

        final Path pathToRobot = Paths.get(robotPath);

        if (!Files.exists(pathToRobot)) {
            throw new ParseException("JSRobot file does not exist");
        }
        propsFile = Paths.get(pathToRobot.getParent().toString() + "/" + robotName + ".properties").toFile();
        final StringBuilder descriptionBuilder = new StringBuilder();
        int numOfChars = 0;
        for (String str : description) {
            numOfChars += (str.length() + 1);
            if (numOfChars >= 75) {
                descriptionBuilder.append("\\n");
                numOfChars = 0;
            }
            descriptionBuilder.append(str).append(" ");

        }

        try (final PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(propsFile)))) {
            if (!propsFile.exists()) {
                propsFile.createNewFile();
            }
            pw.println("#Robocode Robot");
            pw.println("#" + new Date().toString());
            pw.println("robot.description=" + descriptionBuilder.toString());
            pw.println("robot.include.source=true");
            pw.println("robocode.version=" + getRobocodeVersion(rcPath));
            pw.println("robot.version=" + version);
            pw.println("robot.author.name=" + authorName);
            pw.println("robot.classname=" + robotName.toLowerCase() + "." + robotName);
            pw.println("robot.codesize=0");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return propsFile;
    }

    public static boolean checkPropertyFile(File propsFile) {
        System.out.println("CHECK PROPS");

        if (!propsFile.exists()) {
            System.out.println("Property file does not exist");
            return false;
        }

        final List<String> keysNeeded = Arrays.asList(KEYS_NEEDED_ARR);

        final HashMap<String, String> robotProps = parseRobotProps(propsFile);

        boolean checkFlag = true;

        int missingCounter = 0;

        final StringBuilder sb = new StringBuilder();
        for (String key : robotProps.keySet()) {
            if (robotProps.size() != keysNeeded.size()) {
                for (String requiredKey : keysNeeded) {
                    missingCounter++;
                    if (key.equals(requiredKey)) {
                        break;
                    }
                }
            }

            if (missingCounter == keysNeeded.size()) {
                checkFlag = false;
                sb.append("Key ").append(key).append(" is missing. Please, add it to the properties file");
                System.out.println(sb.toString());
                sb.delete(0, sb.length());
                continue;
            }

            if (robotProps.get(key).equals("null")) {
                sb.append("Value of ").append(key).append(" is either missing or invalid");
                System.out.println(sb.toString());
                sb.delete(0, sb.length());
                checkFlag = false;
            }
        }
        return checkFlag;
    }

    private static String getJsWrapperContent(URI uri, Map<String, String> env, List<String> entryNameList) {
        Path jsWrapperPath = null;
        try (final FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            for (String jstoolPath : entryNameList) {
                if (jstoolPath.substring(jstoolPath.lastIndexOf("/") + 1).isEmpty()) {
                    continue;
                }
                if (jstoolPath.startsWith(JS_WRAPPER.substring(1))) {
                    jsWrapperPath = fs.getPath(jstoolPath);
                    break;
                }
            }

            if (jsWrapperPath == null) {
                System.out.println("Error while creating JS Robot");
                return null;
            }

            return new String(Files.readAllBytes(jsWrapperPath));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static StringBuilder jsCodeAsString(File jsRobot) throws IOException {
        BufferedReader br = null;
        try {
            final StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new FileReader(jsRobot));
            String str = null;

            while ((str = br.readLine()) != null) {
                sb.append("\"").append(str).append("\\n\" +");
            }
            return sb;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            br.close();
        }
        return null;
    }

    private static Path fillInJsWrapper(String rcPath, URI uri, Map<String, String> env, List<String> entryNameList,
                                        Path pathToRobot, String[] robotClassName) throws IOException {
        String wrapperContent = getJsWrapperContent(uri, env, entryNameList);

        final StringBuilder jsCode = jsCodeAsString(pathToRobot.toFile());
        if (jsCode == null) {
            System.out.println("Error while parsing javascript code");
            return null;
        }
        jsCode.replace(jsCode.lastIndexOf("+"), jsCode.lastIndexOf("+") + 1, "\n");
        final String jsCodeString = jsCode.toString().replaceAll("\\+", "+\n");

        final StringBuilder sb = new StringBuilder();
        sb.append(robotClassName[0]).append(";\n\n")
                .append("import org.mikitabaltrushevich.fc.robocode.JSRobot_dcc76658_0dd2_4679_b91a_e0c9cf7c9053;");
        wrapperContent = wrapperContent.replace("${namespace};", sb.toString()).replace("${name}", robotClassName[1])
                .replace("\"${source}\"", jsCodeString);

        final File javaRobotFile = new File(pathToRobot.getParent() + robotClassName[1] + ".java");

        if (!javaRobotFile.exists()) {
            javaRobotFile.createNewFile();
        }

        final Path robotPackage = Paths.get(rcPath, ROBOTS, robotClassName[0]);
        if (!Files.exists(robotPackage)) {
            Files.createDirectory(robotPackage);
        }

        Files.write(Paths.get(javaRobotFile.getPath()), wrapperContent.getBytes(),
                StandardOpenOption.TRUNCATE_EXISTING);
        return Files.move(Paths.get(javaRobotFile.getPath()),
                Paths.get(rcPath, ROBOTS, robotClassName[0], javaRobotFile.getName()),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private static void compileJsBot(File file, String rcPath) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        try (final StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            final Iterable<? extends JavaFileObject> fileObjects = fm.getJavaFileObjects(file);

            final List<String> options = new ArrayList<>();
            options.add("-source");
            options.add("1.8");
            options.add("-target");
            options.add("1.8");
            options.add("-classpath");
            options.add(Paths.get(rcPath, LIBS, ROBOCODE_JAR).toString());
            JavaCompiler.CompilationTask task = compiler.getTask(null, fm, null, options, null, fileObjects);
            if (!task.call()) {
                System.out.println("Compilation error");
            }
            task.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Manifest createManifest(String[] robotClassName) {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        final StringBuilder sb = new StringBuilder();
        sb.append(robotClassName[0]).append(".").append(robotClassName[1]);

        manifest.getMainAttributes().put(new Attributes.Name("robots"), sb.toString());
        return manifest;
    }

    private static File createJarFile(String rcPath, Manifest manifest, String[] robotClassName, File[] robotItems,
                                      String robotVersion) {
        final StringBuilder jarName = new StringBuilder();
        jarName.append(robotClassName[0]).append(".").append(robotClassName[1]).append("_").append(robotVersion)
                .append(".jar");
        final File robotJar = new File(Paths.get(rcPath, ROBOTS, jarName.toString()).toString());
        JarOutputStream jarOut = null;

        FileInputStream fis = null;
        BufferedInputStream bis = null;

        try (final FileOutputStream fos = new FileOutputStream(changeSeparatorOrientation(robotJar.toString()))) {
            jarOut = new JarOutputStream(fos, manifest);
            for (File robotItem : robotItems) {
                if (robotItem.getPath().endsWith(".java")) {
                    continue;
                }
                final JarEntry file = new JarEntry(
                        changeSeparatorOrientation(Paths.get(robotClassName[0], robotItem.getName()).toString()));
                file.setTime(robotItem.lastModified());
                jarOut.putNextEntry(file);

                fis = new FileInputStream(robotItem);
                bis = new BufferedInputStream(fis);

                byte[] buffer = new byte[1024];
                while (true) {
                    int count = bis.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    jarOut.write(buffer, 0, count);
                }
                jarOut.closeEntry();
                bis.close();
                fis.close();
            }
            jarOut.setComment(getRobocodeVersion(rcPath) + " - Robocode version");
            jarOut.flush();
            jarOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return robotJar;
    }

    public static void create(String rcPath, String jsRobotPath, File propsFile) throws ParseException {
        if (checkRobocodeDir(rcPath) == null) {
            return;
        }

        final Path pathToRobot = Paths.get(jsRobotPath);

        if (!Files.exists(pathToRobot)) {
            throw new ParseException("JSRobot file does not exist");
        }

        try {
            final HashMap<String, String> robotProps = parseRobotProps(propsFile);

            final List<String> entryNameList = getJarEntries(JS_WRAPPER);

            final URI uri = URI.create(JAR_FILE_PROVIDER + changeSeparatorOrientation(jarPath.substring(6)));
            final Map<String, String> env = new HashMap<>();
            env.put("create", "false");

            final String[] robotClassName = robotProps.get("robot.classname").split("\\.");

            final File javaRobotFile = fillInJsWrapper(rcPath, uri, env, entryNameList, pathToRobot, robotClassName).toFile();

            if (javaRobotFile == null) {
                System.out.println("Unable to create Java Robot File");
                return;
            }

            compileJsBot(javaRobotFile, rcPath);

            Files.move(propsFile.toPath(), Paths.get(rcPath, ROBOTS, robotClassName[0], propsFile.getName()),
                    StandardCopyOption.REPLACE_EXISTING);

            final File robotJar = createJarFile(rcPath, createManifest(robotClassName), robotClassName,
                    javaRobotFile.getParentFile().listFiles(), robotProps.get("robot.version"));

            System.out.println("\nRobot created successfully! Path to JAR: " + robotJar.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Options getOptions() {
        // create the Options
        final Options options = new Options();

        options.addOption(Option.builder("i")
                .longOpt("install")
                .hasArg()
                .argName("path to Robocode")
                .desc("Install JSEngine to Robocode")
                .type(String.class)
                .build())
                .addOption(Option.builder("u")
                        .longOpt("uninstall")
                        .hasArg()
                        .desc("Uninstall JSEngine from Robocode")
                        .type(String.class)
                        .build())
                .addOption(Option.builder("c")
                        .longOpt("create")
                        .numberOfArgs(2)
                        .argName("path to Robocode> <path to YourJSRobot.js")
                        .desc("Create your JSRobot and pull it into Robocode")
                        .type(String.class)
                        .build())
                .addOption(Option.builder("ch")
                        .longOpt("check")
                        .hasArg()
                        .argName("path to Robocode directory")
                        .desc("Check whether your Robocode is patched for JS")
                        .type(String.class)
                        .build())
                .addOption(Option.builder("p")
                        .longOpt("props-file")
                        .hasArg()
                        .argName("path to YourJSRobot.properties")
                        .desc("Use this option if you have .properties file for your robot. \n"
                                + "Note that if you specify options below, they will have higher priority "
                                + "and will overwrite this options in .properties file")
                        .type(String.class).build())
                .addOption(Option.builder("a")
                        .longOpt("author")
                        .hasArg()
                        .argName("name")
                        .desc("JSRobot author name")
                        .type(String.class)
                        .build())
                .addOption(Option.builder("r")
                        .longOpt("robot-name")
                        .hasArg()
                        .argName("robot name")
                        .desc("Name of your robot "
                                + "(please, specify with a capital letter and do not use more than 1 word)")
                        .type(String.class)
                        .build())
                .addOption(Option.builder("d")
                        .longOpt("description")
                        .hasArgs()
                        .argName("description")
                        .desc("Your robot description")
                        .type(String.class)
                        .build())
                .addOption(Option.builder("v")
                        .longOpt("version")
                        .hasArg()
                        .argName("robot version (Like x.y.z)")
                        .desc("Your robot current version")
                        .type(String.class)
                        .build());

        options.addOption("h", "help", false, "print this message");

        return options;
    }

    private static void printHelp(Options options, String jarName) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.setWidth(128);
        formatter.printHelp("java -jar " + (jarName == null ? "jstool.jar" : jarName) + " [options]", "\nOptions:",
                options, "");
    }

    public static void main(String[] args) {
        //System.setProperty("java.home", System.getenv("JAVA_HOME"));

        final Options options = getOptions();
        String jarName = null;
        try {
            JSTool.jarPath = JSTool.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
            jarName = JSTool.jarPath.substring(JSTool.jarPath.lastIndexOf("/") + 1);
        } catch (URISyntaxException e) {
            jarName = null;
        }

        //System.out.println(System.getProperty("java.library.path"));

        try {
            // parse the command line arguments
            final CommandLine cli = new DefaultParser().parse(options, args);

            if (cli.getArgs().length != 1 && cli.getOptions().length < 1) {
                JSToolMainFrame window = new JSToolMainFrame();
                window.pack();
                window.setVisible(true);
            } else if (cli.hasOption("help")) {
                printHelp(options, jarName);
            } else if (cli.hasOption("install")) {
                if (!check(cli.getOptionValue("install"))) {
                    System.out.println("\nInstalling the patch...");
                    install(cli.getOptionValue("install"));
                }
            } else if (cli.hasOption("uninstall")) {
                if (check(cli.getOptionValue("uninstall"))) {
                    System.out.println("\nUninstalling the patch...");
                    uninstall(cli.getOptionValue("uninstall"));
                }
            } else if (cli.hasOption("check")) {
                check(cli.getOptionValue("check"));
            } else if (cli.hasOption("create")) {
                File propsFile = null;

                String[] optionArgs = cli.getOptionValues("create");
                if (optionArgs.length != 2) {
                    System.out.println("Please, specify both path to Robocode and path to your JSRobot");
                    return;
                }

                if (!cli.hasOption("props-file")) {
                    if (!cli.hasOption("author") || !cli.hasOption("robot-name") || !cli.hasOption("description")
                            || !cli.hasOption("version")) {
                        System.out.println("Please, specify all of these: --author, --robot-name, --description, "
                                + "--version or specify .properties file (--props-file)");
                        return;
                    }
                    propsFile = createPropertyFile(optionArgs[0], optionArgs[1], cli.getOptionValue("author"),
                            cli.getOptionValue("robot-name"), cli.getOptionValues("description"),
                            cli.getOptionValue("version"));

                    if (propsFile == null) {
                        System.out.println("Unable to create property file");
                        return;
                    }
                    if (!checkPropertyFile(propsFile)) {
                        System.out.println("Not all options exist or valid. Either fill them manually to the file "
                                + "or use one of options available for this");
                        return;
                    }
                    create(optionArgs[0], optionArgs[1], propsFile);
                    return;
                }
                propsFile = Paths.get(cli.getOptionValue("props-file")).toFile();
                if (!checkPropertyFile(propsFile)) {
                    System.out.println("Not all options exist or valid. Either fill them manually to the file "
                            + "or use one of options available for this");
                    return;
                }
                create(optionArgs[0], optionArgs[1], propsFile);
            } else {
                throw new ParseException("Unknown command");
            }
        } catch (ParseException e) {
            // oops, something went wrong
            System.err.println("Parsing failed. Reason: " + e.getMessage());
        }
    }
}
