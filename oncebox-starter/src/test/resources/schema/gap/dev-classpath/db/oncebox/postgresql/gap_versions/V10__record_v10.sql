INSERT INTO gap_dev_log (version) VALUES ('V10') ON CONFLICT (version) DO NOTHING;
