-- OPT-IN ONLY: synthetic Tharparkar demo data for local/dev/test databases.
-- All addresses use reserved .test domains; they are not real people or Gmail accounts.
-- All posts/events/help requests/inventory are fictional and explicitly marked [DEMO].
-- Demo password for every newly-created account: TharDemo!2026
-- Do not run against production. Existing users with matching email addresses are not overwritten.

BEGIN;

-- Keep the exact set of accounts created by this seed so subsequent inserts do not accidentally
-- include unrelated demo/test users that happen to use the same email pattern.
CREATE TEMP TABLE communex_demo_seed_accounts (
    name TEXT NOT NULL,
    email TEXT PRIMARY KEY,
    role TEXT NOT NULL,
    avatar_seed TEXT NOT NULL,
    password_hash TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO communex_demo_seed_accounts(name,email,role,avatar_seed,password_hash)
WITH special_accounts(name,email,role,avatar_seed) AS (VALUES
    ('Amina Admin 01','demo.admin01@communex.test','ADMIN','admin-1'),
    ('Ramesh Admin 02','demo.admin02@communex.test','ADMIN','admin-2'),
    ('Sana Moderator 01','demo.moderator01@communex.test','MODERATOR','moderator-1'),
    ('Naveed Moderator 02','demo.moderator02@communex.test','MODERATOR','moderator-2'),
    ('Meena Organizer 01','demo.organizer01@communex.test','ORGANIZER','organizer-1'),
    ('Ali Organizer 02','demo.organizer02@communex.test','ORGANIZER','organizer-2'),
    ('Farah Responder 01','demo.responder01@communex.test','RESPONDER','responder-1'),
    ('Kiran Responder 02','demo.responder02@communex.test','RESPONDER','responder-2'),
    ('Hina Facility 01','demo.facility01@communex.test','FACILITY_MANAGER','facility-1'),
    ('Prem Facility 02','demo.facility02@communex.test','FACILITY_MANAGER','facility-2'),
    ('Jaya Orgadmin 01','demo.orgadmin01@communex.test','ORGANIZATION_ADMIN','orgadmin-1'),
    ('Imran Orgadmin 02','demo.orgadmin02@communex.test','ORGANIZATION_ADMIN','orgadmin-2')
), volunteers AS (
    SELECT (ARRAY['Amina','Ramesh','Sana','Naveed','Meena','Ali','Farah','Kiran',
                  'Hina','Prem','Jaya','Imran','Saira','Dev','Lila','Bhavesh']::text[])[((g.n-1)%16)+1]
             || ' Volunteer ' || lpad(g.n::text,2,'0') AS name,
           'demo.volunteer' || lpad(g.n::text,2,'0') || '@communex.test' AS email,
           'VOLUNTEER'::text AS role,
           'volunteer-' || g.n::text AS avatar_seed
    FROM generate_series(1,50) AS g(n)
), residents AS (
    SELECT (ARRAY['Asha','Vikram','Nisha','Arjun','Maya','Karan','Rekha','Sohail',
                  'Rupal','Sanjay','Nazia','Dinesh','Sonal','Manish','Kavita','Ravi']::text[])[((g.n-1)%16)+1]
             || ' Resident ' || lpad(g.n::text,2,'0') AS name,
           'demo.resident' || lpad(g.n::text,2,'0') || '@communex.test' AS email,
           'USER'::text AS role,
           'resident-' || g.n::text AS avatar_seed
    FROM generate_series(1,50) AS g(n)
), all_accounts AS (
    SELECT name,email,role,avatar_seed FROM special_accounts
    UNION ALL SELECT name,email,role,avatar_seed FROM volunteers
    UNION ALL SELECT name,email,role,avatar_seed FROM residents
)
SELECT name,email,role,avatar_seed,
       -- BCrypt hash generated for the stated demo password (verified before writing this file).
       '$2a$10$jTKo9d3vRoWR1IG/TFuuE.MQCm0F6OhLq5KS3RKPUlQ7S6fXzE9AK'
FROM all_accounts;

INSERT INTO users(name,email,password_hash,role,provider,provider_id,profile_image_url,email_verified,created_at,updated_at)
SELECT a.name,a.email,a.password_hash,a.role,'LOCAL',NULL,
       'https://api.dicebear.com/9.x/initials/svg?seed=' || a.avatar_seed,
       TRUE,CURRENT_TIMESTAMP - INTERVAL '14 days',CURRENT_TIMESTAMP
FROM communex_demo_seed_accounts a
ON CONFLICT(email) DO NOTHING;

-- Volunteer profiles (50).
INSERT INTO volunteers(user_id,skills,bio,available,created_at)
SELECT u.id,
       'Community outreach, event support, local information review',
       '[DEMO] Synthetic volunteer profile. Skills and availability are illustrative only.',
       TRUE,CURRENT_TIMESTAMP
FROM users u
JOIN communex_demo_seed_accounts a ON a.email = u.email
WHERE a.role = 'VOLUNTEER'
ON CONFLICT(user_id) DO NOTHING;

-- Temporary approximate demo volunteer pins. These are not live GPS locations.
-- Map/API code must still enforce freshness (e.g. updated_at within the last six hours).
WITH ranked_volunteers AS (
    SELECT u.id,u.email,row_number() OVER (ORDER BY u.email)::int AS rn
    FROM users u
    JOIN communex_demo_seed_accounts a ON a.email = u.email
    WHERE a.role = 'VOLUNTEER'
)
INSERT INTO user_live_locations(user_id,latitude,longitude,accuracy,sharing,updated_at)
SELECT rv.id,
       (ARRAY[24.7401,24.6998,24.4673,25.1161,24.3579,24.7700,24.9000,24.9768,24.8200,24.5868])[((rv.rn-1)%10)+1]
         + ((rv.rn%7)-3)*0.001,
       (ARRAY[69.8004,70.1800,69.5823,70.2518,70.7551,69.5700,70.3500,69.9215,69.9300,70.6810])[((rv.rn-1)%10)+1]
         + ((rv.rn%7)-3)*0.001,
       250.0,TRUE,CURRENT_TIMESTAMP
FROM ranked_volunteers rv
ON CONFLICT(user_id) DO UPDATE
SET latitude=EXCLUDED.latitude,
    longitude=EXCLUDED.longitude,
    accuracy=EXCLUDED.accuracy,
    sharing=TRUE,
    updated_at=CURRENT_TIMESTAMP;

-- One clearly-marked sample community post per seeded account, plus synthetic reactions/comments.
DO $$
DECLARE
    u RECORD;
    p_id BIGINT;
    n INT := 0;
    j INT;
    other_user BIGINT;
    post_text TEXT;
    demo_user_count INT;
BEGIN
    SELECT count(*) INTO demo_user_count
    FROM users u2 JOIN communex_demo_seed_accounts a2 ON a2.email=u2.email;

    FOR u IN
        SELECT u1.id,u1.name,u1.email,u1.role
        FROM users u1 JOIN communex_demo_seed_accounts a1 ON a1.email=u1.email
        ORDER BY u1.email
    LOOP
        n := n + 1;
        post_text := '[DEMO] ' || CASE (n % 10)
          WHEN 0 THEN 'Mithi community note: I am collecting public-source links for local service listings; please include the date and locality.'
          WHEN 1 THEN 'Tharparkar learning circle: what low-bandwidth study resource has helped you practise a new skill this week?'
          WHEN 2 THEN 'Volunteer coordination idea: let us label sample supply counts clearly until a coordinator confirms them.'
          WHEN 3 THEN 'Community calendar suggestion: include an accessible venue, a clear start time, and a contact route in every event post.'
          WHEN 4 THEN 'Map data reminder: a town-centre marker is approximate and should not be treated as an exact building entrance.'
          WHEN 5 THEN 'Local services discussion: please add public source links rather than sharing private phone numbers.'
          WHEN 6 THEN 'School support circle: reusable stationery and reading materials can be catalogued by quantity and update date.'
          WHEN 7 THEN 'Community clean-up planning: a checklist can help volunteers record what is needed without claiming stock is confirmed.'
          WHEN 8 THEN 'Digital access thought: short posts in simple Urdu, Sindhi, and English can make public information easier to use.'
          ELSE 'Neighbourhood question: what is the best way to keep community notices accurate, respectful, and up to date?'
        END || ' (sample entry ' || lpad(n::text,3,'0') || ' by ' || u.name || ').';

        INSERT INTO community_posts(content,author_id,hidden,created_at,updated_at)
        SELECT post_text,u.id,FALSE,CURRENT_TIMESTAMP-(n%21)*INTERVAL '1 day',CURRENT_TIMESTAMP
        WHERE NOT EXISTS (
            SELECT 1 FROM community_posts p WHERE p.author_id=u.id AND p.content=post_text
        );

        SELECT p.id INTO p_id
        FROM community_posts p
        WHERE p.author_id=u.id AND p.content=post_text
        ORDER BY p.id DESC LIMIT 1;

        -- Attempt a varied 5..80 reactions per post; duplicate likes are ignored.
        FOR j IN 1..(5 + ((n*17) % 76)) LOOP
            SELECT u2.id INTO other_user
            FROM users u2 JOIN communex_demo_seed_accounts a2 ON a2.email=u2.email
            ORDER BY u2.email OFFSET ((n*7+j*11) % GREATEST(demo_user_count,1)) LIMIT 1;
            IF other_user IS NOT NULL AND other_user <> u.id AND p_id IS NOT NULL THEN
                INSERT INTO community_likes(post_id,user_id,created_at)
                VALUES(p_id,other_user,CURRENT_TIMESTAMP-((j+n)%18)*INTERVAL '1 day')
                ON CONFLICT(post_id,user_id) DO NOTHING;
            END IF;
        END LOOP;

        -- Attempt varied sample comments. The unique marker makes reruns idempotent.
        FOR j IN 1..(2 + ((n*13) % 58)) LOOP
            SELECT u2.id INTO other_user
            FROM users u2 JOIN communex_demo_seed_accounts a2 ON a2.email=u2.email
            ORDER BY u2.email OFFSET ((n*3+j*5) % GREATEST(demo_user_count,1)) LIMIT 1;
            IF other_user IS NOT NULL AND other_user <> u.id AND p_id IS NOT NULL THEN
                INSERT INTO community_comments(post_id,author_id,content,created_at)
                SELECT p_id,other_user,
                       '[DEMO] Thanks for sharing, ' || u.name || '. A useful next step is to add a source/date and keep the details clearly marked as sample information ('||j||').',
                       CURRENT_TIMESTAMP-((j+n)%17)*INTERVAL '1 day'
                WHERE NOT EXISTS (
                    SELECT 1 FROM community_comments c
                    WHERE c.post_id=p_id AND c.author_id=other_user
                      AND c.content LIKE '%sample information ('||j||').'
                );
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- Shared demo group; seeded accounts join and messages remain marked as samples.
INSERT INTO community_groups(name,description,visibility,owner_id,created_at)
SELECT '[DEMO] Tharparkar Community Connect',
       '[DEMO] Synthetic group for testing local information sharing, volunteering, and event coordination.',
       'PUBLIC',u.id,CURRENT_TIMESTAMP-INTERVAL '14 days'
FROM users u
JOIN communex_demo_seed_accounts a ON a.email=u.email
WHERE a.email='demo.admin01@communex.test'
AND NOT EXISTS (SELECT 1 FROM community_groups g WHERE g.name='[DEMO] Tharparkar Community Connect');

INSERT INTO group_memberships(group_id,user_id,role,status,joined_at,created_at)
SELECT g.id,u.id,
       CASE WHEN a.role='ADMIN' THEN 'ADMIN' WHEN a.role='MODERATOR' THEN 'MODERATOR' ELSE 'MEMBER' END,
       'APPROVED',CURRENT_TIMESTAMP-INTERVAL '10 days',CURRENT_TIMESTAMP-INTERVAL '10 days'
FROM community_groups g
CROSS JOIN users u
JOIN communex_demo_seed_accounts a ON a.email=u.email
WHERE g.name='[DEMO] Tharparkar Community Connect'
ON CONFLICT(group_id,user_id) DO NOTHING;

DO $$
DECLARE i INT; uid BIGINT; gid BIGINT; demo_user_count INT;
BEGIN
    SELECT id INTO gid FROM community_groups WHERE name='[DEMO] Tharparkar Community Connect' ORDER BY id LIMIT 1;
    SELECT count(*) INTO demo_user_count FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email;
    FOR i IN 1..80 LOOP
        SELECT u.id INTO uid
        FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email
        ORDER BY u.email OFFSET ((i*7)%GREATEST(demo_user_count,1)) LIMIT 1;
        INSERT INTO group_messages(group_id,sender_id,content,created_at)
        SELECT gid,uid,
          '[DEMO] '||CASE (i%6)
            WHEN 0 THEN 'Please add a source link and date before a place is marked verified.'
            WHEN 1 THEN 'I can help review the public-service list for my assigned taluka.'
            WHEN 2 THEN 'Let us keep sample event details separate from real public announcements.'
            WHEN 3 THEN 'The map pin is approximate, so please confirm the exact venue before travel.'
            WHEN 4 THEN 'Could someone share a simple checklist for welcoming new volunteers?'
            ELSE 'I updated the sample inventory notes; these quantities are not real stock.'
          END||' (message '||i||').',
          CURRENT_TIMESTAMP-(80-i)*INTERVAL '1 hour'
        WHERE gid IS NOT NULL AND uid IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM group_messages m WHERE m.group_id=gid AND m.content LIKE '%(message '||i||').');
    END LOOP;
