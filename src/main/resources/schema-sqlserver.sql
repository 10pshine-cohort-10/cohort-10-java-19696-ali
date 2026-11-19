IF OBJECT_ID('accounts', 'U') IS NULL
CREATE TABLE accounts (
  id BIGINT IDENTITY PRIMARY KEY, display_name NVARCHAR(100) NOT NULL,
  login_id VARCHAR(254) NOT NULL UNIQUE, password_hash VARCHAR(200) NOT NULL,
  credential_version BIGINT NOT NULL
);
IF OBJECT_ID('contacts', 'U') IS NULL
CREATE TABLE contacts (
  id BIGINT IDENTITY PRIMARY KEY, owner_id BIGINT NOT NULL REFERENCES accounts(id),
  first_name NVARCHAR(80) NOT NULL, last_name NVARCHAR(80) NOT NULL,
  title NVARCHAR(100) NOT NULL
);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='ix_contacts_owner_name' AND object_id=OBJECT_ID('contacts'))
CREATE INDEX ix_contacts_owner_name ON contacts(owner_id, last_name, first_name, id);
IF OBJECT_ID('contact_emails', 'U') IS NULL
CREATE TABLE contact_emails (
  contact_id BIGINT NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
  position INT NOT NULL, label NVARCHAR(30) NOT NULL, entry_value NVARCHAR(254) NOT NULL,
  PRIMARY KEY(contact_id, position)
);
IF OBJECT_ID('contact_phones', 'U') IS NULL
CREATE TABLE contact_phones (
  contact_id BIGINT NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
  position INT NOT NULL, label NVARCHAR(30) NOT NULL, entry_value NVARCHAR(254) NOT NULL,
  PRIMARY KEY(contact_id, position)
);
