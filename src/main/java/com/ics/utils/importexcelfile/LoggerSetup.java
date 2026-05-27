package com.ics.utils.importexcelfile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerSetup {

    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    public static void init() {
        File logDir = resolveLogDir();
        logDir.mkdirs();

        String dateSuffix = FILE_DATE.format(Instant.now());

        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);

        for (Handler h : root.getHandlers()) {
            root.removeHandler(h);
        }

        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(Level.INFO);
        console.setFormatter(buildFormatter());
        root.addHandler(console);

        // all.log — ทุก level
        addFile(root, new File(logDir, "all_" + dateSuffix + ".log"), Level.ALL, null);

        // info.log — เฉพาะ INFO (ไม่รวม WARNING/SEVERE)
        addFile(root, new File(logDir, "info_" + dateSuffix + ".log"), Level.INFO,
                record -> record.getLevel().intValue() < Level.WARNING.intValue());

        // error.log — WARNING + SEVERE
        addFile(root, new File(logDir, "error_" + dateSuffix + ".log"), Level.WARNING, null);
    }

    private static void addFile(Logger logger, File file, Level level,
                                java.util.logging.Filter filter) {
        try {
            FileHandler fh = new FileHandler(file.getAbsolutePath(), true);
            fh.setLevel(level);
            fh.setFormatter(buildFormatter());
            if (filter != null) {
                fh.setFilter(filter);
            }
            logger.addHandler(fh);
        } catch (IOException ex) {
            Logger.getLogger(LoggerSetup.class.getName())
                  .log(Level.WARNING, "Cannot create log file: " + file.getAbsolutePath(), ex);
        }
    }

    private static Formatter buildFormatter() {
        return new Formatter() {
            private final DateTimeFormatter dtf =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

            @Override
            public String format(LogRecord record) {
                String ts  = dtf.format(Instant.ofEpochMilli(record.getMillis()));
                String msg = formatMessage(record);
                if (record.getThrown() != null) {
                    StringWriter sw = new StringWriter();
                    record.getThrown().printStackTrace(new PrintWriter(sw));
                    msg += "\n" + sw;
                }
                return String.format("[%s] [%-7s] [%s] %s%n",
                        ts, record.getLevel().getName(), record.getLoggerName(), msg);
            }
        };
    }

    private static File resolveLogDir() {
        try {
            File jar = new File(LoggerSetup.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            return new File(jar.getParentFile(), "logs");
        } catch (URISyntaxException | SecurityException ex) {
            return new File("logs");
        }
    }
}
