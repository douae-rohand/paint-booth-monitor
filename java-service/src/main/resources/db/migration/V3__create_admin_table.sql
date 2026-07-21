-- Héritage par table jointe : un Admin EST un Superviseur
CREATE TABLE admin (
    id_admin UUID PRIMARY KEY REFERENCES superviseur(id_superviseur) ON DELETE CASCADE
);