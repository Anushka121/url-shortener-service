package com.example.urlshortener.entity;

import lombok.*;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("url_mapping")
public class UrlMapping {

    @PrimaryKey
    @Column("short_code")
    private String shortCode;

    @Column("original_url")
    private String originalUrl;

    @Column("custom_alias")
    private Boolean customAlias;

    @Column("created_at")
    private Instant createdAt;
}