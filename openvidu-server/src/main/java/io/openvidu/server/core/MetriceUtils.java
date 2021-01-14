package io.openvidu.server.core;

import com.codahale.metrics.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class MetriceUtils {

    private static MetricRegistry registry = new MetricRegistry();

    private static Slf4jReporter reporter = Slf4jReporter.forRegistry(registry).build();

    static {
        reporter.start(5, TimeUnit.MINUTES);
    }

    public static Histogram histogram(String name, long value) {
        long now = System.currentTimeMillis();
        Histogram histogram = registry.histogram(name);
        histogram.update(now - value);
        return histogram;
    }

    public static void meterMark(String name) {
        registry.meter("meter-" + name).mark();
    }

    public static Timer.Context timer(String name) {
        return registry.timer(name).time();
    }

    public static void reset(long period) {
        synchronized (lock) {
            reporter.stop();
            registry = new MetricRegistry();
            reporter = Slf4jReporter.forRegistry(registry).build();
            reporter.start(period, TimeUnit.MINUTES);
        }
    }


    // ************************************************************************************************
    private static final Object lock = new Object();

    public static String report() {
        synchronized (lock) {
            return report(registry.getGauges(),
                    registry.getCounters(),
                    registry.getHistograms(),
                    registry.getMeters(),
                    registry.getTimers());
        }
    }

    private static String report(SortedMap<String, Gauge> gauges,
                                 SortedMap<String, Counter> counters,
                                 SortedMap<String, Histogram> histograms,
                                 SortedMap<String, Meter> meters,
                                 SortedMap<String, Timer> timers) {

        StringBuilder sb = new StringBuilder();
        sb.append("report time:").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
//        if (!gauges.isEmpty()) {
//            printWithBanner("-- Gauges", '-');
//            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
//                logGauge(entry.getKey(), entry.getValue());
//            }
//        }

//
//        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
//            logCounter(entry.getKey(), entry.getValue());
//        }
        if (!histograms.isEmpty()) {
            sb.append("-- Histograms --------------------------------------------------------------------------------\n");
            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                logHistogram(entry.getKey(), entry.getValue(), sb);
            }
        }

        if (!meters.isEmpty()) {
            sb.append("-- Meters --------------------------------------------------------------------------------\n");
            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                logMeter(entry.getKey(), entry.getValue(), sb);
            }
        }

        if (!timers.isEmpty()) {
            sb.append("-- Timers --------------------------------------------------------------------------------\n");
            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                logTimer(entry.getKey(), entry.getValue(), sb);
            }
        }

        return sb.toString();
    }

    private static void logTimer(String name, Timer timer, StringBuilder sb) {
        final Snapshot snapshot = timer.getSnapshot();
        sb.append(name).append("\n");
        sb.append(String.format(Locale.CHINA, "             count = %d\n", timer.getCount()));
        sb.append(String.format(Locale.CHINA, "         mean rate = %2.2f calls/%s\n", convertRate(timer.getMeanRate()), getRateUnit()));
        sb.append(String.format(Locale.CHINA, "     1-minute rate = %2.2f calls/%s\n", convertRate(timer.getOneMinuteRate()), getRateUnit()));
        sb.append(String.format(Locale.CHINA, "     5-minute rate = %2.2f calls/%s\n", convertRate(timer.getFiveMinuteRate()), getRateUnit()));
        sb.append(String.format(Locale.CHINA, "    15-minute rate = %2.2f calls/%s\n", convertRate(timer.getFifteenMinuteRate()), getRateUnit()));

        sb.append(String.format(Locale.CHINA, "               min = %2.2f %s\n", convertDuration(snapshot.getMin()), getDurationUnit()));
        sb.append(String.format(Locale.CHINA, "               max = %2.2f %s\n", convertDuration(snapshot.getMax()), getDurationUnit()));
        sb.append(String.format(Locale.CHINA, "              mean = %2.2f %s\n", convertDuration(snapshot.getMean()), getDurationUnit()));
        sb.append(String.format(Locale.CHINA, "            stddev = %2.2f %s\n", convertDuration(snapshot.getStdDev()), getDurationUnit()));
        sb.append(String.format(Locale.CHINA, "            median = %2.2f %s\n", convertDuration(snapshot.getMedian()), getDurationUnit()));
        sb.append(String.format(Locale.CHINA, "              75%% <= %2.2f %s\n", convertDuration(snapshot.get75thPercentile()), getDurationUnit()));
        sb.append(String.format(Locale.CHINA, "              95%% <= %2.2f %s\n", convertDuration(snapshot.get95thPercentile()), getDurationUnit()));
        sb.append(String.format(Locale.CHINA, "              98%% <= %2.2f %s\n", convertDuration(snapshot.get98thPercentile()), getDurationUnit()));
        sb.append(String.format(Locale.CHINA, "              99%% <= %2.2f %s\n", convertDuration(snapshot.get99thPercentile()), getDurationUnit()));
        sb.append(String.format(Locale.CHINA, "            99.9%% <= %2.2f %s\n", convertDuration(snapshot.get999thPercentile()), getDurationUnit()));

    }


    private static void logHistogram(String name, Histogram histogram, StringBuilder sb) {
        sb.append(name).append("\n");
        sb.append(String.format(Locale.CHINA, "             count = %d\n", histogram.getCount()));
        Snapshot snapshot = histogram.getSnapshot();
        sb.append(String.format(Locale.CHINA, "               min = %d\n", snapshot.getMin()));
        sb.append(String.format(Locale.CHINA, "               max = %d\n", snapshot.getMax()));
        sb.append(String.format(Locale.CHINA, "              mean = %2.2f\n", snapshot.getMean()));
        sb.append(String.format(Locale.CHINA, "            stddev = %2.2f\n", snapshot.getStdDev()));
        sb.append(String.format(Locale.CHINA, "            median = %2.2f\n", snapshot.getMedian()));
        sb.append(String.format(Locale.CHINA, "              75%% <= %2.2f\n", snapshot.get75thPercentile()));
        sb.append(String.format(Locale.CHINA, "              95%% <= %2.2f\n", snapshot.get95thPercentile()));
        sb.append(String.format(Locale.CHINA, "              98%% <= %2.2f\n", snapshot.get98thPercentile()));
        sb.append(String.format(Locale.CHINA, "              99%% <= %2.2f\n", snapshot.get99thPercentile()));
        sb.append(String.format(Locale.CHINA, "            99.9%% <= %2.2f\n", snapshot.get999thPercentile()));
    }

    private static void logMeter(String name, Meter meter, StringBuilder sb) {
        sb.append(name).append("\n");
        sb.append(String.format(Locale.CHINA, "             count = %d\n", meter.getCount()));
        sb.append(String.format(Locale.CHINA, "         mean rate = %2.2f events/%s\n", convertRate(meter.getMeanRate()), getRateUnit()));
        sb.append(String.format(Locale.CHINA, "     1-minute rate = %2.2f events/%s\n", convertRate(meter.getOneMinuteRate()), getRateUnit()));
        sb.append(String.format(Locale.CHINA, "     5-minute rate = %2.2f events/%s\n", convertRate(meter.getFiveMinuteRate()), getRateUnit()));
        sb.append(String.format(Locale.CHINA, "    15-minute rate = %2.2f events/%s\n", convertRate(meter.getFifteenMinuteRate()), getRateUnit()));
    }

    private static final TimeUnit rateUnit = TimeUnit.SECONDS;
    private static final TimeUnit durationUnit = TimeUnit.MILLISECONDS;
    private static final long durationFactor = durationUnit.toNanos(1);

    protected static String getRateUnit() {
        return calculateRateUnit(rateUnit);
    }

    private static String calculateRateUnit(TimeUnit unit) {
        final String s = unit.toString().toLowerCase(Locale.CHINA);
        return s.substring(0, s.length() - 1);
    }

    private static double convertRate(double rate) {
        return rate * rateUnit.toSeconds(1);
    }

    protected static double convertDuration(double duration) {
        return duration / durationFactor;
    }

    protected static String getDurationUnit() {
        return durationUnit.toString().toLowerCase(Locale.CHINA);
    }

    public static void main(String[] args) throws InterruptedException {
        Random random = new Random(23);
        for (int i = 0; i < 10; i++) {
            String name = "testName-" + random.nextInt(3);
//            MetriceUtils.meterMark(name);
//            MetriceUtils.histogram(name, System.currentTimeMillis() - random.nextInt(1000));

            Timer.Context timer = MetriceUtils.timer(name);
            if (i > 7) {
                continue;
            }
            TimeUnit.MILLISECONDS.sleep(random.nextInt(1000));
            timer.stop();

        }
        System.out.println(MetriceUtils.report());

        reset(5);
        for (int i = 0; i < 10; i++) {
            String name = "testName-" + random.nextInt(3);
//            MetriceUtils.meterMark(name);
//            MetriceUtils.histogram(name, System.currentTimeMillis() - random.nextInt(1000));

            Timer.Context timer = MetriceUtils.timer(name);
            if (i > 7) {
                continue;
            }
            TimeUnit.MILLISECONDS.sleep(random.nextInt(1000));
            timer.stop();

        }
        System.out.println(MetriceUtils.report());
    }
}
