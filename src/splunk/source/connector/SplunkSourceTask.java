package splunk.source.connector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.requests.OffsetCommitRequest;
import org.apache.kafka.common.requests.OffsetCommitRequest.PartitionData;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.apache.zookeeper.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.splunk.HttpService;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobResultsPreviewArgs;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import static splunk.source.connector.SplunkSourceConnectorConfig.*;

public class SplunkSourceTask extends SourceTask {
	
	private static final Logger log = LoggerFactory.getLogger(SplunkSourceTask.class);
	private SplunkSourceTaskConfig config;
    private AtomicBoolean stop;
    private Job currentJob;
    private JobResultsPreviewArgs previewArgs;
    private final String OFFSET_KEY = "offset";
    private final String TOPIC_NAME = "SOMETOPICNAME";
    private String currentOffset = null;
    private final String FILENAME_FIELD = "filename";
    private final String POSITION_FIELD = "position";
    private final String FILENAME = "internal_search";
    private final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";

	@Override
	public String version() {
		return Version.getVersion();
	}

	@Override
	public List<SourceRecord> poll() throws InterruptedException {
	    log.trace("Polling for new data");
	    List<SourceRecord> results = new ArrayList<>();
	    Map<String, String> recordProperties = new HashMap<String, String>();
        while (true) {
        	try {

        		InputStream stream = currentJob.getResultsPreview(previewArgs);
                String line = null;
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream, "UTF-8"));
                String earlierKey = null;
                String value = null;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    
                    if (line.contains("<result")) {
                        recordProperties.clear();
                        String offsetStr = "<result offset='";
                        int sIndex = line.indexOf(offsetStr);
                        String offset = line.substring(sIndex + offsetStr.length(), line.length()-2);
                        recordProperties.put(OFFSET_KEY, offset);
                    }
                    if (line.contains("<field k=")) {

                    	int index = line.indexOf("<field k=") + "<field k=".length();
                    	earlierKey = line.substring(index+1, line.length()-2);
                    }
                    if (line.contains("<value>")) {
                    	String startStr = "<value><text>";
                    	String endStr = "</text></value>";
                    	int sIndex = line.indexOf(startStr) + startStr.length();
                    	int eIndex = line.indexOf(endStr);
                    	value = line.subSequence(sIndex, eIndex).toString();
                    	recordProperties.put(earlierKey, value);
                    }
                    if (line.contains("</result>")) {
                    	results.add(extractRecord(null, line, recordProperties));	
                    }
                }
                reader.close();
                stream.close();
                Thread.sleep(500);
        	} catch(Exception ex) {
        		System.out.println("Exception occurred : " + ex);
        	}
        }
	}

	private SourceRecord extractRecord(Schema schema, String line, Map<String, String> recordProperties) {
		Struct record = new Struct(schema);
		Map<String, String> sourcePartition = Collections.singletonMap(FILENAME_FIELD, FILENAME);
        Map<String, String> sourceOffset = Collections.singletonMap(POSITION_FIELD, recordProperties.get(OFFSET_KEY));
		return new SourceRecord(sourcePartition, sourceOffset, TOPIC_NAME, record.schema(), record);
	}

	@Override
	public void start(Map<String, String> properties) {
		try {
	        config = new SplunkSourceTaskConfig(properties);
		} catch (ConfigException e) {
		      throw new ConnectException("Couldn't start SplunkSourceTask due to configuration error", e);
	    }
		HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
        Service service = new Service(config.getString(HOST_NAME), config.getInt(PORT_NO_CONFIG));
        String credentials = "admin:abcde12345";
        String basicAuthHeader = Base64.encode(credentials.getBytes());
        service.setToken("Basic " + basicAuthHeader);
		
        // Print the session token
        System.out.println("Your session token: " + service.getToken());

        // if you don't have the offset, you can do it searching from real-time
        // else, you can do it by setting since.
        /*JobArgs jobArgs = new JobArgs();
        jobArgs.setExecutionMode(JobArgs.ExecutionMode.NORMAL);
        jobArgs.setSearchMode(JobArgs.SearchMode.REALTIME);
        
        jobArgs.setEarliestTime(config.getString(EARLIEST_TIME_CONFIG));
        jobArgs.setLatestTime(config.getString(LATEST_TIME_CONFIG));
        jobArgs.setStatusBuckets(config.getInt(STATUS_BUCKETS_CONFIG));*/
        
        String startOffset = readOffset();

        // this was the default, which was working
        /*JobArgs jobArgs = new JobArgs();
        jobArgs.setExecutionMode(JobArgs.ExecutionMode.NORMAL);
        jobArgs.setSearchMode(JobArgs.SearchMode.NORMAL);
        jobArgs.setEarliestTime("2016-05-03T07:47:28.668-07:00");
        jobArgs.setLatestTime("now");
        jobArgs.setStatusBuckets(300);*/
        JobArgs jobArgs = new JobArgs();
        if (startOffset != null) {
        	jobArgs.setExecutionMode(JobArgs.ExecutionMode.NORMAL);
            jobArgs.setSearchMode(JobArgs.SearchMode.NORMAL);
            //jobArgs.setEarliestTime("2016-05-14T19:08:17.154-07:00"); --works
            //jobArgs.setEarliestTime("1305010800000"); -- don't work
            jobArgs.setEarliestTime(startOffset);
            jobArgs.setLatestTime("now");
            jobArgs.setStatusBuckets(300);
        } else {
        	jobArgs.setExecutionMode(JobArgs.ExecutionMode.NORMAL);
            jobArgs.setSearchMode(JobArgs.SearchMode.REALTIME);
            jobArgs.setEarliestTime("rt-30");
            jobArgs.setLatestTime("rt");
            jobArgs.setTimeFormat(ISO_FORMAT);
            jobArgs.setStatusBuckets(300);
        }
        
        String mySearch = "search index=_internal";
        currentJob = service.search(mySearch, jobArgs);
        // try to read if any offset value is written from the file.
        // and if so start reading from that offset value.
        
        while (!currentJob.isReady()) {
        	try {
				Thread.sleep(500);
			} catch (InterruptedException ex) {
				log.error("Exception occurred while waiting for job to start: " + ex);
			}
        }
        previewArgs = new JobResultsPreviewArgs();
        previewArgs.setCount(300); // Retrieve 300 previews at a time
        
        stop = new AtomicBoolean(false);
	}

	private String readOffset() {
		OffsetStorageReader offsetReader = context.offsetStorageReader();
		Map<String, Object> offset = offsetReader.offset(Collections.singletonMap(FILENAME_FIELD, FILENAME));
		if (offset != null) {
			Long offsetValue = (Long) offset.get(POSITION_FIELD);
			return seekToOffset(offsetValue);
		}
		return null;
	}

	private String seekToOffset(Long offsetValue) {
		// now convert it to zulu time format
		//jobArgs.setEarliestTime("2016-05-14T19:08:17.154-07:00"); --works
		SimpleDateFormat isoFormatter = new SimpleDateFormat(ISO_FORMAT);
		String formattedOffset = isoFormatter.format(new Date(offsetValue));
		System.out.println("Seeking back from offset : " + formattedOffset);
		return formattedOffset;
	}

	@Override
	public void stop() {
		if (stop != null) {
			stop.set(true);
			writeOffset();
		}
	}

	private void writeOffset() {
		//public OffsetCommitRequest(String groupId, int generationId, String memberId, long retentionTime, Map<TopicPartition, PartitionData> offsetData) {
		/*Map<TopicPartition, PartitionData> offsetData = new HashMap<TopicPartition, PartitionData>();
		TopicPartition partition = new TopicPartition(TOPIC_NAME, 0);
		PartitionData partitionData = new PartitionData(currentOffset, "This is the time for slunk");
		OffsetCommitRequest request = new OffsetCommitRequest(groupId, generationId, memberId, DEFAULT_RETENTION_TIME);
		request.*/
	}
}
