-- Complaint Report procedure that was missing
CREATE OR REPLACE FUNCTION get_complaint_report(
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    status VARCHAR,
    crime_type VARCHAR
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
        (status IS NULL OR c.status::VARCHAR = status) AND
        (crime_type IS NULL OR c.crime_type::VARCHAR = crime_type)
    GROUP BY 
        c.id, c.crime_type, c.description, c.status, c.date_filed, c.date_last_updated,
        c.user_id, u.first_name, u.last_name, po.id, po_user.first_name, po_user.last_name,
        d.name, c.location, c.priority_score
    ORDER BY c.date_filed DESC;
END;
$$;

-- Officer Performance Report procedure that may also be missing
CREATE OR REPLACE FUNCTION get_officer_performance_report(
    department_id BIGINT,
    start_date TIMESTAMP,
    end_date TIMESTAMP
)
RETURNS TABLE (
    officer_id BIGINT,
    officer_name VARCHAR,
    badge_number VARCHAR,
    department_name VARCHAR,
    assigned_complaints BIGINT,
    resolved_complaints BIGINT,
    pending_complaints BIGINT,
    avg_resolution_days NUMERIC,
    avg_priority_score NUMERIC
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
        COUNT(c.id) AS assigned_complaints,
        COUNT(c.id) FILTER (WHERE c.status = 'RESOLVED') AS resolved_complaints,
        COUNT(c.id) FILTER (WHERE c.status IN ('ASSIGNED', 'INVESTIGATING', 'PENDING_EVIDENCE')) AS pending_complaints,
        COALESCE(AVG(EXTRACT(EPOCH FROM (c.date_last_updated - c.date_filed))/86400) FILTER (WHERE c.status = 'RESOLVED'), 0) AS avg_resolution_days,
        COALESCE(AVG(c.priority_score), 0) AS avg_priority_score
    FROM police_officers po
    JOIN users u ON po.user_id = u.id
    JOIN departments d ON po.department_id = d.id
    LEFT JOIN complaints c ON po.id = c.assigned_officer_id AND 
                            c.date_filed BETWEEN start_date AND end_date
    WHERE 
        (department_id IS NULL OR po.department_id = department_id)
    GROUP BY po.id, u.first_name, u.last_name, po.badge_number, d.name
    ORDER BY resolved_complaints DESC;
END;
$$;

-- User Activity Report procedure that may also be missing
CREATE OR REPLACE FUNCTION get_user_activity_report(
    start_date TIMESTAMP,
    end_date TIMESTAMP
)
RETURNS TABLE (
    user_id BIGINT,
    email VARCHAR,
    first_name VARCHAR,
    last_name VARCHAR,
    role VARCHAR,
    registration_date TIMESTAMP,
    complaints_filed BIGINT,
    last_login_date TIMESTAMP
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        u.id AS user_id,
        u.email,
        u.first_name,
        u.last_name,
        u.role::VARCHAR,
        u.created_at AS registration_date,
        COUNT(c.id) AS complaints_filed,
        MAX(u.updated_at) AS last_login_date
    FROM users u
    LEFT JOIN complaints c ON u.id = c.user_id AND c.date_filed BETWEEN start_date AND end_date
    WHERE u.created_at <= end_date
    GROUP BY u.id, u.email, u.first_name, u.last_name, u.role, u.created_at
    ORDER BY u.created_at DESC;
END;
$$; 