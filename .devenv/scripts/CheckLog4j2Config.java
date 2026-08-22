/*
 * Loads each shipped log4j2 configuration with the log4j-core the build resolved, and prints what
 * the parser actually made of it - every appender's target/file/charset/threshold, every logger's
 * level and additivity - plus any StatusLogger message at WARN or above.
 *
 *   docker-compose exec -T builder bash -lc 'mkdir -p /tmp/lc && cd /tmp/lc \
 *     && unzip -oqj /workspace/dist/catgenome-h2.jar "BOOT-INF/lib/log4j-api-*.jar" \
 *          "BOOT-INF/lib/log4j-core-*.jar" -d lib \
 *     && java -cp "lib/*" /workspace/.devenv/scripts/CheckLog4j2Config.java \
 *          /workspace/server/catgenome/profiles/{dev,jar,release,staging}/log4j2.xml \
 *          /workspace/server/ngb-cli/src/main/resources/log4j2.xml'
 *
 * Why it exists: a log4j2 configuration attribute that the running version does not know is not a
 * build failure and not a startup failure - it is silently ignored, and the appender comes up with
 * that attribute unset. So a log4j2 upgrade whose fix *is* an attribute rename (CVE-2026-34478,
 * closed in 2.25.4; see JAVA21-VULNERABILITY-REVIEW.md §4.1) cannot be verified by starting the
 * server and reading the log. This prints the parsed values so they can be compared with the file.
 *
 * Run it from a scratch directory: starting a configuration creates the files its RollingFile
 * appenders name, and the dev and jar profiles name them relative to the working directory.
 */
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;

public final class CheckLog4j2Config {

    private static final List<String> PROBLEMS = new ArrayList<>();

    private CheckLog4j2Config() {
    }

    public static void main(String[] args) throws Exception {
        StatusLogger.getLogger().registerListener(new StatusListener() {
            public void log(StatusData data) {
                if (data.getLevel().isMoreSpecificThan(Level.WARN)) {
                    PROBLEMS.add(data.getLevel() + " " + data.getMessage().getFormattedMessage());
                }
            }

            public Level getStatusLevel() {
                return Level.WARN;
            }

            public void close() {
            }
        });

        for (String path : args) {
            System.out.println("===== " + path);
            PROBLEMS.clear();
            File file = new File(path);
            LoggerContext ctx = new LoggerContext("check-" + file.getParentFile().getName());
            ConfigurationSource source = new ConfigurationSource(new FileInputStream(file), file);
            Configuration cfg = ConfigurationFactory.getInstance().getConfiguration(ctx, source);
            ctx.start(cfg);

            for (Map.Entry<String, Appender> entry : new TreeMap<>(cfg.getAppenders()).entrySet()) {
                System.out.println(describe(entry.getKey(), entry.getValue()));
            }
            for (Map.Entry<String, LoggerConfig> entry : new TreeMap<>(cfg.getLoggers()).entrySet()) {
                LoggerConfig logger = entry.getValue();
                System.out.println("  logger " + (entry.getKey().isEmpty() ? "<root>" : entry.getKey())
                        + " level=" + logger.getLevel()
                        + " additivity=" + logger.isAdditive()
                        + " refs=" + logger.getAppenderRefs());
            }
            System.out.println("  status WARN+: " + (PROBLEMS.isEmpty() ? "none" : PROBLEMS));
            ctx.stop();
        }
    }

    private static String describe(String name, Appender appender) {
        StringBuilder sb = new StringBuilder("  appender ").append(name).append(' ')
                .append(appender.getClass().getSimpleName());
        if (appender instanceof ConsoleAppender) {
            sb.append(" target=").append(((ConsoleAppender) appender).getTarget())
              .append(" ignoreExceptions=").append(appender.ignoreExceptions());
        }
        if (appender instanceof RollingFileAppender) {
            RollingFileAppender rolling = (RollingFileAppender) appender;
            sb.append(" fileName=").append(rolling.getFileName())
              .append(" filePattern=").append(rolling.getFilePattern())
              .append(" policy=").append(rolling.getTriggeringPolicy().getClass().getSimpleName());
        }
        Layout<?> layout = appender.getLayout();
        if (layout instanceof PatternLayout) {
            PatternLayout pattern = (PatternLayout) layout;
            Charset charset = pattern.getCharset();
            sb.append("\n    layout PatternLayout charset=").append(charset)
              .append(" pattern=").append(pattern.getConversionPattern());
        }
        if (appender instanceof AbstractFilterable) {
            sb.append("\n    filter ").append(describe(((AbstractFilterable) appender).getFilter()));
        }
        return sb.toString();
    }

    private static String describe(Filter filter) {
        if (filter == null) {
            return "none";
        }
        if (filter instanceof CompositeFilter) {
            StringBuilder sb = new StringBuilder("Composite[");
            for (Filter inner : ((CompositeFilter) filter).getFiltersArray()) {
                sb.append(describe(inner)).append(' ');
            }
            return sb.append(']').toString();
        }
        if (filter instanceof ThresholdFilter) {
            ThresholdFilter threshold = (ThresholdFilter) filter;
            return "ThresholdFilter level=" + threshold
                    + " onMatch=" + threshold.getOnMatch()
                    + " onMismatch=" + threshold.getOnMismatch();
        }
        return filter.getClass().getSimpleName();
    }
}
