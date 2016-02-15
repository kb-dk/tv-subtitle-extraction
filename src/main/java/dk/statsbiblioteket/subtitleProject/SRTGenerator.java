package dk.statsbiblioteket.subtitleProject;

import dk.statsbiblioteket.subtitleProject.common.ResourceLinks;
import dk.statsbiblioteket.subtitleProject.common.SubtitleFragment;
import dk.statsbiblioteket.subtitleProject.hardCodedSubs.HardCodedSubs;
import dk.statsbiblioteket.subtitleProject.nonStreamed.MpegWmvStreamInfo;
import dk.statsbiblioteket.subtitleProject.subtitleStream.Demux;
import dk.statsbiblioteket.subtitleProject.teletext.TeletextIndexes;
import dk.statsbiblioteket.subtitleProject.teletext.TimePeriod;
import dk.statsbiblioteket.subtitleProject.transportStream.TransportStreamInfo;
import dk.statsbiblioteket.util.console.ProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.type.UnknownTypeException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Generates srt-files. Deletes srt if no content is found.
 */
public class SRTGenerator implements Callable<Integer>  {
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);

	private final String executableProgramNo = " --program-number ";
	private final static String executableDest = " -out=srt -o ";
	private final static String executabletelxPage = " -utf8 --nofontcolor -tpage ";

	private final File path;
	private final String outputFolder;
	private final ResourceLinks resources;
	boolean demuxed;
	private Map<String, List<SubtitleFragment>> dvbSubstreamMap;

	private TeletextIndexes teletextIndexes;

	public SRTGenerator(final File path, ResourceLinks resources) {
		this.path = path;
		this.outputFolder = resources.getOutput();
		this.resources = resources;
	}

	/**
	 * Generate srt files for every program in transportStream
	 * @param videoFile to extract srt files from
	 * @return The number of srt files there has been generated based on the single path
	 * @throws Exception
	 */
	private  int generateFile(File videoFile) throws Exception{
		int numberOfSrt = 0;

		boolean transportStream = videoFile.getName().endsWith(".ts");

		if(transportStream){
			log.info("TransportStream Detected");
			log.debug("Analyzing '{}'",videoFile.getAbsolutePath());
			List<TransportStreamInfo> transportStreamContent = TransportStreamInfo.analyze(videoFile.getAbsolutePath(), resources);
			dvbSubstreamMap = new HashMap<String, List<SubtitleFragment>>();
			log.debug("ProgramCount: {}",transportStreamContent.size());

			// bool to make sure projectX isn't called twice on same file..
			demuxed = false;
			String srtEnd = ".srt";

			for(int i = 0;i<transportStreamContent.size();i++){
				TransportStreamInfo localtsContent = transportStreamContent.get(i);
				String[] temp = transportStreamContent.get(i).getProgramNo().split(" ");
				String programNo = executableProgramNo + temp[temp.length-1] + " ";

				String program ="_";
				for(int j = 0; j<temp.length;j++){
					program += temp[j];
				}
				// More than one program in stream, have to generate multiple srt-files. Srt filenames based on programNo
				srtEnd=program;


				File srtTeleTextPath = new File(outputFolder,videoFile.getName().replaceFirst("\\.ts$", srtEnd+"_teleText.srt"));
				File srtdvbSubPath = new File(outputFolder,videoFile.getName().replaceFirst("\\.ts$", srtEnd+"_dvbSub.srt"));
				File srthardcodedSubsPath = new File(outputFolder,videoFile.getName().replaceFirst("\\.ts$", srtEnd+"_hardcodedSubs.srt"));

				int content;

				content = extractSRTFromTeletext(srtTeleTextPath, localtsContent, programNo);

				if(!transportStreamContent.get(i).getVideoStreamDetails().contains("No Video Stream")){

					content = extractSRTFromPicture(videoFile,
													transportStreamContent, srtdvbSubPath,
													srthardcodedSubsPath, content, localtsContent);
				}
				else{
					log.info("{} have no videostream.. Moving on",localtsContent.getProgramNo());
					srtdvbSubPath.delete();
					srthardcodedSubsPath.delete();
				}

				numberOfSrt += content;
			}
		}
		else{
			numberOfSrt = generateSRTFromNonTs(videoFile, numberOfSrt);
		}
		return numberOfSrt;
	}

	/**
	 * If videofiletype is mpeg or wmv, hardcoded subs will try to be detected
	 * @param path to videofile
	 * @param numberOfSrt there has been generated so far (Really not needed)
	 * @return 1 if content detected and srt has been generated, else 0
	 * @throws IOException
	 * @throws UnknownTypeException
	 */
	private int generateSRTFromNonTs(File path, int numberOfSrt)
			throws IOException, UnknownTypeException {
		String[] fileName = path.getName().split("\\.");
		String type = fileName[fileName.length-1];
		log.info("{} Detected",type);
		if(type.equalsIgnoreCase("wmv")||type.equalsIgnoreCase("mpeg")){
			log.debug("Analyzing {}",path.getAbsolutePath());
			MpegWmvStreamInfo MpegWmvStreamContent = MpegWmvStreamInfo.analyze(path.getAbsolutePath(), resources);
			File srthardcodedSubsPath;
			if(path.getName().endsWith(".mpeg")){
				srthardcodedSubsPath = new File(outputFolder,path.getName().replaceFirst("\\.mpeg$", "_hardcodedSubs.srt"));
			}
			else{
				srthardcodedSubsPath = new File(outputFolder,path.getName().replaceFirst("\\.wmv$", "_hardcodedSubs.srt"));
			}
			int tempValue = HardCodedSubs.generateNonTsFrames(path, MpegWmvStreamContent, resources, srthardcodedSubsPath);
			if(tempValue==0){
				log.info("{} (didn't detect enough valid text)",srthardcodedSubsPath.getAbsolutePath());
				srthardcodedSubsPath.delete();
			}
			numberOfSrt += tempValue;
		}
		else{
			log.error("{} is not supported",type);
			throw new UnknownTypeException(null, null);
		}
		return numberOfSrt;
	}

	/**
	 * Uses ccExtractor to detect subtitles from teletext, based on service name and programno in transportStream
	 * @param srtTeleTextPath to write to
	 * @param localtsContent info about current stream
	 * @param programNo to process
	 * @return 1 if teletextsubtitles is detected else 0
	 * @throws Exception if no xmlpage is found
	 */
	private int extractSRTFromTeletext(File srtTeleTextPath, TransportStreamInfo localtsContent, String programNo) throws Exception{

		// If no outputDestination descriped in config file, inputdest will be used
		String dest = executableDest;
		dest += srtTeleTextPath.getAbsolutePath();
		if(outputFolder.equals(path.getParent()) || outputFolder.equals("")){
			dest = "";
		}

		// Sets the teltextpage based on properties
		String telNo;
		try {
			telNo = getTeletextPage(localtsContent, path);
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}

		String commandline = resources.getCcextractor()+" "+ programNo + path.getAbsolutePath()+dest+executabletelxPage+telNo;
		log.debug("Running commandline: {}",commandline);
		ProcessRunner pr = new ProcessRunner("bash","-c", commandline);
		pr.run();
		//String StringOutput = pr.getProcessOutputAsString();
		//String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);
		int content = haveContent(srtTeleTextPath);
		return content;
	}

	/**
	 * Try to detect subtitles in picture using ocr
	 * @param path to videofile
	 * @param transportStreamContent info of current stream
	 * @param srtdvbSubPath file to write to
	 * @param srthardcodedSubsPath file to write to
	 * @param content current number of srt numbers generated
	 * @param localtsContent info of current stream
	 * @return Number of srt files generated
	 * @throws IOException if srt file doens't exist
	 * @throws FileNotFoundException if srt file doens't exist
	 * @throws UnsupportedEncodingException if UTF-8 isn't supported
	 */
	private int extractSRTFromPicture(File path,
									  List<TransportStreamInfo> transportStreamContent,
									  File srtdvbSubPath, File srthardcodedSubsPath, int content,
									  TransportStreamInfo localtsContent) throws IOException,
			FileNotFoundException, UnsupportedEncodingException {
		int tempint = HardCodedSubs.generateTsFrames(path, localtsContent, resources, srthardcodedSubsPath);
		if(tempint==0){
			log.info("{} (didn't detect enough valid text in content)",srthardcodedSubsPath.getAbsolutePath());
			srthardcodedSubsPath.delete();
		}
		else{
			content += tempint;
		}

		if(!transportStreamContent.isEmpty()){
			content = extractSRTFromDvbSub(path, srtdvbSubPath,
										   content, localtsContent);
		}
		else{
			log.info("{} has no dvb_substream",srtdvbSubPath.getAbsolutePath());
			srtdvbSubPath.delete();
		}
		return content;
	}

	/**
	 * Demux the file if not demuxed yet, and gathers the pids. If current stream contains extracted demuxed pid, srt file is generated
	 * @param path to videofile
	 * @param srtdvbSubPath to write to
	 * @param content number of current generated srt files
	 * @param localtsContent info of current stream
	 * @return number of generated srt files
	 * @throws IOException if no srt-file or videofile exists
	 * @throws FileNotFoundException if no srt file exists
	 * @throws UnsupportedEncodingException if UTF-8 isn't supported
	 */
	private int extractSRTFromDvbSub(File path, File srtdvbSubPath,
									 int content, TransportStreamInfo localtsContent)
			throws IOException, FileNotFoundException,
			UnsupportedEncodingException {
		if(!demuxed){
			demuxed = true;
			log.info("Extracting subPictures from transportstream using ProjectX");
			dvbSubstreamMap = Demux.DemuxFile(resources, path);
			log.debug("amount of dvb_subtitle registred: {}",dvbSubstreamMap.keySet().size());
		}
		Iterator<String> it = dvbSubstreamMap.keySet().iterator();
		boolean match = false;
		while(it.hasNext() && !match){
			String pid = it.next();
			for(String subtitleStreams: localtsContent.getSubtitleStreams()){
				if(subtitleStreams.contains(pid)){
					match=true;
					log.info("{} (content detected.. running ocr)",srtdvbSubPath.getAbsolutePath());
					content += generateSrtFromSubtitleFragments(srtdvbSubPath, dvbSubstreamMap.get(pid));
				}
			}
		}
		if(!match){
			log.info("{} (no content...)",srtdvbSubPath.getAbsolutePath());
			srtdvbSubPath.delete();
		}
		return content;
	}

	/**
	 * Checks if there is content in the new srt file, if not the file is deleted
	 * @param file srt file to parse
	 * @return 1 if content is found, 0 if empty
	 * @throws IOException if no srt file exists
	 */
	private  int haveContent(File file) throws IOException{
		int lineCount = 0;
		int emptyLineCount = 0;
		String deleteNote ="";
		int content = 0;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))){

			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] srtContent = line.toLowerCase().trim().split(" ");
				lineCount++;
				if(srtContent.length<=1){
					emptyLineCount++;
				}

			}
		}

		if(emptyLineCount==lineCount){
			deleteNote =" (no Content...)";
			file.delete();
		}
		else{
			deleteNote =" (Content Detected)";
			content=1;
		}

		log.info("{} {}",file.getAbsolutePath(),deleteNote);
		return content;
	}

	/**
	 * Writes subtitleFragments to srt file according to the SRT protocol
	 * @param srtPath to write to
	 * @param subtitleFragments to write to srt
	 * @return 1
	 * @throws FileNotFoundException if srtfile don't exists
	 * @throws UnsupportedEncodingException if UTF-8 dont exists
	 */
	private int generateSrtFromSubtitleFragments(File srtPath, List<SubtitleFragment> subtitleFragments) throws FileNotFoundException, UnsupportedEncodingException{
		String srtContent ="";
		Collections.sort(subtitleFragments);
		for(int i =0;i<subtitleFragments.size();i++){
			if(subtitleFragments.get(i).haveContent()){
				srtContent+=subtitleFragments.get(i).getNo()+"\n";
				srtContent+=subtitleFragments.get(i).toString();
			}
		}
		try(PrintWriter writer = new PrintWriter(srtPath.getAbsolutePath(), "UTF-8")){
			writer.write(srtContent);
		}
		log.info("{} - (substream subs detected... file generated)",srtPath.getAbsolutePath());
		return 1;
	}

	/**
	 * Gets teletext page based on service_name or filename
	 * @param streamInfo info about current stream
	 * @param file videofile to extract from
	 * @return teletextPage to search on
	 * @throws Exception if no xml file is found
	 */
	private String getTeletextPage(TransportStreamInfo streamInfo, File file) throws Exception{
		if(teletextIndexes==null){
			try{
				teletextIndexes= new TeletextIndexes(resources.getTeleIndex());
			}
			catch(Exception e){
				throw new Exception(e.getMessage());
			}
		}

		//Default page - Denmark
		String page = "399";
		String[] splitName = file.getName().split("-");

		String yy = splitName[1];
		String mm = splitName[2];
		String dd = splitName[3];

		boolean foundTimePeriod = false;
		int i = 0;
		TimePeriod currentTP = null;

		while(!foundTimePeriod && i<teletextIndexes.getTimePeriods().size()){
			if(teletextIndexes.getTimePeriods().get(i).compareTo(new String[]{yy,mm,dd})>0){
				foundTimePeriod = true;
				if(i!=0){
					i--;
				}
				currentTP = teletextIndexes.getTimePeriods().get(i);
			}
			else{
				i++;
			}
		}

		if(!foundTimePeriod){
			currentTP = teletextIndexes.getTimePeriods().get(teletextIndexes.getTimePeriods().size()-1);
		}

		Iterator<String> channels = currentTP.getIndexes().keySet().iterator();
		boolean found = false;
		String serviceNameString = streamInfo.getServiceName().replace(" ", "");

		while(!found && channels.hasNext()){
			String tmpChannel = channels.next();
			if(tmpChannel.toLowerCase().contains(serviceNameString.toLowerCase())){

				page = currentTP.getIndexes().get(tmpChannel);
				found = true;
			}
		}
		return page;
	}

	public Integer call() throws Exception {
		return generateFile(path);
	}
}

