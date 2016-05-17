package splunk.source.connector;


import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.kafka.common.requests.OffsetCommitRequest;

import com.splunk.Application;
import com.splunk.HttpService;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobResultsArgs;
import com.splunk.JobResultsPreviewArgs;
import com.splunk.ResultsReaderJson;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;

@SuppressWarnings("restriction")
public class SplunkTest {
	public static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";
	
	
	public static void main(String[] args) throws Exception {
		// Log in using a basic authorization header
        // Note: Do not call the Service.login method
		HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
		
        Service service = new Service("localhost", 8089);
        String credentials = "admin:abcde12345";
        String basicAuthHeader = Base64.encode(credentials.getBytes());
        service.setToken("Basic " + basicAuthHeader);
		
        // Print the session token
        System.out.println("Your session token: " + service.getToken());

        // Print installed apps to the console to verify login
//        for (Application app : service.getApplications().values()) {
//            System.out.println(app.getName());
//        }
        
       /* JobArgs jobArgs = new JobArgs();
        jobArgs.setExecutionMode(JobArgs.ExecutionMode.NORMAL);
        jobArgs.setSearchMode(JobArgs.SearchMode.NORMAL);
        //jobArgs.setEarliestTime("rt-30");
        //jobArgs.setTimeFormat("mm/dd/yy hh:mm:ss");
        //jobArgs.setEarliestTime("05/03/16 07:33:01");
        jobArgs.setEarliestTime("2016-05-14T19:08:17.154-07:00");
        //jobArgs.setEarliestTime("2016-05-14T");
        //jobArgs.setLatestTime("rt");
        //jobArgs.setLatestTime("5/3/16 7:35:01");
        //jobArgs.setLatestTime("2016-05-03T07:49:28.668-07:00");
        jobArgs.setLatestTime("now");
        jobArgs.setStatusBuckets(300);*/
        
        /**
         * Came up with a plan, say, if you don't have any offset value
         * stored, then just do realtime search
         * , if offset is stored, then convert it into normal search.
         */
        // https://cwiki.apache.org/confluence/display/KAFKA/Committing+and+fetching+consumer+offsets+in+Kafka
        
        // So, I found out,it is possible to use zulu format for offsets.
        String startOffset = "2016-05-17T05:13:53.867 UTC";
        JobArgs jobArgs = new JobArgs();
       // jobArgs.setTimeFormat("MM:DD:YYYY HH:MM:SSS");
        //strftime()
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

       /**
        * These options work
        * rt-30, rt
        * but setting an earliest time in time format and latest time to rt, doesn't work
        * 2016-05-03T07:47:28.668-07:00, now works.
        */
        
        String mySearch = /*"search dependencies";*/"search index=_internal";
        Job job = service.search(mySearch, jobArgs);
        
        while (!job.isReady()) {
        	Thread.sleep(500);
        }
        
        JobResultsPreviewArgs previewArgs = new JobResultsPreviewArgs();
        previewArgs.setCount(300);     // Retrieve 300 previews at a time
        
        /*// Display results
        while (true) {
        	InputStream results = job.getResults();
            String line = null;
            System.out.println("Results from the search job as XML:\n");
            BufferedReader br = new BufferedReader(new InputStreamReader(results, "UTF-8"));
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
            System.out.println("Terminated");
        }*/
        
        // Use a continual loop 
        while (true) {
            InputStream stream = job.getResultsPreview(previewArgs);
            String line = null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, "UTF-8"));
            boolean isStarted = true;
            boolean isStopped = true;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.contains("<result")) {
                	isStarted = true;
                }
                if (line.contains("</result>")) {
                	isStopped = true;
                }
            }
            reader.close();
            stream.close();
            Thread.sleep(500);
        }
	}
}
