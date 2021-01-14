package io.openvidu.server.core;

import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class UseTime {

    private static final ThreadLocal<List<Point>> threadLocal = new ThreadLocal<>();

    public static void start() {
        List<Point> list = new ArrayList<>();
        Point point = new Point("start", System.currentTimeMillis());
        list.add(point);
        threadLocal.set(list);
    }

    public static void point(String name) {
        Point point = new Point(name, System.currentTimeMillis());
        List<Point> list = threadLocal.get();
        if (list == null) {
            list = new LinkedList<>();
        }
        list.add(point);
    }


    public static long elapse(){
        long now = System.currentTimeMillis();
        List<Point> list = threadLocal.get();
        if (list == null) {
            return -1L;
        }
        Point point = list.get(0);
        if (point == null) {
            return -1L;
        }
        return now - point.getTime();
    }

    public static String endAndPrint() {
        Point point = new Point("end", System.currentTimeMillis());
        List<Point> list = threadLocal.get();
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(point);


        StringBuilder msg = new StringBuilder();

        Point pre = list.get(0);
        Point end = list.get(list.size() - 1);
        msg.append(MessageFormat.format("{0}-{1}={2},  ", end.getName(), pre.getName(), (end.getTime() - pre.getTime())));

        for (int i = 1; i < list.size(); i++) {
            Point now = list.get(i);
            msg.append(MessageFormat.format("{0}-{1}={2},  ", now.getName(), pre.getName(), (now.getTime() - pre.getTime())));
            pre = now;
        }
        threadLocal.remove();
        return msg.toString();
    }

    static class Point {
        String name;
        long time;

        public Point(String name, long time) {
            this.name = name;
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public long getTime() {
            return time;
        }
    }
}
