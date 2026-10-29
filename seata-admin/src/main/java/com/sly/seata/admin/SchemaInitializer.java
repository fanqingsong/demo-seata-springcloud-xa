package com.sly.seata.admin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the Seata 0.6.1 session tables before the application datasource opens.
 * Finished sessions are archived by triggers, because Seata deletes them after commit or rollback.
 */
public final class SchemaInitializer {

	private static final String[] STATEMENTS = new String[] {
			"CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8",
			"CREATE TABLE IF NOT EXISTS seata.global_table ("
					+ "xid varchar(128) NOT NULL,"
					+ "transaction_id bigint(20) DEFAULT NULL,"
					+ "status tinyint(4) NOT NULL,"
					+ "application_id varchar(128) DEFAULT NULL,"
					+ "transaction_service_group varchar(64) DEFAULT NULL,"
					+ "transaction_name varchar(128) DEFAULT NULL,"
					+ "timeout int(11) DEFAULT NULL,"
					+ "begin_time bigint(20) DEFAULT NULL,"
					+ "application_data varchar(2000) DEFAULT NULL,"
					+ "gmt_create datetime DEFAULT NULL,"
					+ "gmt_modified datetime DEFAULT NULL,"
					+ "PRIMARY KEY (xid),"
					+ "KEY idx_gmt_modified_status (gmt_modified, status),"
					+ "KEY idx_transaction_id (transaction_id)"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8",
			"CREATE TABLE IF NOT EXISTS seata.branch_table ("
					+ "branch_id bigint(20) NOT NULL,"
					+ "xid varchar(128) NOT NULL,"
					+ "transaction_id bigint(20) DEFAULT NULL,"
					+ "resource_group_id varchar(64) DEFAULT NULL,"
					+ "resource_id varchar(256) DEFAULT NULL,"
					+ "lock_key varchar(2000) DEFAULT NULL,"
					+ "branch_type varchar(16) DEFAULT NULL,"
					+ "status tinyint(4) DEFAULT NULL,"
					+ "client_id varchar(128) DEFAULT NULL,"
					+ "application_data varchar(2000) DEFAULT NULL,"
					+ "gmt_create datetime DEFAULT NULL,"
					+ "gmt_modified datetime DEFAULT NULL,"
					+ "PRIMARY KEY (branch_id),"
					+ "KEY idx_xid (xid)"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8",
			"CREATE TABLE IF NOT EXISTS seata.lock_table ("
					+ "row_key varchar(128) NOT NULL,"
					+ "xid varchar(128) DEFAULT NULL,"
					+ "transaction_id bigint(20) DEFAULT NULL,"
					+ "branch_id bigint(20) NOT NULL,"
					+ "resource_id varchar(256) DEFAULT NULL,"
					+ "table_name varchar(64) DEFAULT NULL,"
					+ "pk varchar(128) DEFAULT NULL,"
					+ "gmt_create datetime DEFAULT NULL,"
					+ "gmt_modified datetime DEFAULT NULL,"
					+ "PRIMARY KEY (row_key),"
					+ "KEY idx_branch_id (branch_id)"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8",
			"CREATE TABLE IF NOT EXISTS seata.global_table_history ("
					+ "xid varchar(128) NOT NULL,"
					+ "transaction_id bigint(20) DEFAULT NULL,"
					+ "status tinyint(4) NOT NULL,"
					+ "application_id varchar(128) DEFAULT NULL,"
					+ "transaction_service_group varchar(64) DEFAULT NULL,"
					+ "transaction_name varchar(128) DEFAULT NULL,"
					+ "timeout int(11) DEFAULT NULL,"
					+ "begin_time bigint(20) DEFAULT NULL,"
					+ "application_data varchar(2000) DEFAULT NULL,"
					+ "gmt_create datetime DEFAULT NULL,"
					+ "gmt_modified datetime DEFAULT NULL,"
					+ "archived_at datetime NOT NULL,"
					+ "PRIMARY KEY (xid)"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8",
			"CREATE TABLE IF NOT EXISTS seata.branch_table_history ("
					+ "branch_id bigint(20) NOT NULL,"
					+ "xid varchar(128) NOT NULL,"
					+ "transaction_id bigint(20) DEFAULT NULL,"
					+ "resource_group_id varchar(64) DEFAULT NULL,"
					+ "resource_id varchar(256) DEFAULT NULL,"
					+ "lock_key varchar(2000) DEFAULT NULL,"
					+ "branch_type varchar(16) DEFAULT NULL,"
					+ "status tinyint(4) DEFAULT NULL,"
					+ "client_id varchar(128) DEFAULT NULL,"
					+ "application_data varchar(2000) DEFAULT NULL,"
					+ "gmt_create datetime DEFAULT NULL,"
					+ "gmt_modified datetime DEFAULT NULL,"
					+ "archived_at datetime NOT NULL,"
					+ "PRIMARY KEY (branch_id),"
					+ "KEY idx_xid (xid)"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8",
			"DROP TRIGGER IF EXISTS seata.trg_global_archive",
			"CREATE TRIGGER seata.trg_global_archive BEFORE DELETE ON seata.global_table FOR EACH ROW "
					+ "INSERT INTO seata.global_table_history ("
					+ "xid, transaction_id, status, application_id, transaction_service_group, transaction_name, "
					+ "timeout, begin_time, application_data, gmt_create, gmt_modified, archived_at) "
					+ "VALUES (OLD.xid, OLD.transaction_id, OLD.status, OLD.application_id, OLD.transaction_service_group, "
					+ "OLD.transaction_name, OLD.timeout, OLD.begin_time, OLD.application_data, OLD.gmt_create, "
					+ "OLD.gmt_modified, NOW()) "
					+ "ON DUPLICATE KEY UPDATE status = VALUES(status), gmt_modified = VALUES(gmt_modified), "
					+ "archived_at = VALUES(archived_at)",
			"DROP TRIGGER IF EXISTS seata.trg_branch_archive",
			"CREATE TRIGGER seata.trg_branch_archive BEFORE DELETE ON seata.branch_table FOR EACH ROW "
					+ "INSERT INTO seata.branch_table_history ("
					+ "branch_id, xid, transaction_id, resource_group_id, resource_id, lock_key, branch_type, status, "
					+ "client_id, application_data, gmt_create, gmt_modified, archived_at) "
					+ "VALUES (OLD.branch_id, OLD.xid, OLD.transaction_id, OLD.resource_group_id, OLD.resource_id, "
					+ "OLD.lock_key, OLD.branch_type, OLD.status, OLD.client_id, OLD.application_data, OLD.gmt_create, "
					+ "OLD.gmt_modified, NOW()) "
					+ "ON DUPLICATE KEY UPDATE status = VALUES(status), gmt_modified = VALUES(gmt_modified), "
					+ "archived_at = VALUES(archived_at)"
	};

	private SchemaInitializer() {
	}

	public static void prepare() throws Exception {
		String host = env("MYSQL_HOST", "mysql");
		String user = env("MYSQL_USERNAME", "root");
		String password = env("MYSQL_PASSWORD", "123456");
		String url = "jdbc:mysql://" + host + ":3306/?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai";
		Class.forName("com.mysql.cj.jdbc.Driver");
		SQLException last = null;
		for (int i = 0; i < 30; i++) {
			try (Connection connection = DriverManager.getConnection(url, user, password);
					Statement statement = connection.createStatement()) {
				for (String sql : STATEMENTS) {
					statement.execute(sql);
				}
				return;
			} catch (SQLException ex) {
				last = ex;
				Thread.sleep(2000L);
			}
		}
		throw last;
	}

	private static String env(String key, String fallback) {
		String value = System.getenv(key);
		return value == null || value.isEmpty() ? fallback : value;
	}
}
