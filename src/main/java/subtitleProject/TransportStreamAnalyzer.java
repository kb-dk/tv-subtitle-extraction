package subtitleProject;

import java.io.File;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;

public class TransportStreamAnalyzer {
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);
	/**
	 * Uses ffProbe to analyze the transportStream
	 * @param tsPath
	 * @param properties
	 * @return an arraylist with StreamInfo instances
	 */
	public static ArrayList<TransportStreamInfo> analyze(String tsPath){
		log.debug("Running commandline: "+"var/ffprobe "+tsPath);
		ProcessRunner pr = new ProcessRunner("bash","-c","var/ffprobe "+tsPath);
		pr.run();
		//String StringOutput = pr.getProcessOutputAsString();
		String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);
		
		String[] outPut =StringError.split("\n");
		//Checks if working on mux-file.. based on filename
		File ts = new File(tsPath);
		boolean isMux =false;
		if(ts.getName().startsWith("mux")){
			isMux =true;
		}
		
		//iterate through ffprobe output, extracts programNo, service_name, videoStreamInfo and subStreams
		ArrayList<TransportStreamInfo> tsData = new ArrayList<TransportStreamInfo>();
		String service_name = "";
		String videoStreamInfo = "";
		String programNo = "";
		for(int i = 0; i<outPut.length; i++){
			if(outPut[i].toLowerCase().contains("program")){
				programNo = outPut[i].trim();
				if(!isMux){
					String[] name = ts.getName().split("_");
					service_name = name[0].trim();
				}
				else{
					boolean foundServiceName = false;
					while(!foundServiceName){
						if(outPut[i].toLowerCase().contains("service_name")){
							service_name = outPut[i].trim();
							foundServiceName = true;
						}
						else{
							i++;
						}
					}
				}
				
				boolean foundFrameSize = false;
				
				//bool to know when to break if no Video stream is found
				boolean firstStreamFound = false;
				
				while(!foundFrameSize){	
					if(outPut[i].toLowerCase().contains("stream")){
						firstStreamFound=true;
						if(outPut[i].toLowerCase().contains("video")){
							videoStreamInfo = outPut[i].trim()+"\n";
							foundFrameSize = true;
						}
						else{
							i++;
						}
					}
					else if(firstStreamFound){
						videoStreamInfo = "No Video Stream\n";
						foundFrameSize = true;
					}
					else{
						i++;
					}
				}
				
				firstStreamFound = false;
				ArrayList<String> subStreams = new ArrayList<String>();
				while(!firstStreamFound && i<outPut.length){
					if(outPut[i].toLowerCase().contains("dvb_subtitle")){
						subStreams.add(outPut[i]);	
						i++;
					}
					else if(outPut[i].toLowerCase().contains("program")){
						i--;
						firstStreamFound = true;
					}
					else{
						i++;
					}
				}
				
				tsData.add(new TransportStreamInfo(programNo, service_name,videoStreamInfo, subStreams));
			}
		}

		for(TransportStreamInfo t: tsData){
			log.debug(t.toString());
		}
		return tsData;
	}
}
