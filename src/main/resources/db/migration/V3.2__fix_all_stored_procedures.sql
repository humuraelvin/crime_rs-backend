-- Drop all stored procedures to ensure a clean slate
DROP FUNCTION IF EXISTS get_complaint_report(TIMESTAMP, TIMESTAMP, VARCHAR, VARCHAR);
DROP FUNCTION IF EXISTS get_officer_performance_report(BIGINT, TIMESTAMP, TIMESTAMP);
DROP FUNCTION IF EXISTS get_user_activity_report(TIMESTAMP, TIMESTAMP);
DROP FUNCTION IF EXISTS get_complaint_stats_by_date_range(TIMESTAMP, TIMESTAMP);
DROP FUNCTION IF EXISTS get_complaint_stats_by_crime_type();
DROP FUNCTION IF EXISTS get_system_overview_stats(TIMESTAMP, TIMESTAMP);

-- Complaint Statistics by Date Range
CREATE OR REPLACE FUNCTION get_complaint_stats_by_date_range(
    start_date TIMESTAMP,
    end_date TIMESTAMP
)
RETURNS TABLE (
    date DATE,
    total_count BIGINT,
    resolved_count BIGINT,
    pending_count BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    WITH date_series AS (
        SELECT generate_series(
            date_trunc('day', start_date::TIMESTAMP),
            date_trunc('day', end_date::TIMESTAMP),
            '1 day'::INTERVAL
        )::DATE AS series_date
    ),
    counts AS (
        SELECT 
            date_trunc('day', date_filed)::DATE AS complaint_date,
            COUNT(*) AS total,
            COUNT(*) FILTER (WHERE status = 'RESOLVED') AS resolved,
            COUNT(*) FILTER (WHERE status IN ('SUBMITTED', 'ASSIGNED', 'INVESTIGATING', 'PENDING_EVIDENCE', 'UNDER_REVIEW')) AS pending
        FROM complaints
        WHERE date_filed BETWEEN start_date AND end_date
        GROUP BY date_trunc('day', date_filed)::DATE
    )
    SELECT 
        ds.series_date AS date,
        COALESCE(c.total, 0) AS total_count,
        COALESCE(c.resolved, 0) AS resolved_count,
        COALESCE(c.pending, 0) AS pending_count
    FROM date_series ds
    LEFT JOIN counts c ON ds.series_date = c.complaint_date
    ORDER BY ds.series_date;
END;
$$;

-- Complaint Statistics by Crime Type
CREATE OR REPLACE FUNCTION get_complaint_stats_by_crime_type()
RETURNS TABLE (
    crime_type VARCHAR,
    count BIGINT,
    percentage NUMERIC
)
LANGUAGE plpgsql
AS $$
DECLARE
    total_complaints BIGINT;
BEGIN
    -- Get total count
    SELECT COUNT(*) INTO total_complaints FROM complaints;
    
    -- Return crime type stats with percentages
    RETURN QUERY
    SELECT 
        c.crime_type::VARCHAR,
        COUNT(*) AS count,
        CASE 
            WHEN total_complaints > 0 THEN 
                ROUND((COUNT(*) * 100.0 / total_complaints), 2)
            ELSE 0.0
        END AS percentage
    FROM complaints c
    GROUP BY c.crime_type
    ORDER BY count DESC;
END;
$$;

-- User Activity Report
CREATE OR REPLACE FUNCTION get_user_activity_report(
    start_date TIMESTAMP,
    end_date TIMESTAMP
)
RETURNS TABLE (
    user_id BIGINT,
    username VARCHAR,
    email VARCHAR,
    role VARCHAR,
    registration_date TIMESTAMP,
    complaints_filed BIGINT,
    last_login TIMESTAMP
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        u.id AS user_id,
        CONCAT(u.first_name, ' ', u.last_name) AS username,
        u.email,
        u.role::VARCHAR,
        u.created_at AS registration_date,
        COUNT(c.id) AS complaints_filed,
        MAX(u.updated_at) AS last_login
    FROM users u
    LEFT JOIN complaints c ON u.id = c.user_id AND 
                           c.date_filed BETWEEN COALESCE(start_date, '1900-01-01'::TIMESTAMP) 
                                            AND COALESCE(end_date, NOW())
    WHERE u.created_at <= COALESCE(end_date, NOW())
    GROUP BY u.id, u.first_name, u.last_name, u.email, u.role, u.created_at
    ORDER BY complaints_filed DESC;
END;
$$;

-- Complaint Report
CREATE OR REPLACE FUNCTION get_complaint_report(
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    status_filter VARCHAR,
    crime_type_filter VARCHAR
)
RETURNS TABLE (
    complaint_id BIGINT,
    crime_type VARCHAR,
    description TEXT,
    status VARCHAR,
    date_filed TIMESTAMP,
    date_updated TIMESTAMP,
    user_id BIGINT,
    user_name VARCHAR,
    officer_id BIGINT,
    officer_name VARCHAR,
    department_name VARCHAR,
    location VARCHAR,
    priority_score INTEGER,
    evidence_count BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.id AS complaint_id,
        c.crime_type::VARCHAR,
        c.description,
        c.status::VARCHAR,
        c.date_filed,
        c.date_last_updated AS date_updated,
        c.user_id,
        CONCAT(u.first_name, ' ', u.last_name) AS user_name,
        po.id AS officer_id,
        CONCAT(po_user.first_name, ' ', po_user.last_name) AS officer_name,
        d.name AS department_name,
        c.location,
        c.priority_score,
        COUNT(e.id) AS evidence_count
    FROM complaints c
    JOIN users u ON c.user_id = u.id
    LEFT JOIN police_officers po ON c.assigned_officer_id = po.id
    LEFT JOIN users po_user ON po.user_id = po_user.id
    LEFT JOIN departments d ON po.department_id = d.id
    LEFT JOIN evidences e ON c.id = e.complaint_id
    WHERE 
        (start_date IS NULL OR c.date_filed >= start_date) AND
        (end_date IS NULL OR c.date_filed <= end_date) AND
        (status_filter IS NULL OR c.status::VARCHAR = status_filter) AND
        (crime_type_filter IS NULL OR c.crime_type::VARCHAR = crime_type_filter)
    GROUP BY 
        c.id, c.crime_type, c.description, c.status, c.date_filed, c.date_last_updated,
        c.user_id, u.first_name, u.last_name, po.id, po_user.first_name, po_user.last_name,
        d.name, c.location, c.priority_score
    ORDER BY c.date_filed DESC;
END;
$$;

-- Officer Performance Report with renamed parameter to inp_department_id
CREATE OR REPLACE FUNCTION get_officer_performance_report(
    inp_department_id BIGINT,
    start_date TIMESTAMP,
    end_date TIMESTAMP
)
RETURNS TABLE (
    officer_id BIGINT,
    officer_name VARCHAR,
    badge_number VARCHAR,
    department_name VARCHAR,
    assigned_count BIGINT,
    closed_count BIGINT,
    pending_count BIGINT,
    avg_resolution_days NUMERIC
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        po.id AS officer_id,
        CONCAT(u.first_name, ' ', u.last_name) AS officer_name,
        po.badge_number,
        d.name AS department_name,
        COUNT(c.id) AS assigned_count,
        COUNT(c.id) FILTER (WHERE c.status = 'RESOLVED') AS closed_count,
        COUNT(c.id) FILTER (WHERE c.status IN ('ASSIGNED', 'INVESTIGATING', 'PENDING_EVIDENCE')) AS pending_count,
        COALESCE(AVG(EXTRACT(EPOCH FROM (c.date_last_updated - c.date_filed))/86400) FILTER (WHERE c.status = 'RESOLVED'), 0) AS avg_resolution_days
    FROM police_officers po
    JOIN users u ON po.user_id = u.id
    JOIN departments d ON po.department_id = d.id
    LEFT JOIN complaints c ON po.id = c.assigned_officer_id AND 
                           c.date_filed BETWEEN COALESCE(start_date, '1900-01-01'::TIMESTAMP) 
                                            AND COALESCE(end_date, NOW())
    WHERE 
        (inp_department_id IS NULL OR po.department_id = inp_department_id)
    GROUP BY po.id, u.first_name, u.last_name, po.badge_number, d.name
    ORDER BY closed_count DESC;
END;
$$;

-- System Overview Stats
CREATE OR REPLACE FUNCTION get_system_overview_stats(
    start_date TIMESTAMP,
    end_date TIMESTAMP
)
RETURNS TABLE (
    total_complaints BIGINT,
    open_complaints BIGINT,
    closed_complaints BIGINT,
    avg_resolution_days NUMERIC,
    total_officers BIGINT,
    total_departments BIGINT,
    total_users BIGINT,
    new_users_period BIGINT,
    high_priority_complaints BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        -- Complaint stats
        (SELECT COUNT(*) FROM complaints 
         WHERE date_filed <= COALESCE(end_date, NOW())) AS total_complaints,
         
        (SELECT COUNT(*) FROM complaints 
         WHERE status IN ('SUBMITTED', 'ASSIGNED', 'INVESTIGATING', 'PENDING_EVIDENCE', 'UNDER_REVIEW') 
         AND date_filed <= COALESCE(end_date, NOW())) AS open_complaints,
         
        (SELECT COUNT(*) FROM complaints 
         WHERE status IN ('RESOLVED', 'REJECTED', 'CLOSED') 
         AND date_filed <= COALESCE(end_date, NOW())) AS closed_complaints,
         
        (SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (date_last_updated - date_filed))/86400), 0) 
         FROM complaints 
         WHERE status = 'RESOLVED' 
         AND date_filed BETWEEN COALESCE(start_date, '1900-01-01'::TIMESTAMP) AND COALESCE(end_date, NOW())) AS avg_resolution_days,
         
        -- Officer and department stats
        (SELECT COUNT(*) FROM police_officers) AS total_officers,
        
        (SELECT COUNT(*) FROM departments) AS total_departments,
        
        -- User stats
        (SELECT COUNT(*) FROM users) AS total_users,
        
        (SELECT COUNT(*) FROM users 
         WHERE created_at BETWEEN COALESCE(start_date, '1900-01-01'::TIMESTAMP) AND COALESCE(end_date, NOW())) AS new_users_period,
         
        -- High priority complaints
        (SELECT COUNT(*) FROM complaints 
         WHERE priority_score >= 7 
         AND date_filed BETWEEN COALESCE(start_date, '1900-01-01'::TIMESTAMP) AND COALESCE(end_date, NOW())) AS high_priority_complaints;
END;
$$; 