INSERT INTO gap_dev_log (version) VALUES ('V2') ON CONFLICT (version) DO NOTHING;
