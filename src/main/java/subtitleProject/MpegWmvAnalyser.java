package subtitleProject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;

/**
 * Class to analyze mpeg or wmv files
 * @author Jacob
 *
 */
public class MpegWmvAnalyser {
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);
	
	/**
	 * Uses ffProbe to analyze the mpegfile
	 * @param Path of file to analyze
	 * @param properties
	 * @return a list with StreamInfo instances
	 */
	public static MpegWmvStreamInfo analyze(String Path, ResourceLinks resources){
		log.debug("Running commandline: "+resources.getFfprobe()+" "+Path);
		ProcessRunner pr = new ProcessRunner("bash","-c",resources.getFfprobe()+" "+Path);
		
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
