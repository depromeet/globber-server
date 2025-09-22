CREATE TABLE IF NOT EXISTS city (
                                    city_id SERIAL PRIMARY KEY,
                                    city_name TEXT NOT NULL,
                                    country_name TEXT NOT NULL
);