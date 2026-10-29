package com.sly.seata.admin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AdminController {

	private static final Map<Integer, String> GLOBAL_STATUS = new LinkedHashMap<Integer, String>();
	private static final Map<Integer, String> BRANCH_STATUS = new LinkedHashMap<Integer, String>();

	static {
		GLOBAL_STATUS.put(0, "未知");
		GLOBAL_STATUS.put(1, "已开始");
		GLOBAL_STATUS.put(2, "提交中");
		GLOBAL_STATUS.put(3, "提交重试");
		GLOBAL_STATUS.put(4, "回滚中");
		GLOBAL_STATUS.put(5, "回滚重试");
		GLOBAL_STATUS.put(6, "超时回滚中");
		GLOBAL_STATUS.put(7, "超时回滚重试");
		GLOBAL_STATUS.put(8, "异步提交中");
		GLOBAL_STATUS.put(9, "已提交");
		GLOBAL_STATUS.put(10, "提交失败");
		GLOBAL_STATUS.put(11, "已回滚");
		GLOBAL_STATUS.put(12, "回滚失败");
		GLOBAL_STATUS.put(13, "超时已回滚");
		GLOBAL_STATUS.put(14, "超时回滚失败");
		GLOBAL_STATUS.put(15, "已完成");

		BRANCH_STATUS.put(0, "未知");
		BRANCH_STATUS.put(1, "已注册");
		BRANCH_STATUS.put(2, "一阶段完成");
		BRANCH_STATUS.put(3, "一阶段失败");
		BRANCH_STATUS.put(4, "一阶段超时");
		BRANCH_STATUS.put(5, "二阶段已提交");
		BRANCH_STATUS.put(6, "二阶段提交失败可重试");
		BRANCH_STATUS.put(7, "二阶段提交失败不可重试");
		BRANCH_STATUS.put(8, "二阶段已回滚");
		BRANCH_STATUS.put(9, "二阶段回滚失败可重试");
		BRANCH_STATUS.put(10, "二阶段回滚失败不可重试");
	}

	private final JdbcTemplate jdbcTemplate;

	public AdminController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/")
	public String index() {
		return "index";
	}

	@GetMapping("/api/sessions")
	@ResponseBody
	public Map<String, Object> sessions() {
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("activeGlobals", withStatus(jdbcTemplate.queryForList(
				"SELECT xid, transaction_id, status, application_id, transaction_service_group, transaction_name, timeout, begin_time, gmt_create, gmt_modified FROM global_table ORDER BY gmt_create DESC LIMIT 50"),
				GLOBAL_STATUS));
		body.put("activeBranches", withStatus(jdbcTemplate.queryForList(
				"SELECT branch_id, xid, transaction_id, resource_id, branch_type, status, client_id, gmt_create, gmt_modified FROM branch_table ORDER BY gmt_create DESC LIMIT 100"),
				BRANCH_STATUS));
		body.put("historyGlobals", withStatus(jdbcTemplate.queryForList(
				"SELECT xid, transaction_id, status, application_id, transaction_service_group, transaction_name, timeout, begin_time, gmt_create, gmt_modified, archived_at FROM global_table_history ORDER BY archived_at DESC LIMIT 50"),
				GLOBAL_STATUS));
		body.put("historyBranches", withStatus(jdbcTemplate.queryForList(
				"SELECT branch_id, xid, transaction_id, resource_id, branch_type, status, client_id, gmt_create, gmt_modified, archived_at FROM branch_table_history ORDER BY archived_at DESC LIMIT 100"),
				BRANCH_STATUS));
		return body;
	}

	private static List<Map<String, Object>> withStatus(List<Map<String, Object>> rows, Map<Integer, String> names) {
		for (Map<String, Object> row : rows) {
			Object status = row.get("status");
			int code = status == null ? -1 : ((Number) status).intValue();
			String name = names.get(code);
			row.put("statusName", name == null ? String.valueOf(status) : name);
		}
		return rows;
	}
}
