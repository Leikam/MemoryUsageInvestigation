import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.sun.nio.file.ExtendedOpenOption;

public class STWInvestigation {

    // example line:
    // [1.070s][info][gc] GC(0) Pause Young (Allocation Failure) 50M->44M(192M) 15.469ms
    public static final Pattern STRING_CHECK_PATTERN = Pattern.compile(".*\\[info\\]\\[gc\\].*GC\\(.*");
    public static final Pattern OP_TIME_PATTERN = Pattern.compile("([0-9]+[.0-9]*(?=ms))");
    public static final Pattern GC_COUNTER_PATTERN = Pattern.compile("\\[info\\]\\[gc\\] GC\\(([0-9]+)\\)");
    public static long START = -1;
    public static double TIMEOUT = 20e3; //ms

    public static void main(String[] args) throws IOException {
        int i = 1;
        final Path reportFile = Path.of("./memory_report.log");
        for (String report : args) {
            final List<String> lines = Files.readAllLines(Path.of(report));
            final Number[] result = {0, 0};

            final ArrayDeque<String> collect = lines.stream()
                                                    .filter(log -> STRING_CHECK_PATTERN.matcher(log).matches())
                                                    .peek(
                                                        log -> {
                                                            final Matcher m = OP_TIME_PATTERN.matcher(log);
                                                            if (m.find()) {
                                                                final String opTime = m.group(1);
                                                                result[1] = result[1].floatValue() + Float.parseFloat(opTime);
                                                            }
                                                        }
                                                    )
                                                    .collect(Collectors.toCollection(ArrayDeque::new));

            int gcCount = 0;
            float gcTotalTime = result[1].floatValue();
            final String lastLog = collect.peekLast();
            if (lastLog != null) {
                final Matcher m = GC_COUNTER_PATTERN.matcher(lastLog);
                if (m.find()) {
                    gcCount = Integer.parseInt(m.group(1)) + 1;
                }
            }

            try (final BufferedWriter writer = Files.newBufferedWriter(reportFile, i > 1 ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.append(String.format("== test %d ==", i));
                writer.append(String.format("\nGC attempts: %d", gcCount));
                writer.append(String.format("\nGC avg time: %f(ms)", gcTotalTime / gcCount));
                writer.append(String.format("\nGC total time: %f(ms)", gcTotalTime));
                writer.append(String.format("\n\n"));
            }

            i++;
        }

    }

    public static void logCounterToExit() {
        START = System.currentTimeMillis();

        new Thread(() -> {
            do {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
            } while (System.currentTimeMillis() - START < TIMEOUT);

            System.out.println("timeMillis = " + (System.currentTimeMillis() - START));
            System.exit(0);
        }).start();
    }
}
