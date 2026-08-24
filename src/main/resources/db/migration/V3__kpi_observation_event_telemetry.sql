-- Phase 2: event identity and ingestion time on projected KPI observations.
-- observed_at remains event time (when the measurement occurred).

ALTER TABLE kpi_observation
    ADD COLUMN event_id VARCHAR(128),
    ADD COLUMN ingested_at TIMESTAMPTZ;

UPDATE kpi_observation
SET event_id = 'seed-' || id::text,
    ingested_at = observed_at
WHERE event_id IS NULL;

ALTER TABLE kpi_observation
    ALTER COLUMN event_id SET NOT NULL,
    ALTER COLUMN ingested_at SET NOT NULL;

ALTER TABLE kpi_observation
    ADD CONSTRAINT kpi_observation_event_id_uk UNIQUE (event_id);

CREATE INDEX kpi_observation_cell_metric_observed_idx
    ON kpi_observation (cell_id, metric, observed_at DESC);
