package com.example.urlshortener.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("url_clicks")
public class UrlClicks {

    @PrimaryKey
    @Column("short_code")
    private String shortCode;

    @Column("click_count")
    private Long clickCount;
}