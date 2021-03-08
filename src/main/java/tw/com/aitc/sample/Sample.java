package tw.com.aitc.sample;

import org.apache.camel.main.Main;

public class Sample {
	public static void main(String[] args) throws Exception {
		Main main = new Main();
		// 綁定設定檔
		main.setPropertyPlaceholderLocations("sample.properties");
		// 添加 Route 設定  >> 會去執行 configure()
		main.addRouteBuilder(new SampleRoute());

		main.run();
	}
}
