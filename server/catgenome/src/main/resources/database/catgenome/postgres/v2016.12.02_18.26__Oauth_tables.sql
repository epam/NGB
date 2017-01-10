CREATE TABLE IF NOT EXISTS oauth_client_details (
    client_id VARCHAR(256) PRIMARY KEY,
    resource_ids VARCHAR(256),
    client_secret VARCHAR(256),
    scope VARCHAR(256),
    authorized_grant_types VARCHAR(256),
    web_server_redirect_uri VARCHAR(256),
    authorities VARCHAR(256),
    access_token_validity INTEGER,
    refresh_token_validity INTEGER,
    additional_information VARCHAR(4096),
    autoapprove VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS oauth_client_token (
    token_id VARCHAR(256),
    token bytea,
    authentication_id VARCHAR(256),
    user_name VARCHAR(256),
    client_id VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS oauth_access_token (
    token_id VARCHAR(256),
    token bytea,
    authentication_id VARCHAR(256),
    user_name VARCHAR(256),
    client_id VARCHAR(256),
    authentication bytea,
    refresh_token VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS oauth_refresh_token (
    token_id VARCHAR(256),
    token bytea,
    authentication bytea
);

CREATE TABLE IF NOT EXISTS oauth_code (
    code VARCHAR(256), authentication bytea
);