END $$;

-- 25 announcements with traceable seeded creator accounts.
DO $$
DECLARE
    i INT;
    uid BIGINT;
    titles TEXT[] := ARRAY[
      'Community directory review','Volunteer orientation reminder','Digital literacy resource share',
      'Map accuracy check','School supply catalogue demo','Local event planning note',
      'Public information translation','Community centre listing review','Low-bandwidth access tip',
      'Water information source review','Neighbourhood clean-up planning','Library resource exchange',
      'Small business data-quality check','Community safety information review','Accessible event checklist',
      'Public transport information review','Food-support directory review','Youth learning circle',
      'Volunteer coordination update','Service category review','Inventory demo update','Community group welcome',
      'Poll participation guide','Public-source citation reminder','Monthly data-quality recap'
    ];
BEGIN
    FOR i IN 1..25 LOOP
        SELECT u.id INTO uid
        FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email
        ORDER BY u.email OFFSET ((i*9)%112) LIMIT 1;
        INSERT INTO community_announcements(title,body,category,published,created_by,created_at,updated_at)
        SELECT '[DEMO] '||titles[i],
               '[DEMO] Synthetic announcement for local testing only. This is not an official notice or real emergency information. Please use public sources and label approximate data honestly.',
               'COMMUNITY',TRUE,uid,CURRENT_TIMESTAMP-(i%14)*INTERVAL '1 day',CURRENT_TIMESTAMP
        WHERE uid IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM community_announcements a WHERE a.title='[DEMO] '||titles[i]);
    END LOOP;
