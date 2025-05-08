#!/bin/bash

# Configuration
DB_NAME="crime_db"
DB_USER="postgres"
DB_PASSWORD="netshwlan"
DB_HOST="localhost"
DB_PORT="5432"

echo "Applying SQL migrations to database $DB_NAME..."

# Apply the V3.0 migration (missing stored procedures)
echo "Applying V3.0 migration (missing stored procedures)..."
PGPASSWORD="$DB_PASSWORD" psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "V3.0__add_missing_procedures.sql"

echo "Migration completed." 