#!/bin/bash

# Configuration
DB_NAME="crime_db"
DB_USER="postgres"
DB_PASSWORD=""
DB_HOST="localhost"
DB_PORT="5432"

# Migration directory
MIGRATION_DIR="migration"

echo "Applying SQL migrations to database $DB_NAME..."

# Array of migration files in order
MIGRATION_FILES=(
    "V2.1__create_stored_procedures.sql"
    "V2.2__create_system_overview_procedure.sql"
    "V3.0__add_missing_procedures.sql"
    "V3.1__fix_officer_performance_report.sql"
    "V3.2__fix_all_stored_procedures.sql"
)

# Loop through and apply each migration file
for FILE in "${MIGRATION_FILES[@]}"; do
    FILE_PATH="$MIGRATION_DIR/$FILE"
    if [ -f "$FILE_PATH" ]; then
        echo "Applying migration $FILE..."
        PGPASSWORD="$DB_PASSWORD" psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$FILE_PATH"
        if [ $? -eq 0 ]; then
            echo "Successfully applied $FILE"
        else
            echo "Error applying $FILE. Exiting..."
            exit 1
        fi
    else
        echo "Migration file $FILE_PATH not found. Exiting..."
        exit 1
    fi
done

echo "All migrations completed successfully."