END $$;

-- 12 synthetic polls with sample votes from seeded accounts.
DO $$
DECLARE i INT; j INT; pid BIGINT; uid BIGINT; oid BIGINT; moderator_id BIGINT; q TEXT; demo_user_count INT;
BEGIN
    SELECT u.id INTO moderator_id
    FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email
    WHERE a.email='demo.moderator01@communex.test';
    SELECT count(*) INTO demo_user_count FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email;
    FOR i IN 1..12 LOOP
        q := '[DEMO] '||CASE (i%6)
          WHEN 0 THEN 'Which time works best for a fictional study circle?'
          WHEN 1 THEN 'Which public-data category should be reviewed next?'
          WHEN 2 THEN 'Which language format would help with sample notices?'
          WHEN 3 THEN 'Which volunteer orientation topic should come first?'
          WHEN 4 THEN 'Which map marker detail needs clearer explanation?'
          ELSE 'Which low-bandwidth learning resource should be demonstrated?'
        END||' ('||i||')';
        INSERT INTO community_polls(question,created_by,closed,created_at)
        SELECT q,moderator_id,FALSE,CURRENT_TIMESTAMP-(i%10)*INTERVAL '1 day'
        WHERE moderator_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM community_polls p WHERE p.question=q);
        SELECT p.id INTO pid FROM community_polls p WHERE p.question=q ORDER BY p.id DESC LIMIT 1;

        FOR j IN 1..4 LOOP
            INSERT INTO poll_options(poll_id,text)
            SELECT pid,(ARRAY['Option A — morning','Option B — afternoon','Option C — evening','Option D — weekend'])[j]
            WHERE pid IS NOT NULL
              AND NOT EXISTS (
                SELECT 1 FROM poll_options po
                WHERE po.poll_id=pid
                  AND po.text=(ARRAY['Option A — morning','Option B — afternoon','Option C — evening','Option D — weekend'])[j]
              );
        END LOOP;
        FOR j IN 1..40 LOOP
            SELECT u.id INTO uid
            FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email
            ORDER BY u.email OFFSET ((i*5+j*3)%GREATEST(demo_user_count,1)) LIMIT 1;
            SELECT po.id INTO oid FROM poll_options po WHERE po.poll_id=pid ORDER BY po.id OFFSET ((j+i)%4) LIMIT 1;
            IF uid IS NOT NULL AND oid IS NOT NULL THEN
                INSERT INTO poll_votes(poll_id,option_id,user_id,created_at)
                VALUES(pid,oid,uid,CURRENT_TIMESTAMP-(j%8)*INTERVAL '1 day')
                ON CONFLICT DO NOTHING;
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- 20 fictional events dated 20-26 November 2026; capacity is at most 100.
DO $$
DECLARE i INT; j INT; uid BIGINT; eid BIGINT; town TEXT; lat DOUBLE PRECISION; lon DOUBLE PRECISION; demo_user_count INT;
BEGIN
    SELECT count(*) INTO demo_user_count FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email;
    FOR i IN 1..20 LOOP
        town := (ARRAY['Mithi','Islamkot','Diplo','Chachro','Nagarparkar','Kaloi','Dahli'])[((i-1)%7)+1];
        lat := (ARRAY[24.7401,24.6998,24.4673,25.1161,24.3579,24.7700,24.9000])[((i-1)%7)+1];
        lon := (ARRAY[69.8004,70.1800,69.5823,70.2518,70.7551,69.5700,70.3500])[((i-1)%7)+1];
        SELECT u.id INTO uid
        FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email
        WHERE a.role='ORGANIZER' ORDER BY u.email OFFSET (i%2) LIMIT 1;
        INSERT INTO community_events(title,description,location,starts_at,capacity,organizer_id,status,created_at,updated_at)
        SELECT '[DEMO] Tharparkar Community Session '||lpad(i::text,2,'0'),
               '[DEMO] Fictional event for testing only; not a real public event. Confirm real details before sharing.',
               town||', Tharparkar District, Sindh',
               TIMESTAMP '2026-11-20 09:00:00'+((i-1)%7)*INTERVAL '1 day'+(i%4)*INTERVAL '2 hours',
               CASE WHEN i%4=0 THEN 100 ELSE 25+(i*7)%66 END,
               uid,'PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
        WHERE uid IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM community_events e WHERE e.title='[DEMO] Tharparkar Community Session '||lpad(i::text,2,'0'));
        SELECT e.id INTO eid FROM community_events e WHERE e.title='[DEMO] Tharparkar Community Session '||lpad(i::text,2,'0') ORDER BY e.id DESC LIMIT 1;
        INSERT INTO map_locations(entity_type,entity_id,latitude,longitude,updated_at)
        SELECT 'events',eid,lat,lon,CURRENT_TIMESTAMP WHERE eid IS NOT NULL
        ON CONFLICT(entity_type,entity_id) DO UPDATE
        SET latitude=EXCLUDED.latitude,longitude=EXCLUDED.longitude,updated_at=CURRENT_TIMESTAMP;
        FOR j IN 1..LEAST(10,5+(i%6)) LOOP
            SELECT u.id INTO uid
            FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email
            ORDER BY u.email OFFSET ((i*7+j*3)%GREATEST(demo_user_count,1)) LIMIT 1;
            INSERT INTO event_registrations(event_id,user_id,created_at)
            SELECT eid,uid,CURRENT_TIMESTAMP-(j%5)*INTERVAL '1 day'
            WHERE eid IS NOT NULL AND uid IS NOT NULL
            ON CONFLICT(event_id,user_id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- 100 fictional help requests with town-centre demo pins; not real requests for aid.
DO $$
DECLARE i INT; uid BIGINT; vid BIGINT; hid BIGINT; town TEXT; lat DOUBLE PRECISION; lon DOUBLE PRECISION; demo_user_count INT;
BEGIN
    SELECT count(*) INTO demo_user_count FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email;
    FOR i IN 1..100 LOOP
        town := (ARRAY['Mithi','Islamkot','Diplo','Chachro','Nagarparkar','Kaloi','Dahli'])[((i-1)%7)+1];
        lat := (ARRAY[24.7401,24.6998,24.4673,25.1161,24.3579,24.7700,24.9000])[((i-1)%7)+1];
        lon := (ARRAY[69.8004,70.1800,69.5823,70.2518,70.7551,69.5700,70.3500])[((i-1)%7)+1];
        SELECT u.id INTO uid
        FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email
        WHERE a.role='USER' ORDER BY u.email OFFSET ((i-1)%50) LIMIT 1;
        SELECT u.id INTO vid
        FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email
        WHERE a.role='VOLUNTEER' ORDER BY u.email OFFSET ((i*3)%50) LIMIT 1;
        INSERT INTO help_requests(title,description,category,location,status,created_by,assigned_volunteer_id,priority,created_at)
        SELECT '[DEMO] Sample community coordination request '||lpad(i::text,3,'0'),
               '[DEMO] Fictional request for workflow testing only. Not a real emergency or request for aid.',
               (ARRAY['COMMUNITY','EDUCATION','WATER','TRANSPORT','ACCESSIBILITY'])[((i-1)%5)+1],
               town||', Tharparkar District',CASE WHEN i%4=0 THEN 'ACKNOWLEDGED' ELSE 'OPEN' END,
               uid,CASE WHEN i%3=0 THEN vid ELSE NULL END,
               CASE WHEN i%10=0 THEN 'HIGH' ELSE 'NORMAL' END,
               CURRENT_TIMESTAMP-(i%14)*INTERVAL '1 day'
        WHERE uid IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM help_requests h WHERE h.title='[DEMO] Sample community coordination request '||lpad(i::text,3,'0'));
        SELECT h.id INTO hid FROM help_requests h WHERE h.title='[DEMO] Sample community coordination request '||lpad(i::text,3,'0') ORDER BY h.id DESC LIMIT 1;
        INSERT INTO map_locations(entity_type,entity_id,latitude,longitude,updated_at)
        SELECT 'help-requests',hid,lat,lon,CURRENT_TIMESTAMP WHERE hid IS NOT NULL
        ON CONFLICT(entity_type,entity_id) DO UPDATE
        SET latitude=EXCLUDED.latitude,longitude=EXCLUDED.longitude,updated_at=CURRENT_TIMESTAMP;
    END LOOP;
