@echo off
REM Configuration
set DB_NAME=crime_db
set DB_USER=postgres
set DB_PASSWORD=netshwlan
set DB_HOST=localhost
set DB_PORT=5432

echo Applying SQL migrations to database %DB_NAME%...

REM Apply the V3.0 migration (missing stored procedures)
echo Applying V3.0 migration (missing stored procedures)...
set PGPASSWORD=%DB_PASSWORD%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "V3.0__add_missing_procedures.sql"

echo Migration completed. 