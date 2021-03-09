需求 
開發(編譯)環境要有 Java8，要設定 JAVA_HOME 
開發(編譯)環境要安裝 maven，要設定 MAVEN_HOME 並添加到 PATH 
開發(編譯)環境要能聯外網，否則需要的第三方庫要事先安裝到 MAVEN 的 .m2 目錄 
 
 
Maven 下載 
官網 https://maven.apache.org/download.cgi 
可以下 mvn --version 檢查 
 
 
編譯、運行 
在專案目錄中下 mvn clean package 
java -jar APISample-1.0.0.jar 運行 
