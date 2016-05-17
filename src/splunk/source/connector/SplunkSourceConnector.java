package splunk.source.connector;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceConnector;
import org.apache.zookeeper.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SplunkSourceConnector extends SourceConnector {
	
	private static final Logger log = LoggerFactory.getLogger(SplunkSourceConnector.class);
	private Map<String, String> configProperties;
	private SplunkSourceConnectorConfig config;
	
	@Override
	public void start(Map<String, String> properties) {
		try {
			configProperties = properties;
			config = new SplunkSourceConnectorConfig(properties);
		} catch(ConfigException ex) {
			throw new ConnectException(" Couldn't start SplunksourceConnectorConfig "
					+ "due to error", ex);
		}
	}

	@Override
	public void stop() {
		log.info("Stopping splunk connector");
	}

	@Override
	public Class<? extends Task> taskClass() {
		return SplunkSourceTask.class;
	}

	@Override
	public List<Map<String, String>> taskConfigs(int arg0) {
	    return null;	
	}

	@Override
	public String version() {
		return Version.getVersion();
	}
}
