#!/bin/bash

# Configuration from application.properties
DB_NAME="sec-api"
DB_USER="postgres"
export PGPASSWORD="postgres"

echo "Wiping database: $DB_NAME..."

# Drop tables and recreate them (handled by Hibernate update/create on restart)
psql -h localhost -U $DB_USER -d $DB_NAME -c "DROP TABLE IF EXISTS quarters, listings, assets CASCADE;"

echo "Database wiped successfully."
