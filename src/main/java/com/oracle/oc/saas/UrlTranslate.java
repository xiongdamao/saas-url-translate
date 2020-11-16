package com.oracle.oc.saas;

import org.apache.commons.cli.*;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author ke.xiong@oracle.com
 * @version 1.0
 * @date 2020-11-13 10:41
 */
public class UrlTranslate {
    public static void main(String[] args) throws ParseException {
        CommandLine cmd = checkOptions(args);
        if (cmd != null) {
            try {

                List<KeyValue> mapping = readMapping(cmd.getOptionValue("c"));
                String outFolder = cmd.getOptionValue("o");
                String inFolder = cmd.getOptionValue("i");
                File outLogFile = new File(outFolder + "/report.html");
                if (!outLogFile.getParentFile().exists()) {
                    outLogFile.getParentFile().mkdirs();
                }
                long startTime = System.currentTimeMillis();
                try (FileOutputStream logFile = new FileOutputStream(outLogFile)) {
                    logFile.write("<style>table{width:100%} td{border: 1px double;}</style><table>".getBytes());
                    List<File> files = new ArrayList<>();
                    listf(inFolder, files);
                    int i = 0;
                    int total = files.size();
                    for (File file : files) {
                        File outFile = new File(file.getAbsolutePath().replaceAll(inFolder, outFolder));
                        if (file.getName().endsWith(".jar")) {
                            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile));
                            try (ZipFile zipFile = new ZipFile(file)) {
                                final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                                while (entries.hasMoreElements()) {
                                    ZipEntry entry = entries.nextElement();
                                    String fileName = entry.getName();
                                    ZipEntry ze = new ZipEntry(fileName);
                                    zos.putNextEntry(ze);

                                    findKey(zipFile.getInputStream(entry), file.getAbsolutePath() + "->" + fileName, mapping, zos, logFile);


                                }

                                if (zos != null) {
                                    zos.close();
                                }

                            }
                        } else if (file.getName().endsWith(".jpg")
                                || file.getName().endsWith(".sso")
                        ) {

                        } else {
                            try {

                                if (!outFile.getParentFile().exists()) {
                                    outFile.getParentFile().mkdirs();
                                }
                                FileInputStream fileInputStream = new FileInputStream(file);
                                FileOutputStream fileOutputStream = new FileOutputStream(outFile);
                                findKey(fileInputStream, file.getAbsolutePath(), mapping, fileOutputStream, logFile);
                                fileInputStream.close();
                                if (fileOutputStream != null) {
                                    fileOutputStream.close();
                                }
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                        printProgress(startTime, total, ++i);
                    }
                    ;
                    logFile.write("</table>".getBytes());
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("ok");
        }

    }

    public static void findKey(InputStream file, String fileName, List<KeyValue> mapping, OutputStream outputStream, FileOutputStream logFile) {
        BufferedReader reader;
        try {
            logFile.write(("<tr><td colspan=\"4\">" + fileName + "</td></tr>").getBytes());
            reader = new BufferedReader(new InputStreamReader(file));
            String line = reader.readLine();
            int i = 0;
            Matcher matcher = null;
            String replace = null;
            while (line != null) {
                matcher = null;

                for (KeyValue kv : mapping) {
                    i++;
                    replace = kv.getValue();
                    matcher = findByKey(i, line, kv.getKey(), replace, logFile);
                    if (matcher != null) {
                        break;
                    }
                }
                if (outputStream != null) {
                    outputStream.write(((matcher == null ? line : matcher.replaceAll(replace)) + "\n").getBytes());
                }

                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Pattern> patterns = new HashMap<>();

    public static Matcher findByKey(int i, String line, String pattern, String replace, FileOutputStream logFile) throws IOException {
        Pattern r = patterns.get(pattern);
        if (r == null) {
            r = Pattern.compile(pattern);
            patterns.put(pattern, r);
        }

        Matcher m = r.matcher(line);
        if (m.find()) {
            logFile.write(("<tr><td>" + i + "</td><td>" + line.replaceAll("<", "&lt;") + "</td><td width='300'>" + m.group(0) + "</td><td width='300'>" + replace + "</td></tr>").getBytes());
            return m;
        }
        return null;
    }

    public static void listf(String directoryName, List<File> files) {
        File directory = new File(directoryName);

        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    listf(file.getAbsolutePath(), files);
                }
            }
        }


    }

    static List<KeyValue> readMapping(String mappingFileName) throws IOException, BackingStoreException {
        List<KeyValue> maps = new ArrayList<>();
        Ini ini = new Ini(new File(mappingFileName));
        java.util.prefs.Preferences prefs = new IniPreferences(ini);
        Preferences urlp = prefs.node("url");
        String[] urls = urlp.keys();
        Preferences contextp = prefs.node("context");
        String[] contexts = contextp.keys();
        for (String url : urls) {
            for (String context : contexts) {
                maps.add(new KeyValue(url + "/" + context, urlp.get(url, null) + "/" + contextp.get(context, null)));
            }
        }

        for (String url : urls) {

            maps.add(new KeyValue(url, urlp.get(url, null)));
        }

        return maps;
    }

    public static CommandLine checkOptions(String[] args) throws ParseException {
        Options options = new Options();
        Option opt = new Option("h", "help", false, "Print help");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("c", "config", true, "Config file");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("m", "model", true, "model[find/replace]");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("i", "in", true, "Input folder");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("o", "out", true, "Output folder");
        opt.setRequired(true);
        options.addOption(opt);

        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(110);
        CommandLine commandLine = null;
        CommandLineParser parser = new DefaultParser();
        try {
            commandLine = parser.parse(options, args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        if (commandLine == null || commandLine.hasOption('h')) {
            hf.printHelp("testApp", options, true);
        }


        return commandLine;
    }

    private static void printProgress(long startTime, long total, long current) {
        long eta = current == 0 ? 0 :
                (total - current) * (System.currentTimeMillis() - startTime) / current;

        String etaHms = current == 0 ? "N/A" :
                String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
                        TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));

        StringBuilder string = new StringBuilder(140);
        int percent = (int) (current * 100 / total);
        string
                .append('\r')
                .append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
                .append(String.format(" %d%% [", percent))
                .append(String.join("", Collections.nCopies(percent, "=")))
                .append('>')
                .append(String.join("", Collections.nCopies(100 - percent, " ")))
                .append(']')
                .append(String.join("", Collections.nCopies(current == 0 ? (int) (Math.log10(total)) : (int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
                .append(String.format(" %d/%d, ETA: %s", current, total, etaHms));

        System.out.print(string);
    }
}
