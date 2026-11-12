-- an unknown column type, so the migration fails on the database
CREATE TABLE mig_broken (id NOT_A_TYPE);
