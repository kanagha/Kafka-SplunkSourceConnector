package splunk.source.connector;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

public class SplunkSourceTaskConfig extends SplunkSourceConnectorConfig {

	public static final String INDEX_NAME = "index";
	private static final String INDEX_NAME_DOC = "List of servers for this task to watch.";
    private static final String TOPIC_PREFIX_CONFIG = "TopicPrefix";
	
	  static ConfigDef config = baseConfigDef();
	     // .define(INDEX_NAME, Type.STRING, Importance.HIGH, INDEX_NAME_DOC);

	public SplunkSourceTaskConfig(Map<String, String> props) {
		super(props);
	}
}