END $$;

-- 520 fictional inventory records; counts are illustrations, not actual physical stock.
DO $$
DECLARE i INT; rid BIGINT; uid BIGINT; town TEXT;
BEGIN
    SELECT u.id INTO uid FROM users u JOIN communex_demo_seed_accounts a ON a.email=u.email
    WHERE a.email='demo.admin01@communex.test' LIMIT 1;
    FOR i IN 1..520 LOOP
        town := (ARRAY['Mithi','Islamkot','Diplo','Chachro','Nagarparkar','Kaloi','Dahli'])[((i-1)%7)+1];
        INSERT INTO resource_items(name,category,description,location,quantity,unit,low_stock_threshold,active,created_at,updated_at)
        SELECT '[DEMO] Inventory item '||lpad(i::text,3,'0'),
               (ARRAY['EDUCATION','WATER','COMMUNITY','EVENTS','VOLUNTEERING','ACCESSIBILITY','GENERAL'])[((i-1)%7)+1],
               '[DEMO] Illustrative catalogue entry only. No physical stock is implied.',
               town||', Tharparkar District',((i*13)%120)::numeric,
               CASE WHEN i%3=0 THEN 'packs' WHEN i%3=1 THEN 'items' ELSE 'units' END,
               10,TRUE,CURRENT_TIMESTAMP-(i%20)*INTERVAL '1 day',CURRENT_TIMESTAMP
        WHERE NOT EXISTS (SELECT 1 FROM resource_items r WHERE r.name='[DEMO] Inventory item '||lpad(i::text,3,'0'));
        SELECT r.id INTO rid FROM resource_items r WHERE r.name='[DEMO] Inventory item '||lpad(i::text,3,'0') ORDER BY r.id DESC LIMIT 1;
        IF rid IS NOT NULL AND uid IS NOT NULL
           AND NOT EXISTS (SELECT 1 FROM inventory_transactions t WHERE t.resource_id=rid AND t.reason='[DEMO] Initial sample inventory entry') THEN
            INSERT INTO inventory_transactions(resource_id,updated_by,delta,quantity_after,reason,created_at)
            SELECT rid,uid,r.quantity,r.quantity,'[DEMO] Initial sample inventory entry',CURRENT_TIMESTAMP
            FROM resource_items r WHERE r.id=rid;
        END IF;
        INSERT INTO map_locations(entity_type,entity_id,latitude,longitude,updated_at)
        SELECT 'resources',rid,
               (ARRAY[24.7401,24.6998,24.4673,25.1161,24.3579,24.7700,24.9000])[((i-1)%7)+1],
               (ARRAY[69.8004,70.1800,69.5823,70.2518,70.7551,69.5700,70.3500])[((i-1)%7)+1],
               CURRENT_TIMESTAMP
        WHERE rid IS NOT NULL
        ON CONFLICT(entity_type,entity_id) DO NOTHING;
    END LOOP;
END $$;

COMMIT;
