-- System Overview Report Statistics
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
         WHERE date_filed <= end_date) AS total_complaints,
         
        (SELECT COUNT(*) FROM complaints 
         WHERE status IN ('SUBMITTED', 'ASSIGNED', 'INVESTIGATING', 'PENDING_EVIDENCE', 'UNDER_REVIEW') 
         AND date_filed <= end_date) AS open_complaints,
         
        (SELECT COUNT(*) FROM complaints 
         WHERE status IN ('RESOLVED', 'REJECTED', 'CLOSED') 
         AND date_filed <= end_date) AS closed_complaints,
         
        (SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (date_last_updated - date_filed))/86400), 0) 
         FROM complaints 
         WHERE status = 'RESOLVED' 
         AND date_filed BETWEEN start_date AND end_date) AS avg_resolution_days,
         
        -- Officer and department stats
        (SELECT COUNT(*) FROM police_officers) AS total_officers,
        
        (SELECT COUNT(*) FROM departments) AS total_departments,
        
        -- User stats
        (SELECT COUNT(*) FROM users) AS total_users,
        
        (SELECT COUNT(*) FROM users 
         WHERE created_at BETWEEN start_date AND end_date) AS new_users_period,
         
        -- High priority complaints
        (SELECT COUNT(*) FROM complaints 
         WHERE priority_score >= 7 
         AND date_filed BETWEEN start_date AND end_date) AS high_priority_complaints;
END;
$$; 