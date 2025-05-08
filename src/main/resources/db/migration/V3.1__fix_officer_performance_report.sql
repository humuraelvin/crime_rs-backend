-- Fix Officer Performance Report procedure ambiguous column reference
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
                            c.date_filed BETWEEN start_date AND end_date
    WHERE 
        (department_id IS NULL OR po.department_id = department_id)
    GROUP BY po.id, u.first_name, u.last_name, po.badge_number, d.name
    ORDER BY closed_count DESC;
END;
$$;

-- Fix System Overview Stats procedure
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