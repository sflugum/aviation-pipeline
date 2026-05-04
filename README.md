# Aviation Data Pipeline

A portfolio project building an end-to-end ingestion and transformation pipeline for private aviation operations data. Actively in development — Phase I is complete and Phase II is underway.

Built to demonstrate data engineering fundamentals: normalized schema design, containerized infrastructure, cloud storage integration, and a clear separation between raw, integrated, and analytical data layers.

---

## Current status

**Phase I: in progress.** MySQL and PostgreSQL will be containerized via Docker Compose. Initial DDL scripts will define a 3NF schema across fleet, flight, and maintenance domains.

**Phase II: up next.** Python extraction scripts and AWS S3 bronze layer integration.

---

## Architecture

Raw source data lives in MySQL, simulating a transactional operations database. A PostgreSQL integration layer holds a normalized 3NF schema — chosen to enforce referential integrity and eliminate redundancy before data moves downstream for analytics.

The design follows a Medallion pattern:

```
MySQL (source) → S3 landing zone (bronze) → PostgreSQL 3NF (silver) → Star schema (gold)
```

Phase III will introduce dbt for silver-to-gold transformations and a columnar warehouse (Snowflake or DuckDB) for BI reporting.

---

## Tech stack

| Layer | Tool |
| --- | --- |
| Source DB | MySQL (Docker) |
| Integration DB | PostgreSQL (Docker) |
| Orchestration | Docker Compose |
| Cloud storage | AWS S3 |
| Extraction | Python |
| Transformation (Phase III) | dbt |
| Analytical warehouse (Phase III) | DuckDB |

---

## Quick start

Upcoming

---

## Repo structure

Upcoming

---

## Roadmap

- [ ] Phase I — Docker infrastructure, MySQL + PostgreSQL, 3NF schema DDL
- [ ] Phase II — Python extraction scripts, AWS S3 bronze layer
- [ ] Phase III — dbt transformations, star schema, Airflow orchestration

---

## Notes

This project is a deliberate learning exercise in data engineering fundamentals. The aviation domain was chosen to work with realistic operational data patterns: high-frequency telemetry, maintenance event logs, and fleet scheduling; all of which stress-test schema design and pipeline reliability in ways generic datasets don't.

Feedback welcome via issues or PRs.
