package subtitleProject.nonStreamed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;
import subtitleProject.common.ResourceLinks;
import subtitleProject.common.StreamInfo;
import subtitleProject.transportStream.TransportStreamInfo;

/**
 * Extended StreamInfo class to handle additional mpeg or wmv info 
 */
public class MpegWmvStreamInfo extends StreamInfo{

	private static Logger log = LoggerFactory.getLogger(TransportStreamInfo.class);
	private String duration;

	public MpegWmvStreamInfo(String videoStreamDetails, String duration) {
		super(videoStreamDetails);
		this.duration = duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}
	
	/**
	 * Calculates the duration in seconds, based on MpegWmvStreamInfo
	 * @param localtsContent the extracted info
	 * @return duration of the videofile in seconds
	 */
	public String getDuration() {
		String[] outPut = duration.trim().split(" ");
		duration = outPut[1];
		duration = duration.substring(0, duration.length()-1);
		System.out.println(duration);
		outPut = duration.split(":");
		int sec = Integer.parseInt(outPut[outPut.length-1].split("\\.")[0]);
		int min =0;
		if(outPut.length-2>0){
			min = Integer.parseInt(outPut[outPut.length-2]);
		}
		int hou = 0;
		if(outPut.length-3>=0){
			hou = Integer.parseInt(outPut[outPut.length-3]);
		}
		duration = sec + (min*60) + (hou*60*60)+"";
		return duration;
	}
	
	/**
	 * Uses ffProbe to analyze the transportStream
	 * @param Path to inputfile
	 * @param resources
	 * @return a MpegWmvStreamInfo instance with analyzed ffprope data
	 */
	public static MpegWmvStreamInfo analyze(String Path, ResourceLinks resources){
		String commandline =resources.getFfprobe()+" "+Path;
		log.debug("Running commandline: {}",commandline);
		ProcessRunner pr = new ProcessRunner("bash","-c",commandline);

		pr.run();
		String StringOutput = pr.getProcessOutputAsString();
		String StringError = pr.getProcessErrorAsString();
		log.debug(StringOutput);
		log.debug(StringError);
		String[] outPut =StringError.split("\n");
		String duration = "";
		String videoInfo = "";
		for(String s: outPut){
			if(s.toLowerCase().contains("duration")){
				duration = s.trim();
			}
			if(s.toLowerCase().contains("video")){
				videoInfo = s.trim()+"\n";
			}
		}

		return new MpegWmvStreamInfo(videoInfo, duration);
	}
}
