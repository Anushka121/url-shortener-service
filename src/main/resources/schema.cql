CREATE KEYSPACE IF NOT EXISTS url_shortener
    WITH replication = {
        'class': 'SimpleStrategy',
        'replication_factor': 1
        }
     AND durable_writes = true;


USE url_shortener;


CREATE TABLE IF NOT EXISTS url_mapping (
                                           short_code   text PRIMARY KEY,
                                           original_url text,
                                           custom_alias boolean,
                                           created_at   timestamp
);


CREATE TABLE IF NOT EXISTS url_clicks (
                                          short_code text PRIMARY KEY,
                                          click_count counter
);

