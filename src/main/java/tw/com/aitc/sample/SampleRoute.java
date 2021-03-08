package tw.com.aitc.sample;

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class SampleRoute extends RouteBuilder {

	public void configure() throws Exception {
		prepareDB();

		restConfiguration()
				.component("undertow")
				.host("0.0.0.0")        // 開放連入 IP
				.port("{{SAMPLE_PORT}}") // 開放端口
				.bindingMode(RestBindingMode.off)
		;

		// API
		rest("/sample")
				// 打 /sample/{???}，??? 的值會存到 Header 的 action 欄位
				.post("/{action}")
				.produces("application/json")
				.to("direct:sqlAction") // 用 sqlAction 流程繼續處理

		// 打 /other/xxxx
//				.post("/other/xxxx")
//				.produces("application/json")
//				.to("direct:doXXXX")
		;

		// 用 timer 定義一段用來檢查 DB 是否連上的流程，只重複執行一次
		from("timer:checkDB?repeatCount=1")
				.to("sql:SELECT LOCALTIMESTAMP(2)")
				.log(LoggingLevel.INFO, "${body}")
		;

		// 定義一段處理交易的流程 sqlAction
		from("direct:sqlAction")
				.routeId("sqlAction")
				.log(LoggingLevel.INFO, "Receive:\n${body}")

				// 用 JsonPath 解析電文，存放到 Header 欄位
				.setHeader("ID", jsonpath("id"))
				.setHeader("NAME", jsonpath("info.name"))

				// SQL，參考 https://camel.apache.org/components/2.x/sql-component.html
				// Simeple，參考 https://camel.apache.org/components/2.x/languages/simple-language.html
				.setHeader("SQL", simple("insert into sample_table VALUES(':#ID', ':#NAME')"))
				.toD("sql:${header.SQL}")

				// 類似 if-else
				.choice()
				// 如果 action 為 insert
				.when(simple("${header.action} ~~ 'insert'"))
				.log(LoggingLevel.INFO, "Insert ${header.CamelSqlUpdateCount} record")
				.setBody(simple("{\"result\": \"Insert ${header.CamelSqlUpdateCount} record\"}"))

				// 其他狀況
				.otherwise()
				.log(LoggingLevel.ERROR, "Unknown Action")
				.setBody(constant("{\"result\": \"Unknown Action\"}"))
				.end()
		;
	}

	private void prepareDB() throws Exception {
		CamelContext context = getContext();
		Map<String, Object> registryMap = new HashMap();

		// 從設定檔讀出 DB 相關資料
		DataSource dataSource = context.getRegistry().lookupByNameAndType("dataSource", DataSource.class);
		if (dataSource == null) {
			BasicDataSource dbcp2 = new BasicDataSource();
			dbcp2.setDriverClassName(context.resolvePropertyPlaceholders("{{DB_DRIVER_CLASS_NAME}}"));
			dbcp2.setUrl(context.resolvePropertyPlaceholders("{{DB_URL}}"));
			dbcp2.setUsername(context.resolvePropertyPlaceholders("{{DB_USER_NAME}}"));
			dbcp2.setPassword(context.resolvePropertyPlaceholders("{{DB_PASSWORD}}"));
			dbcp2.setDefaultSchema(context.resolvePropertyPlaceholders("{{DB_DEFAULT_SCHEMA}}"));
			dbcp2.setMaxTotal(Integer.parseInt(context.resolvePropertyPlaceholders("{{DB_MAX_TOTAL}}")));
			dbcp2.setMaxIdle(Integer.parseInt(context.resolvePropertyPlaceholders("{{DB_MAX_IDLE}}")));
			dbcp2.setInitialSize(Integer.parseInt(context.resolvePropertyPlaceholders("{{DB_INITIAL_SIZE}}")));
			dataSource = dbcp2;
			registryMap.put("dataSource", dbcp2);
		}

		SqlComponent sqlComponent = new SqlComponent();
		sqlComponent.setDataSource(dataSource);
		registryMap.put("sql", sqlComponent);

		DataSourceTransactionManager txnManager = new DataSourceTransactionManager();
		txnManager.setDataSource(dataSource);
		registryMap.put("txnManager", txnManager);

		SpringTransactionPolicy required = new SpringTransactionPolicy();
		required.setTransactionManager(txnManager);
		required.setPropagationBehaviorName("PROPAGATION_REQUIRED");
		registryMap.put("PROPAGATION_REQUIRED", required);

		SpringTransactionPolicy requiresNew = new SpringTransactionPolicy();
		requiresNew.setTransactionManager(txnManager);
		requiresNew.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
		registryMap.put("PROPAGATION_REQUIRES_NEW", requiresNew);

		RegistryUtils.addAllRegistry(context, registryMap);
	}
}
