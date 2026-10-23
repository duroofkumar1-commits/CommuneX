-- CommuneX: 600 DEMO/SAMPLE inventory records (60 items x 10 locations)
-- IMPORTANT: These are synthetic test records, NOT verified physical stock.
-- Review and verify inventory before using this dataset for real relief decisions.
-- This script does not delete existing data. It skips rows with the same name + location + category.
-- Intended for PostgreSQL / pgAdmin Query Tool.

BEGIN;

WITH materials (resource_name, category, item_description, unit, base_quantity, low_stock_threshold) AS (
    VALUES
        ('Rice bags', 'Food', 'Staple rice stock for community meal distribution.', 'kg', 120, 30),
        ('Wheat flour bags', 'Food', 'Wheat flour for household food support.', 'kg', 100, 25),
        ('Lentils packs', 'Food', 'Dry lentils for community kitchens.', 'kg', 75, 20),
        ('Cooking oil bottles', 'Food', 'Sealed cooking oil bottles for food parcels.', 'bottle', 80, 20),
        ('Sugar bags', 'Food', 'Packaged sugar for household food parcels.', 'kg', 60, 15),
        ('Salt packs', 'Food', 'Packaged table salt for household supplies.', 'kg', 50, 12),
        ('Chickpea bags', 'Food', 'Dry chickpeas for community kitchens.', 'kg', 70, 18),
        ('Dates cartons', 'Food', 'Packed dates for emergency food distribution.', 'carton', 40, 10),
        ('Milk powder tins', 'Food', 'Sealed milk powder for eligible food-support programmes.', 'tin', 45, 12),
        ('Ready-to-eat meal packs', 'Food', 'Shelf-stable meals for emergency distribution.', 'pack', 90, 25),
        ('Biscuits cartons', 'Food', 'Sealed biscuit cartons for relief packs.', 'carton', 55, 15),
        ('Fortified food packs', 'Food', 'Fortified packaged food for nutrition-support programmes.', 'pack', 65, 18),
        ('Tea packs', 'Food', 'Packaged tea for household essentials.', 'pack', 35, 10),
        ('Drinking water bottles', 'Water & Sanitation', 'Sealed drinking-water bottles; verify expiry and seal condition.', 'bottle', 200, 50),
        ('Drinking water cans', 'Water & Sanitation', 'Sealed drinking-water cans for distribution.', 'can', 100, 25),
        ('Water storage containers', 'Water & Sanitation', 'Clean reusable containers for safe water storage.', 'piece', 35, 10),
        ('Water purification tablet packs', 'Water & Sanitation', 'Packaged water-purification tablets; follow product instructions.', 'pack', 40, 10),
        ('Water filter units', 'Water & Sanitation', 'Portable water-filter units for community use.', 'unit', 20, 5),
        ('Bath soap bars', 'Water & Sanitation', 'Wrapped soap bars for hygiene kits.', 'bar', 120, 30),
        ('Hand sanitizer bottles', 'Water & Sanitation', 'Sealed hand-sanitizer bottles.', 'bottle', 70, 18),
        ('Laundry detergent packs', 'Water & Sanitation', 'Packaged laundry detergent for household hygiene.', 'pack', 60, 15),
        ('Sanitary pad packs', 'Water & Sanitation', 'Sealed menstrual-hygiene packs.', 'pack', 80, 20),
        ('Diaper packs', 'Water & Sanitation', 'Sealed disposable diaper packs in mixed sizes.', 'pack', 55, 14),
        ('Toothbrush sets', 'Water & Sanitation', 'Basic toothbrush sets for hygiene kits.', 'set', 75, 18),
        ('Toothpaste tubes', 'Water & Sanitation', 'Sealed toothpaste tubes for hygiene kits.', 'tube', 75, 18),
        ('Reusable towels', 'Water & Sanitation', 'Clean reusable towels for household hygiene kits.', 'piece', 45, 12),
        ('First-aid kits', 'Medical Supplies', 'Sealed general first-aid kits; check contents and expiry.', 'kit', 25, 7),
        ('Bandage rolls', 'Medical Supplies', 'Packaged bandage rolls for authorised first-aid stock.', 'roll', 100, 25),
        ('Sterile gauze packs', 'Medical Supplies', 'Sealed sterile gauze packs; check expiry before use.', 'pack', 65, 16),
        ('Disposable gloves boxes', 'Medical Supplies', 'Disposable examination gloves; box count varies by product.', 'box', 45, 12),
        ('Surgical masks boxes', 'Medical Supplies', 'Sealed medical-mask boxes.', 'box', 50, 12),
        ('Digital thermometers', 'Medical Supplies', 'Digital thermometers for authorised health facilities.', 'unit', 18, 5),
        ('ORS sachet packs', 'Medical Supplies', 'Oral rehydration sachets; follow packet directions and health guidance.', 'pack', 90, 22),
        ('Antiseptic solution bottles', 'Medical Supplies', 'Sealed antiseptic solution; check product label and expiry.', 'bottle', 35, 9),
        ('Wound dressing packs', 'Medical Supplies', 'Sealed wound-dressing packs for trained care providers.', 'pack', 45, 12),
        ('Walking crutches', 'Medical Supplies', 'Assistive walking crutches, subject to fit and condition checks.', 'pair', 12, 3),
        ('Wheelchairs', 'Medical Supplies', 'Wheelchairs for loan or distribution after condition inspection.', 'unit', 8, 2),
        ('Medical cotton rolls', 'Medical Supplies', 'Packaged medical cotton rolls.', 'roll', 55, 14),
        ('Medical waste bags', 'Medical Supplies', 'Medical-waste bags for use by authorised facilities.', 'pack', 35, 9),
        ('Emergency blankets', 'Shelter & Relief', 'Packed emergency thermal blankets.', 'piece', 80, 20),
        ('Tarpaulin sheets', 'Shelter & Relief', 'Weather-resistant tarpaulin sheets for temporary shelter.', 'sheet', 60, 15),
        ('Family emergency tents', 'Shelter & Relief', 'Packed family tents; inspect parts and condition before issue.', 'tent', 15, 4),
        ('Sleeping mats', 'Shelter & Relief', 'Sleeping mats for temporary shelter centres.', 'piece', 70, 18),
        ('Mosquito nets', 'Shelter & Relief', 'Packaged mosquito nets for household protection.', 'piece', 75, 18),
        ('Rope coils', 'Shelter & Relief', 'General-purpose rope coils for safe shelter setup.', 'coil', 30, 8),
        ('Plastic ground sheets', 'Shelter & Relief', 'Plastic ground sheets for temporary shelter flooring.', 'sheet', 45, 12),
        ('Solar lanterns', 'Shelter & Relief', 'Solar lanterns; check charging and function before issue.', 'unit', 25, 6),
        ('Rechargeable torches', 'Shelter & Relief', 'Rechargeable torches for emergency lighting.', 'unit', 35, 9),
        ('Battery packs', 'Shelter & Relief', 'Packaged batteries matching approved device requirements.', 'pack', 40, 10),
        ('Heavy-duty buckets', 'Shelter & Relief', 'Clean buckets for household and relief-centre use.', 'piece', 55, 14),
        ('Water storage drums', 'Shelter & Relief', 'Clean covered drums for non-hazardous water storage.', 'drum', 15, 4),
        ('Basic tool kits', 'Shelter & Relief', 'Basic hand-tool kits for authorised maintenance tasks.', 'kit', 12, 3),
        ('Adult clothing kits', 'Clothing', 'Clean packaged adult clothing kits; check sizes before distribution.', 'kit', 45, 12),
        ('Children clothing kits', 'Clothing', 'Clean packaged children''s clothing kits; check sizes before distribution.', 'kit', 50, 12),
        ('Baby clothing packs', 'Clothing', 'Clean packaged baby clothing packs.', 'pack', 35, 9),
        ('School notebooks', 'Education', 'Blank notebooks for school-support kits.', 'piece', 150, 35),
        ('Pencil packs', 'Education', 'Pencil packs for student stationery kits.', 'pack', 90, 22),
        ('School bags', 'Education', 'School bags for student-support programmes.', 'piece', 45, 12),
        ('Stationery kits', 'Education', 'Basic stationery kits for students.', 'kit', 60, 15),
        ('School textbook bundles', 'Education', 'Sample textbook bundles; confirm grade and curriculum before issue.', 'bundle', 30, 8)
),
locations (site_name, location_name) AS (
    VALUES
        ('Mithi Relief Depot', 'Mithi, Tharparkar'),
        ('Islamkot Community Store', 'Islamkot, Tharparkar'),
        ('Diplo Community Store', 'Diplo, Tharparkar'),
        ('Chachro Relief Depot', 'Chachro, Tharparkar'),
        ('Nagarparkar Community Store', 'Nagarparkar, Tharparkar'),
        ('Umerkot Distribution Store', 'Umerkot, Sindh'),
        ('Mirpur Khas Warehouse', 'Mirpur Khas, Sindh'),
        ('Hyderabad Relief Warehouse', 'Hyderabad, Sindh'),
        ('Badin Community Store', 'Badin, Sindh'),
        ('Karachi Central Store', 'Karachi, Sindh')
),
generated AS (
    SELECT
        m.resource_name || ' - ' || l.site_name AS name,
        m.category,
        'DEMO SAMPLE DATA: ' || m.item_description || ' Verify physical stock, condition, and expiry before distribution.' AS description,
        l.location_name AS location,
        CASE
            WHEN ROW_NUMBER() OVER (ORDER BY l.site_name, m.resource_name)::integer % 7 = 0
                THEN GREATEST(0, m.low_stock_threshold - 1)
            ELSE m.base_quantity
                 + ((ROW_NUMBER() OVER (ORDER BY l.site_name, m.resource_name)::integer * 37) % 250)
        END::numeric(12,2) AS quantity,
        m.unit,
        m.low_stock_threshold::numeric(12,2) AS low_stock_threshold,
        TRUE AS active,
        CURRENT_TIMESTAMP AS created_at,
        CURRENT_TIMESTAMP AS updated_at
    FROM materials m
    CROSS JOIN locations l
),
inserted AS (
    INSERT INTO resource_items
        (name, category, description, location, quantity, unit,
         low_stock_threshold, active, created_at, updated_at)
    SELECT
        g.name, g.category, g.description, g.location, g.quantity,
        g.unit, g.low_stock_threshold, g.active, g.created_at, g.updated_at
    FROM generated g
    WHERE NOT EXISTS (
        SELECT 1
        FROM resource_items existing
        WHERE LOWER(existing.name) = LOWER(g.name)
          AND COALESCE(LOWER(existing.location), '') = COALESCE(LOWER(g.location), '')
          AND LOWER(existing.category) = LOWER(g.category)
    )
    RETURNING id
)
SELECT COUNT(*) AS inserted_records FROM inserted;

COMMIT;

-- After running, check total active demo records with:
-- SELECT COUNT(*) FROM resource_items WHERE active = TRUE AND description LIKE 'DEMO SAMPLE DATA:%';
