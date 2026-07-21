-- V21__add_port_to_configuration_plc.sql
ALTER TABLE configuration_plc ADD COLUMN plc_port INTEGER NOT NULL DEFAULT 102;