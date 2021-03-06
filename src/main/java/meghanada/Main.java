package meghanada;

import meghanada.config.Config;
import meghanada.server.Server;
import meghanada.server.emacs.EmacsServer;
import meghanada.utils.FileUtils;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Main {

    public static final String VERSION = "0.7.2";

    private static final Logger log = LogManager.getLogger(Main.class);

    public static String getVersion() throws IOException {
        final String version = FileUtils.getVersionInfo();
        if (version != null) {
            return version;
        }
        return Main.VERSION;
    }

    public static void main(String args[]) throws ParseException, IOException {
        final String version = getVersion();
        System.setProperty("meghanada-server.version", version);

        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
                log.catching(e));

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                log.info("shutdown server")));

        final Options options = buildOptions();

        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("meghanada server", options);
            return;
        }
        if (cmd.hasOption("version")) {
            System.out.println(version);
            return;
        }

        System.setProperty("home", Config.getInstalledPath().getParentFile().getCanonicalPath());

        if (isDevelop()) {
            addFileAppender();
        }

        if (cmd.hasOption("v")) {
            final LoggerContext context = (LoggerContext) LogManager.getContext(false);
            final Configuration configuration = context.getConfiguration();
            final LoggerConfig loggerConfig = configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
            loggerConfig.setLevel(Level.DEBUG);
            context.updateLoggers();
            System.setProperty("log-level", "DEBUG");
            log.debug("set verbose flag(DEBUG)");
        }

        if (cmd.hasOption("vv")) {
            final LoggerContext context = (LoggerContext) LogManager.getContext(false);
            final Configuration configuration = context.getConfiguration();
            final LoggerConfig loggerConfig = configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

            loggerConfig.setLevel(Level.TRACE);
            context.updateLoggers();
            System.setProperty("log-level", "TRACE");
            log.debug("set verbose flag(TRACE)");
        }

        if (cmd.hasOption("clear-cache")) {
            System.setProperty("clear-cache-on-start", "true");
        }

        if (cmd.hasOption("gradle-version")) {
            final String gradleVersion = cmd.getOptionValue("gradle-version", "");
            if (!version.isEmpty()) {
                System.setProperty("gradle-version", gradleVersion);
            }
        }

        String port = "55555";
        String projectRoot = "./";
        String fmt = "sexp";

        if (cmd.hasOption("p")) {
            port = cmd.getOptionValue("p", port);
        }
        if (cmd.hasOption("r")) {
            projectRoot = cmd.getOptionValue("r", projectRoot);
        }
        if (cmd.hasOption("output")) {
            fmt = cmd.getOptionValue("output", fmt);
        }
        log.debug("set port:{}, projectRoot:{}, output:{}", port, projectRoot, fmt);
        final int portInt = Integer.parseInt(port);

        log.info("Meghanada-Server Version:{}", version);
        final Server server = createServer("localhost", portInt, projectRoot, fmt);
        server.startServer();
    }

    private static void addFileAppender() throws IOException {
        final String tempDir = System.getProperty("java.io.tmpdir");
        final File logFile = new File(tempDir, "meghanada_server.log");
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = context.getConfiguration();
        final LoggerConfig loggerConfig = configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        final FileAppender fileAppender = FileAppender.newBuilder()
                .withName("file")
                .withLayout(PatternLayout.newBuilder()
                        .withPattern("[%d][%-5.-5p][%-14.-14c{1}:%4L] %-22.-22M - %m%n")
                        .build())
                .withFileName(logFile.getCanonicalPath())
                .build();
        configuration.addAppender(fileAppender);
        loggerConfig.addAppender(fileAppender, Level.INFO, null);
        context.updateLoggers();
    }

    private static Server createServer(final String host, final int port, final String projectRoot, final String fmt)
            throws IOException {
        return new EmacsServer(host, port, projectRoot);
    }

    private static Options buildOptions() {
        final Options options = new Options();
        final Option help = new Option("h", "help", false, "show help");
        options.addOption(help);
        final Option version = new Option(null, "version", false, "show version information");
        options.addOption(version);
        final Option port = new Option("p", "port", true, "set server port. default: 55555");
        options.addOption(port);
        final Option project = new Option("r", "project", true, "set project root path. default: current path ");
        options.addOption(project);
        final Option verbose = new Option("v", "verbose", false, "show verbose message (DEBUG)");
        options.addOption(verbose);
        final Option traceVerbose = new Option("vv", "traceVerbose", false, "show verbose message (TRACE)");
        options.addOption(traceVerbose);
        final Option out = new Option(null, "output", true, "output format (sexp, csv, json). default: sexp");
        options.addOption(out);
        final Option gradleVersion = new Option(null, "gradle-version", true, "set use gradle version");
        options.addOption(gradleVersion);
        final Option clearCache = new Option("c", "clear-cache", false, "clear cache on start");
        options.addOption(clearCache);
        return options;
    }

    public static boolean isDevelop() {
        final String dev = System.getenv("MEGHANADA_DEVELOP");
        return Objects.equals(dev, "1");
    }

}
