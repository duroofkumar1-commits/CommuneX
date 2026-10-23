-- V12: Additional Tharparkar Map Explorer demo catalog.
-- Synthetic demonstration markers only; NOT real or verified businesses/facilities.
-- Coordinates are approximate town-area demo pins; do not use for navigation or emergency dispatch.
-- Generates 350 markers across the seven existing categories (50 each), plus 120 Other markers.

-- Ensure the metadata table exists before the first insert that uses it.
CREATE TABLE IF NOT EXISTS facility_data_sources (
    facility_id BIGINT PRIMARY KEY REFERENCES facilities(id) ON DELETE CASCADE,
    source_name VARCHAR(160) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    source_record_id VARCHAR(120),
    verification_status VARCHAR(50) NOT NULL,
    location_precision VARCHAR(50) NOT NULL,
    coordinate_source_name VARCHAR(160),
    coordinate_source_url VARCHAR(1000),
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_facility_data_sources_status
    ON facility_data_sources(verification_status);

WITH towns(town, lat, lon, taluka, ord) AS (VALUES
    ('Mithi',24.7401,69.8004,'Mithi',1),
    ('Islamkot',24.6998,70.1800,'Islamkot',2),
    ('Diplo',24.4673,69.5823,'Diplo',3),
    ('Chachro',25.1161,70.2518,'Chachro',4),
    ('Nagarparkar',24.3579,70.7551,'Nagarparkar',5),
    ('Kaloi',24.7700,69.5700,'Kaloi',6),
    ('Dahli',24.9000,70.3500,'Dahli',7),
    ('Chelhar',24.9768,69.9215,'Chachro',8),
    ('Joruo',24.8200,69.9300,'Mithi',9),
    ('Veerawah',24.5868,70.6810,'Nagarparkar',10)
), cats(category, ord) AS (VALUES
    ('Hospital',1),('Clinic',2),('Pharmacy',3),('Shelter',4),
    ('Library',5),('Community Centre',6),('Food Support',7)
), generated AS (
    SELECT i, t.town, t.taluka, c.category,
           ('[DEMO] ' || c.category || ' Service Point ' || lpad(i::text,3,'0') || ' — ' || t.town) AS name,
           format('[DEMO] Synthetic Map Explorer marker for testing the %s category. This is not a verified real-world facility. Approximate town-area coordinate only; confirm the actual location before visiting.', c.category) AS description,
           (t.lat + (((i % 9)-4) * 0.0020))::numeric(10,7) AS latitude,
           (t.lon + (((i % 11)-5) * 0.0020))::numeric(10,7) AS longitude
    FROM generate_series(1,350) AS gs(i)
    JOIN towns t ON t.ord = ((i-1) % 10)+1
    JOIN cats c ON c.ord = ((i-1) % 7)+1
), other_types(label, ord) AS (VALUES
    ('Malls',1),('Shops & Markets',2),('Mosques & Worship',3),
    ('Police & Fire Station',4),('Schools & Colleges',5),('Banks & ATMs',6),
    ('Petrol Pumps',7),('Restaurants & Cafe',8),('Hardware & Repair',9),
    ('Transport & Public Service',10)
), others AS (
    -- IMPORTANT: taluka appears only once in this CTE to avoid an ambiguous column reference.
    SELECT i, t.town, t.taluka,
           ('[DEMO] ' || ot.label || ' Place ' || lpad(i::text,3,'0') || ' — ' || t.town) AS name,
           format('[DEMO] Synthetic example for the %s place type. Not a real or verified business/facility. Approximate town-area coordinate only; no phone number or opening hours are claimed.', ot.label) AS description,
           'Other'::varchar(60) AS category,
           (t.lat + (((i % 9)-4) * 0.0020))::numeric(10,7) AS latitude,
           (t.lon + (((i % 11)-5) * 0.0020))::numeric(10,7) AS longitude
    FROM generate_series(1,120) AS gs(i)
    JOIN towns t ON t.ord = ((i-1) % 10)+1
    JOIN other_types ot ON ot.ord = ((i-1) % 10)+1
), all_rows AS (
    SELECT name, category, description,
           format('%s, Taluka %s, Tharparkar District, Sindh, Pakistan', town, taluka) AS address,
           latitude, longitude
    FROM generated
    UNION ALL
    SELECT name, category, description,
           format('%s, Taluka %s, Tharparkar District, Sindh, Pakistan', town, taluka) AS address,
           latitude, longitude
    FROM others
 )
INSERT INTO facilities
    (name,category,description,address,latitude,longitude,phone,website,opening_information,verified,active,created_at,updated_at)
SELECT r.name,r.category,r.description,r.address,r.latitude,r.longitude,
       NULL,NULL,'Demo record; hours not verified',FALSE,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM all_rows r
WHERE NOT EXISTS (
    SELECT 1 FROM facilities f
    WHERE f.name = r.name AND f.category = r.category
);

-- This is a separate statement so it can see facilities inserted by the statement above.
INSERT INTO facility_data_sources
    (facility_id,source_name,source_url,source_record_id,verification_status,
     location_precision,coordinate_source_name,coordinate_source_url)
SELECT f.id,
       'CommuneX synthetic demo data',
       'https://www.openstreetmap.org/',
       'DEMO-' || f.id,
       'SYNTHETIC_NOT_REAL',
       'APPROXIMATE_TOWN_AREA',
       'Synthetic coordinate generator',
       NULL
FROM facilities f
WHERE f.name LIKE '[DEMO] %'
  AND f.description LIKE '%Synthetic%'
ON CONFLICT (facility_id) DO NOTHING;
