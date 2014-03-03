package subtitleProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.lang.model.type.UnknownTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;

public class SRTGenerator implements Callable<Integer>  {
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);

	private final String executableProgramNo = " --program-number ";
	private final static String executableDest = " -out=srt -o ";
	private final static String executabletelxPage = " -utf8 --nofontcolor -tpage ";

	private final File path;
	private final String outputFolder;
	private final ResourceLinks resources;

	private TeletextIndexes teletextIndexes;

	public SRTGenerator(final File path, ResourceLinks resources) {
		this.path = path;
		this.outputFolder = resources.getOutput();
		this.resources = resources;
	}

	/**
	 * Generate srt files for every program in transportStream
	 * @param path
	 * @return The number of srt files there has been generated based on the single path
	 * @throws Exception 
	 */
	private  int generateFile(File path) throws Exception{
		int numberOfSrt = 0;

		boolean transportStream = path.getName().endsWith(".ts");

		if(transportStream){
			log.info("TransportStream Detected");
			log.debug("Analyzing "+path.getAbsolutePath());
			ArrayList<TransportStreamInfo> transportStreamContent = TransportStreamAnalyzer.analyze(path.getAbsolutePath());
			HashMap<String, ArrayList<SubtitleFragment>> dvbSubstreamMap = new HashMap<String, ArrayList<SubtitleFragment>>();

			// bool to make sure projectX isn't called twice on same file..
			boolean demuxed = false;

			String srtEnd = ".srt";

			for(int i = 0;i<transportStreamContent.size();i++){
				String[] temp = transportStreamContent.get(i).getProgramNo().split(" ");
				String programNo = executableProgramNo + temp[temp.length-1] + " ";

				String program ="_";
				for(int j = 0; j<temp.length;j++){
					program += temp[j];
				}
				// More than one program in stream, have to generate multiple srt-files. Srt filenames based on programNo
				srtEnd=program;

				String dest = executableDest;
				File srtTeleTextPath = new File(outputFolder,path.getName().replaceFirst("\\.ts$", srtEnd+"_teleText.srt"));
				File srtdvbSubPath = new File(outputFolder,path.getName().replaceFirst("\\.ts$", srtEnd+"_dvbSub.srt"));
				File srthardcodedSubsPath = new File(outputFolder,path.getName().replaceFirst("\\.ts$", srtEnd+"_hardcodedSubs.srt"));

				// If no outputDestination descriped in config file, inputdest will be used
				dest += srtTeleTextPath.getAbsolutePath();
				if(outputFolder.equals(path.getParent()) || outputFolder.equals("")){
					dest = "";
				}

				// Sets the teltextpage based on properties
				String telNo;
				try {
					telNo = getTeletextPage(transportStreamContent.get(i), path);
				} catch (Exception e) {
					throw new Exception(e.getMessage());
				}

				log.debug("Running commandline: "+"ccextractor "+ programNo + path.getAbsolutePath()+dest+executabletelxPage+telNo);
				ProcessRunner pr = new ProcessRunner("bash","-c", "ccextractor "+ programNo + path.getAbsolutePath()+dest+executabletelxPage+telNo);
				pr.run();
				//String StringOutput = pr.getProcessOutputAsString();
				//String StringError = pr.getProcessErrorAsString();
				//log.debug(StringOutput);
				//log.debug(StringError);
				int content = haveContent(srtTeleTextPath);
				TransportStreamInfo localtsContent = transportStreamContent.get(i); 

				if(!transportStreamContent.get(i).getVideoStreamDetails().contains("No Video Stream")){

					int tempint = HardCodedSubs.generateTsFrames(path, localtsContent, resources, srthardcodedSubsPath);
					if(tempint==0){
						log.info(srthardcodedSubsPath.getAbsolutePath()+" (didn't detect enough valid text in content)");
						srthardcodedSubsPath.delete();
					}
					else{
						content += tempint;
					}

					if(!transportStreamContent.get(i).getSubtitleStreams().isEmpty()){
						if(!demuxed){
							demuxed = true;
							log.info("Extracting subPictures from transportstream using ProjectX");
							dvbSubstreamMap = Demux.DemuxFile(resources, path);
							log.debug("amount of dvb_subtitle registred: "+dvbSubstreamMap.keySet().size());
						}
						Iterator<String> it = dvbSubstreamMap.keySet().iterator();
						boolean match = false;
						while(it.hasNext() && !match){
							String pid = it.next();
							for(String subtitleStreams: localtsContent.getSubtitleStreams()){	
								if(subtitleStreams.contains(pid)){
									match=true;
									log.info(srtdvbSubPath.getAbsolutePath()+" (content detected.. running ocr)");
									content += generateSrtFromSubtitleFragments(srtdvbSubPath, dvbSubstreamMap.get(pid));
								}
							}
						}
						if(!match){							
							log.info(srtdvbSubPath.getAbsolutePath()+" (no content...)");
							srtdvbSubPath.delete();						
						}
					}
					else{
						log.info(srtdvbSubPath.getAbsolutePath()+" has no dvb_substream");
						srtdvbSubPath.delete();
					}
				}
				else{
					log.info(localtsContent.getProgramNo()+" have no videostream.. Moving on");
					srtdvbSubPath.delete();
					srthardcodedSubsPath.delete();
				}

				numberOfSrt += content;
			}
		}
		else{
			String[] fileName = path.getName().split("\\.");
			String type = fileName[fileName.length-1];
			log.info(type+" Detected");
			if(type.equalsIgnoreCase("wmv")||type.equalsIgnoreCase("mpeg")){
				log.debug("Analyzing "+path.getAbsolutePath());
				MpegWmvStreamInfo MpegWmvStreamContent = MpegWmvAnalyser.analyze(path.getAbsolutePath(), resources);
				File srthardcodedSubsPath;
				if(path.getName().endsWith(".mpeg")){
					srthardcodedSubsPath = new File(outputFolder,path.getName().replaceFirst("\\.mpeg$", "_hardcodedSubs.srt"));
				}
				else{
					srthardcodedSubsPath = new File(outputFolder,path.getName().replaceFirst("\\.wmv$", "_hardcodedSubs.srt"));
				}
				int tempValue = HardCodedSubs.generateNonTsFrames(path, MpegWmvStreamContent, resources, srthardcodedSubsPath);
				if(tempValue==0){
					log.info(srthardcodedSubsPath.getAbsolutePath()+" (didn't detect enough valid text)");
					srthardcodedSubsPath.delete();
				}
				numberOfSrt += tempValue;
			}
			else{
				log.error(type+" is not supported");
				throw new UnknownTypeException(null, null); 
			}
		}
		return numberOfSrt;
	}

	/**
	 * Checks if there is content in the new srt file, if not the file is deleted
	 * @param file
	 * @return 1 if content is found, 0 if empty
	 * @throws IOException
	 */
	private  int haveContent(File file) throws IOException{
		BufferedReader reader = null;
		int lineCount = 0;
		int emptyLineCount = 0;
		String deleteNote ="";
		int content = 0;
		try { 
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] srtContent = line.toLowerCase().trim().split(" ");
				lineCount++;
				if(srtContent.length<=1){
					emptyLineCount++;
				}
			}
		} finally {
			if(reader!=null){
				reader.close();
			}
			if(emptyLineCount==lineCount){
				deleteNote =" (no Content...)";
				file.delete();
			}
			else{
				deleteNote =" (Content Detected)";
				content=1;
			}
		}
		log.info(file.getAbsolutePath()+" "+deleteNote);
		return content;
	}

	/**
	 * Writes subtitleFragments to srt file accoridng to the SRT protocol 
	 * @param srtPath
	 * @param subtitleFragments
	 * @return 1
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private int generateSrtFromSubtitleFragments(File srtPath, ArrayList<SubtitleFragment> subtitleFragments) throws FileNotFoundException, UnsupportedEncodingException{
		String srtContent ="";
		Collections.sort(subtitleFragments);
		for(int i =0;i<subtitleFragments.size();i++){
			if(subtitleFragments.get(i).haveContent()){
				srtContent+=subtitleFragments.get(i).getNo()+"\n";
				srtContent+=subtitleFragments.get(i).toString();
			}
		}
		PrintWriter writer = new PrintWriter(srtPath.getAbsolutePath(), "UTF-8");
		writer.write(srtContent);
		writer.close();
		log.info(srtPath.getAbsolutePath()+" - (substream subs detected... file generated)");
		return 1;
	}

	/**
	 * Gets teletext page based on service_name or filename
	 * @param streamInfo
	 * @param file
	 * @return teletextPage
	 * @throws Exception 
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

		while(!foundTimePeriod && i<teletextIndexes.getTimePeriodes().size()){
			if(teletextIndexes.getTimePeriodes().get(i).compareTo(new String[]{yy,mm,dd})>0){
				foundTimePeriod = true;
				if(i!=0){
					i--;
				}
				currentTP = teletextIndexes.getTimePeriodes().get(i);
			}
			else{
				i++;
			}
		}

		if(!foundTimePeriod){
			currentTP = teletextIndexes.getTimePeriodes().get(teletextIndexes.getTimePeriodes().size()-1);
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
