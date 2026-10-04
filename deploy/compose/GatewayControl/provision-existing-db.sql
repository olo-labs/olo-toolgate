-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
-- Run once with administrator psql. Edit identifiers for custom database/users.
-- Do not run on a database whose roles are already provisioned.
\set ON_ERROR_STOP on
CREATE ROLE toolgate_control_runtime NOLOGIN;
CREATE ROLE control_migrator LOGIN;
CREATE ROLE control_app LOGIN IN ROLE toolgate_control_runtime;
\password control_migrator
\password control_app
CREATE DATABASE control OWNER control_migrator;
