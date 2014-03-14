package subtitleProject.transportStream;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;
import subtitleProject.common.ResourceLinks;
import subtitleProject.common.StreamInfo;

/**
 * Extended StreamInfo class to handle additional TransportStream info 
 */
public class TransportStreamInfo extends StreamInfo{
	
	private static Logger log = LoggerFactory.getLogger(TransportStreamInfo.class);

	private String programNo;
	private String serviceName;
	private List<String> subtitleStreams;

	private TransportStreamInfo(String programNo, String service_name, String videoStreamInfo, List<String>subStreams) {
		super(videoStreamInfo);
		this.programNo = programNo;
		this.serviceName = service_name;
		this.subtitleStreams = subStreams;
	}

	public String getProgramNo() {
		return programNo;
	}
	public void setProgramNo(String programNo) {
		this.programNo = programNo;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public void addSubtitleStream(String subtitleStream) {
		subtitleStreams.add(subtitleStream);
	}
	public List<String> getSubtitleStreams() {
		return subtitleStreams;
	}

	@Override
	public String toString() {
		String dvdStreamInfo = "contains no subtitleStream";
		if(!subtitleStreams.isEmpty()){
			dvdStreamInfo = "";
			for(String s: subtitleStreams){
				dvdStreamInfo += s+"\n";
			}
		}
		return "StreamInfo: ProgramNo = " + programNo + "\n serviceName = "
		+ serviceName + "\n videoStreamDetails = " + super.getVideoStreamDetails()
		+ "dvbSubStream = " + dvdStreamInfo+"\n";
	}

	/**
	 * Uses ffProbe to analyze the transportStream
	 * @param tsPath to TransportStream
	 * @param properties
	 * @return an list with StreamInfo instances
	 */
	public static List<TransportStreamInfo> analyze(String tsPath, ResourceLinks resources){
		String commandline = resources.getFfprobe()+" "+tsPath;
		log.debug("Running commandline: {}",commandline);
		ProcessRunner pr = new ProcessRunner("bash","-c",commandline);
		pr.run();
		//	String StringOutput = pr.getProcessOutputAsString();
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
		List<TransportStreamInfo> tsData = new ArrayList<TransportStreamInfo>();
		String service_name = "";
		String videoStreamInfo = "";
		String programNo = "";
		for(int i = 0; i<outPut.length; i++){
			//When program no is identified, additional stream info is required
			if(outPut[i].toLowerCase().contains("program")){
				programNo = outPut[i].trim();
				
				//service name only in mux tiles, therefore service name is based on videofile name in nonMux files
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
						//list of streams in row
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
						//if no video in stream = move to next program in output
						videoStreamInfo = "No Video Stream\n";
						foundFrameSize = true;
					}
					else{
						i++;
					}
				}
	
				//search for dvd_subtitle stream, if non is found program will appear next on list
				firstStreamFound = false;
				List<String> subStreams = new ArrayList<String>();
				while(!firstStreamFound && i<outPut.length){
					if(outPut[i].toLowerCase().contains("dvb_subtitle")){
						subStreams.add(outPut[i]);	
						i++;
					}
					else if(outPut[i].toLowerCase().contains("program")){
						//Next program is found and i is now to high for next loop, therefore i--
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
