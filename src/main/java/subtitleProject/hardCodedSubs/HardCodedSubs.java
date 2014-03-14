package subtitleProject.hardCodedSubs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import subtitleProject.SubtitleProject;
import subtitleProject.common.NoSubsException;
import subtitleProject.common.OCR;
import subtitleProject.common.ResourceLinks;
import subtitleProject.nonStreamed.MpegWmvStreamInfo;
import subtitleProject.transportStream.TransportStreamInfo;
import dk.statsbiblioteket.util.console.ProcessRunner;

/**
 * Class to extract subtitles by ocr of frames taken every second.
 */
public class HardCodedSubs {
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);
	//private static Set<String> dict;

	/**
	 * Extracts frames from Transportstream and generates srt if content
	 * @param file ts-file to search for hardcoded subtitles
	 * @param localtsContent info of current program
	 * @param resources 
	 * @param srtPath srt file to contain upcoming subtitles
	 * @return 1 if subtitle is detected, else 0
	 * @throws IOException if srt file doesn't exists
	 */
	public static int generateTsFrames(File file, TransportStreamInfo localtsContent, ResourceLinks resources, File srtPath) throws IOException{
		extractPngFilesFromTS(file, srtPath, localtsContent, resources);

		return pngToSRT(resources, srtPath);
	}

	/**
	 * Extracts frames from Mpeg or Wmv and generates srt if content
	 * @param file videofile to search for hardcoded subtitles
	 * @param localtsContent info of current program
	 * @param resources 
	 * @param srtPath srt file to contain upcoming subtitles
	 * @return 1 if subtitle is detected, else 0
	 * @throws IOException if srt file doesn't exists
	 */
	public static int generateNonTsFrames(File file, MpegWmvStreamInfo localtsContent, ResourceLinks resources, File srtPath) throws IOException{
		extractPngFromNonTS(file, srtPath, localtsContent, resources);

		return pngToSRT(resources, srtPath);
	}

	/**
	 * Gather png files associated to the srtpath and run ocr. Result sent to srt.
	 * @param resources
	 * @param srtPath to get the right png files.
	 * @return 1 if subtitles is detected, else 0
	 * @throws IOException if no images is found
	 * @throws FileNotFoundException if srtfile doesn't exist
	 * @throws UnsupportedEncodingException if UTF-8 isn't supported
	 */
	private static int pngToSRT(ResourceLinks resources, File srtPath)
			throws IOException, FileNotFoundException,
			UnsupportedEncodingException {
		File[] files = new File(resources.getOutput()).listFiles(getPngFilter(srtPath));

		if (files == null){
			throw new IOException("No images found");
		}

		Subtitle subtitleFragments = new Subtitle();

		for(int i = 0; i<files.length;i++){
			subtitleFragments.add(OCR.ocrFrame(files[i], resources));
		}

		int succes = 0;
		try {
			String srtContent = subtitleFragments.format();
			succes = writeSRTFile(srtPath, srtContent);
		} catch (NoSubsException e){
			log.info("{} (No hardcoded subs detected)",srtPath.getAbsolutePath());
		}

		return succes;
	}

	/**
	 * Based on streamInfo instance, the recording framesize is calculated (buttom 20%), and then recorded. 3600 frames will be recorded, one for each second in a hour.
	 * @param file ts-file to search for hardcoded subtitle
	 * @param srtPath srt file to contain upcoming subtitles
	 * @param localtsContent info of current program
	 * @param resources
	 * @throws NumberFormatException if filename convention has changed since testing, and trying to parse letters to numbers
	 */
	private static void extractPngFilesFromTS(File file, File srtPath, 
			TransportStreamInfo localtsContent, ResourceLinks resources)
					throws NumberFormatException {
		String[] videoInfo = localtsContent.getVideoStreamDetails().trim().split(" ");
		String frameSize = "";
		for(int i = 0; i<videoInfo.length;i++){
			if(Pattern.matches("^\\d{2,}x\\d{2,}$",videoInfo[i])){
				frameSize = videoInfo[i];
			}
		}
		String[] frameSizeSplit = frameSize.split("x");
		int yOffset = Integer.parseInt(frameSizeSplit[frameSizeSplit.length-1]);
		//variable to contain there to record from on y
		int yFrameSize = yOffset;
		yOffset = (yOffset/100)*80;
		yFrameSize = Integer.parseInt(frameSizeSplit[1])-yOffset;
		frameSize = frameSizeSplit[0]+"x"+yFrameSize;
		log.debug("Running OCR on: {} frames, yOffset: {} (total framsize: {}x{})",frameSize,yOffset,frameSizeSplit[0],frameSizeSplit[1]);
		String recordedFrames = "3600";
		Pattern p = Pattern.compile("\\#(.*?)\\[");
		Matcher m = p.matcher(localtsContent.getVideoStreamDetails());
		String pid ="";
		while (m.find()) {
			pid = m.group(1);
		}
		String commandline = resources.getFfmpeg()+" -i "+file.getAbsolutePath()+" -r 1 -s "+frameSize +" -vf crop="+frameSizeSplit[0]+":"+yFrameSize+":0:"+yOffset+" -an -y -vframes "+recordedFrames+" -map "+pid + " "+resources.getOutput()+srtPath.getName()+"%d.png";
		log.debug("Running commandline: {}",commandline);
		ProcessRunner pr = new ProcessRunner("bash","-c",commandline);
		pr.run();
		//String StringOutput = pr.getProcessOutputAsString();
		//String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);
	}

	/**
	 * Based on streamInfo instance, the recording framesize is calculated (buttom 20%), and then recorded. the frameamount is calculated based on inputStreamn.
	 * @param file ts-file to search for hardcoded subtitle
	 * @param srtPath srt file to contain upcoming subtitles
	 * @param localtsContent info of current program
	 * @param resources
	 * @throws NumberFormatException if filename convention has changed since testing, and trying to parse letters to numbers
	 */
	private static void extractPngFromNonTS(File file, File srtPath,
			MpegWmvStreamInfo localtsContent, ResourceLinks resources)
					throws NumberFormatException {
		//Calculation amount of frames to record
		String duration = localtsContent.getDuration();
		//Calculation framsize to record
		String[] videoInfo = localtsContent.getVideoStreamDetails().trim().split(" ");
		String frameSize = "";
		for(int i = 0; i<videoInfo.length;i++){
			if(Pattern.matches("^\\d{2,}x\\d{2,}$",videoInfo[i])){
				frameSize = videoInfo[i];
			}
		}
		String[] frameSizeSplit = frameSize.split("x");
		int yOffset = Integer.parseInt(frameSizeSplit[frameSizeSplit.length-1]);
		int yFrameSize = yOffset;
		yOffset = (yOffset/100)*80;
		yFrameSize = Integer.parseInt(frameSizeSplit[1])-yOffset;
		frameSize = frameSizeSplit[0]+"x"+yFrameSize;
		String commandLine = resources.getFfmpeg()+" -i "+file.getAbsolutePath()+" -r 1 -s "+frameSize +" -vf crop="+frameSizeSplit[0]+":"+yFrameSize+":0:"+yOffset+" -an -y -vframes "+duration+" "+resources.getOutput()+srtPath.getName()+"%d.png";
		log.debug("Running OCR on: {} frames, yOffset: {} (total framsize: {}x{})",frameSize,yOffset,frameSizeSplit[0],frameSizeSplit[1]);
		log.debug("Running commandline: {}", commandLine);
		ProcessRunner pr = new ProcessRunner("bash","-c",commandLine);
		pr.run();
		//String StringOutput = pr.getProcessOutputAsString();
		//String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);
	}

	/**
	 * Writes the formatted subtitle to the srtFile
	 * @param srtPath the srtfile
	 * @param srtContent the subtitles
	 * @return 1 the srt generated count
	 * @throws FileNotFoundException if srtFile doesn't exists
	 * @throws UnsupportedEncodingException if UTF-8 isn't supported
	 */
	private static int writeSRTFile(File srtPath, String srtContent)
			throws FileNotFoundException, UnsupportedEncodingException {
		try (PrintWriter writer = new PrintWriter(srtPath.getAbsolutePath(), "UTF-8");){
			writer.write(srtContent);
		}
		log.info("{} - (hardcoded subs detected... file generated)",srtPath.getAbsolutePath());
		return 1;
	}

	/**
	 * Extracts every png image associated with the name of the current srtFile
	 * @param file srtFile
	 * @return FilenameFilter with the accepted images
	 */
	private static FilenameFilter getPngFilter(final File file){
		return new  FilenameFilter() {
			public boolean accept(File directory, String filename) {
				return filename.endsWith(".png") && filename.contains(file.getName());
			}
		};
	}
}