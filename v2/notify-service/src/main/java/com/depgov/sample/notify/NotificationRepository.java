package com.depgov.sample.notify;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {
    private final JdbcTemplate jdbc;
    private final NotificationCodec codec;

    public NotificationRepository(JdbcTemplate jdbc, NotificationCodec codec) {
        this.jdbc = jdbc;
        this.codec = codec;
    }

    public void save(Notification notification) {
        jdbc.update("insert into notification_outbox (id, payload) values (?, ?)",
                notification.id(), codec.encode(notification));
    }

    public Notification find(String id) {
        List<String> rows = jdbc.queryForList(
                "select payload from notification_outbox where id = ?", String.class, id);
        return rows.isEmpty() ? null : codec.decode(rows.getFirst());
    }
}
