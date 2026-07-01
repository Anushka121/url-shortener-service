package com.example.urlshortener.repository;

import com.example.urlshortener.entity.UrlClicks;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlClicksRepository extends CassandraRepository<UrlClicks, String> {

    @Query("UPDATE url_clicks SET click_count = click_count + 1 WHERE short_code = ?0")
    void incrementClickCount(String shortCode);
}