package splunk.source.connector;
import java.util.Map;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigException;

public class SplunkSourceConnectorConfig extends AbstractConfig {

	public static final String CONNECTOR_GROUP = "Connector";
	public static final String CONNECTION_URL_CONFIG = "connection.url";
    private static final String CONNECTION_URL_DISPLAY = "Connection Url";
    
    public static final String POLL_INTERVAL_MS_CONFIG = "poll.interval.ms";
    private static final String POLL_INTERVAL_MS_DOC = "Frequency in ms to poll for new data in "
                                                       + "each table.";
    public static final int POLL_INTERVAL_MS_DEFAULT = 5000;
    private static final String POLL_INTERVAL_MS_DISPLAY = "Poll Interval (ms)";
    
    public static final String MODE_CONFIG = "mode";
    
    public static final String EARLIEST_TIME_CONFIG = "etime";
    public static final String EARLIEST_TIME = "rt-30";
    public static final String LATEST_TIME_CONFIG = "ltime";
    public static final String LATEST_TIME = "rt";
    public static final String STATUS_BUCKETS_CONFIG = "statusbucket";
    public static final String STATUS_BUCKETS = "300";
    public static final String MODE_UNSPECIFIED = "";
    
    public static final String HOST_NAME_CONFIG ="hostname";
    public static final String HOST_NAME ="localhost";

    public static final String PORT_NO_CONFIG = "portno";
    public static final String PORT_NO = "8089";

    public static final String USER_NAME_CONFIG = "username";
    public static final String USER_NAME = "admin";

    public static final String PASSWORD_CONFIG = "passwd";
    public static final String PASSWORD = "abcde12345";
    public static final String DEFAULT_SEARCH_CONFIG = "search_index";
    public static final String DEFAULT_SEARCH = "search index=_internal";

	public SplunkSourceConnectorConfig(ConfigDef config, Map<?, ?> originals) {
		super(config, originals);
	}

	static ConfigDef config = baseConfigDef();

	public SplunkSourceConnectorConfig(Map<String, String> props) {
	    super(config, props);
	    String mode = getString(SplunkSourceConnectorConfig.MODE_CONFIG);
	    if (mode.equals(SplunkSourceConnectorConfig.MODE_UNSPECIFIED))
	      throw new ConfigException("Query mode must be specified");
	}

	public static ConfigDef baseConfigDef() {
		return new ConfigDef()
		    //.define(CONNECTION_URL_CONFIG, Type.STRING, Importance.HIGH, "add documentation")
		    .define(HOST_NAME_CONFIG, Type.STRING, HOST_NAME, Importance.HIGH, "add hostname doc")
		    .define(PORT_NO_CONFIG, Type.STRING, PORT_NO, Importance.HIGH, "add port no doc")
		    .define(USER_NAME_CONFIG, Type.STRING, USER_NAME, Importance.HIGH, "add username doc")
		    .define(PASSWORD_CONFIG, Type.STRING, PASSWORD, Importance.HIGH, "add password doc")
		    .define(DEFAULT_SEARCH_CONFIG, Type.STRING, DEFAULT_SEARCH, Importance.HIGH, "add search query doc")
		    .define(EARLIEST_TIME_CONFIG, Type.STRING, EARLIEST_TIME, Importance.HIGH, "add search query doc")
		    .define(LATEST_TIME_CONFIG, Type.STRING, LATEST_TIME, Importance.HIGH, "add search query doc")
		    .define(STATUS_BUCKETS_CONFIG, Type.STRING, STATUS_BUCKETS, Importance.HIGH, "add search query doc");
	}
}
