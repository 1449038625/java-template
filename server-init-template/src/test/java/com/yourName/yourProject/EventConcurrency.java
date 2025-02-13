package com.yourName.yourProject;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class EventConcurrency {

    public static class Event {
        String id;
        Date startTime;
        Date endTime;

        public Event(String id, Date startTime, Date endTime) {
            this.id = id;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static void main(String[] args) {
        // 数据库连接（示例中为模拟）
        String url = "jdbc:mysql://localhost:3306/events_db";
        String user = "root";
        String password = "password";

        // 每次查询的分页大小（每次加载的记录数）
        int pageSize = 10000;
        int currentPage = 1;

        // 使用 ConcurrentSkipListMap 来保证时间戳的线程安全排序
        Map<Long, Integer> timestampChanges = new ConcurrentSkipListMap<>();

        // 数据库连接（用于读取事件数据）
        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            // 假设总记录有 5000w 条，分页加载
            while (true) {
                List<Event> events = fetchEventsFromDatabase(conn, currentPage, pageSize);
                if (events.isEmpty()) break;  // 没有更多的事件数据，退出循环

                // 使用并行流处理事件，减少处理时间
                processEventsInParallel(events, timestampChanges);

                currentPage++;  // 下一页
            }

            // 计算所有事件的最大并发数
            Map.Entry<Long, Integer> result = getMaxConcurrency(timestampChanges);
            if (result != null) {
                System.out.println("最大并发数: " + result.getValue() + " 发生在时间戳: " + formatTimestamp(result.getKey()));
            } else {
                System.out.println("没有找到事件数据。");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 从数据库中获取事件数据，分页查询
    public static List<Event> fetchEventsFromDatabase(Connection conn, int page, int pageSize) throws SQLException {
        String query = "SELECT id, start_time, end_time FROM events LIMIT ?, ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, (page - 1) * pageSize);
            ps.setInt(2, pageSize);

            try (ResultSet rs = ps.executeQuery()) {
                List<Event> events = new ArrayList<>();
                while (rs.next()) {
                    String id = rs.getString("id");
                    Date startTime = rs.getTimestamp("start_time");
                    Date endTime = rs.getTimestamp("end_time");
                    events.add(new Event(id, startTime, endTime));
                }
                return events;
            }
        }
    }

    // 使用并行流处理事件并更新时间戳变化
    public static void processEventsInParallel(List<Event> events, Map<Long, Integer> timestampChanges) {
        events.parallelStream().forEach(event -> {
            // 对开始时间添加 +1
            long startTimestamp = event.startTime.getTime() / 1000;
            timestampChanges.merge(startTimestamp, 1, Integer::sum);

            // 对结束时间（加1秒）添加 -1
            long endTimestamp = (event.endTime.getTime() + 1000) / 1000;  // 结束时间 + 1 秒
            timestampChanges.merge(endTimestamp, -1, Integer::sum);
        });
    }

    // 从时间戳变化映射中计算最大并发数
    public static Map.Entry<Long, Integer> getMaxConcurrency(Map<Long, Integer> timestampChanges) {
        int currentConcurrency = 0;
        int maxConcurrency = 0;
        long maxConcurrencyTimestamp = -1;

        // 按时间戳排序，计算并发数
        for (Map.Entry<Long, Integer> entry : timestampChanges.entrySet()) {
            currentConcurrency += entry.getValue();
            if (currentConcurrency > maxConcurrency) {
                maxConcurrency = currentConcurrency;
                maxConcurrencyTimestamp = entry.getKey();
            }
        }

        return maxConcurrency > 0 ? new AbstractMap.SimpleEntry<>(maxConcurrencyTimestamp, maxConcurrency) : null;
    }

    // 将时间戳格式
    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp * 1000));
    }
}
