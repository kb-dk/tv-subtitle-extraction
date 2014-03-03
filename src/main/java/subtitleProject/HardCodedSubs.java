package subtitleProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;

public class HardCodedSubs {
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);
	private static String[] dict;

	/**
	 * Based on streamInfo instance, the recording framesize is calculated (buttom 20%), and then recorded. 3600 frames will be recorded, one for each second in a hour.
	 * @param file
	 * @param localtsContent
	 * @param properties
	 * @param srtPath
	 * @return
	 * @throws IOException
	 */
	public static int generateTsFrames(File file, TransportStreamInfo localtsContent, ResourceLinks resources, File srtPath) throws IOException{
		int succes = 0;
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
		log.debug("Running OCR on: "+frameSize+" frames, yOffset: "+yOffset+" (total framsize: "+frameSizeSplit[0]+"x"+frameSizeSplit[1]+")");
		String recordedFrames = "60";
		Pattern p = Pattern.compile("\\#(.*?)\\[");
		Matcher m = p.matcher(localtsContent.getVideoStreamDetails());
		String pid ="";
		while (m.find()) {
			pid = m.group(1);
		}
		log.debug("Running commandline: "+"var/ffmpeg -i "+file.getAbsolutePath()+" -r 1 -s "+frameSize +" -vf crop="+frameSizeSplit[0]+":"+yFrameSize+":0:"+yOffset+" -an -y -vframes "+recordedFrames+" -map "+pid + " "+resources.getOutput()+file.getName()+"%d.png");
		ProcessRunner pr = new ProcessRunner("bash","-c","var/ffmpeg -i "+file.getAbsolutePath()+" -r 1 -s "+frameSize +" -vf crop="+frameSizeSplit[0]+":"+yFrameSize+":0:"+yOffset+" -an -y -vframes "+recordedFrames+" -map "+pid + " "+resources.getOutput()+file.getName()+"%d.png");
		pr.run();
		//String StringOutput = pr.getProcessOutputAsString();
		//String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);

		File[] files = new File(resources.getOutput()).listFiles(new FilenameFilter() {

			public boolean accept(File directory, String filename) {
				return filename.endsWith(".png");
			}
		});

		if (files == null){
			throw new IOException("No images found");
		}

		ArrayList<SubtitleFragment> subtitleFragments = new ArrayList<SubtitleFragment>();

		for(int i = 0; i<files.length;i++){
			subtitleFragments.add(ocrFrame(files[i], resources));
		}

		Collections.sort(subtitleFragments);

		int counter = 1;
		String srtContent ="";
		for(int i =0;i<subtitleFragments.size();i++){
			if(subtitleFragments.get(i).haveContent()){
				srtContent+=counter+"\n";
				srtContent+=subtitleFragments.get(i).toString();
				counter++;
			}
		}
		if(counter>1){
			PrintWriter writer = new PrintWriter(srtPath.getAbsolutePath(), "UTF-8");
			writer.write(srtContent);
			writer.close();
			log.info(srtPath.getAbsolutePath()+" - (hardcoded subs detected... file generated)");
			succes = 1;
		}
		else{
			log.info(srtPath.getAbsolutePath()+" (No hardcoded subs detected)");
		}

		return succes;
	}

	/**
	 * Based on streamInfo instance, the recording framesize is calculated (buttom 20%), and then recorded. the frameamount is calculated based on inputStreamn.
	 * @param file
	 * @param localtsContent
	 * @param properties
	 * @param srtPath
	 * @return
	 * @throws IOException
	 */
	public static int generateNonTsFrames(File file, MpegWmvStreamInfo localtsContent, ResourceLinks resources, File srtPath) throws IOException{
		//Calculation amount of frames to record
		String duration = localtsContent.getDuration();
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
		log.debug("Running OCR on: "+frameSize+" frames, yOffset: "+yOffset+" (total framsize: "+frameSizeSplit[0]+"x"+frameSizeSplit[1]+")");
		log.debug("Running commandline: "+"var/ffmpeg -i "+file.getAbsolutePath()+" -r 1 -s "+frameSize +" -vf crop="+frameSizeSplit[0]+":"+yFrameSize+":0:"+yOffset+" -an -y -vframes "+duration+" "+resources.getOutput()+file.getName()+"%d.png");
		ProcessRunner pr = new ProcessRunner("bash","-c","var/ffmpeg -i "+file.getAbsolutePath()+" -r 1 -s "+frameSize +" -vf crop="+frameSizeSplit[0]+":"+yFrameSize+":0:"+yOffset+" -an -y -vframes "+duration+" "+resources.getOutput()+file.getName()+"%d.png");
		pr.run();

		File[] files = new File(resources.getOutput()).listFiles(new FilenameFilter() {
			public boolean accept(File directory, String filename) {
				return filename.endsWith(".png");
			}
		});

		if (files == null){
			throw new IOException("No images found");
		}

		ArrayList<SubtitleFragment> subtitleFragments = new ArrayList<SubtitleFragment>();

		for(int i = 0; i<files.length;i++){
			subtitleFragments.add(ocrFrame(files[i], resources));
		}

		Collections.sort(subtitleFragments);
		int succes = 0;
		int counter = 1;
		String srtContent ="";
		for(int i =0;i<subtitleFragments.size();i++){
			if(subtitleFragments.get(i).haveContent()){
				srtContent+=counter+"\n";
				srtContent+=subtitleFragments.get(i).toString();
				counter++;
			}
		}
		if(counter>1){
			PrintWriter writer = new PrintWriter(srtPath.getAbsolutePath(), "UTF-8");
			writer.write(srtContent);
			writer.close();
			log.info(srtPath.getAbsolutePath()+" - (hardcoded subs detected... file generated)");
			succes = 1;
		}
		else{
			log.info(srtPath.getAbsolutePath()+" (No hardcoded subs detected)");
		}

		return succes;
	}

	/**
	 * Image manipulation using ImageMagick, and the Ocr using Tesseract
	 * @param file
	 * @param properties
	 * @return
	 * @throws IOException
	 */
	private static SubtitleFragment ocrFrame(File file, ResourceLinks resources) throws IOException{

		//Turn non-light pixels in picture to black
		//Sensitivity based on video format
		String thresholdvalue = "170";
		if(file.getName().contains(".ts")){
			thresholdvalue = "200";
		}
		//log.debug("Running commandline: "+"convert "+file.getAbsolutePath()+" -black-threshold "+thresholdvalue+" "+file.getAbsolutePath());
		ProcessRunner pr = new ProcessRunner("bash","-c","convert "+file.getAbsolutePath()+" -black-threshold "+thresholdvalue+" "+file.getAbsolutePath());
		pr.run();

		//Reduce Contrast to make darker
		//log.debug("Running commandline: "+"convert "+file.getAbsolutePath()+" -contrast -contrast "+file.getAbsolutePath());
		pr = new ProcessRunner("bash","-c","convert "+file.getAbsolutePath()+" -contrast -contrast "+file.getAbsolutePath());
		pr.run();

		//log.debug("Running commandline: "+"tesseract "+file.getAbsolutePath()+" "+ file.getAbsolutePath()+" -l dan "+resources.getTessConfig());
		pr = new ProcessRunner("bash","-c","tesseract "+file.getAbsolutePath()+" "+ file.getAbsolutePath()+" -l dan "+resources.getTessConfig());
		pr.run();
		File ocrTxt = new File(file.getAbsolutePath()+".txt");

		BufferedReader reader = null;
		String line ="";
		String content ="";
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(ocrTxt), "UTF-8"));

		while ((line = reader.readLine()) != null)
		{
			content += line.trim().toLowerCase()+"\n";		
		}

		reader.close();
		ocrTxt.delete();
		file.delete();

		String tempNo = file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-4);
		String[] name;
		if(tempNo.toLowerCase().contains("mpeg")){
			name = tempNo.split("mpeg");
		}
		else if(tempNo.toLowerCase().contains("wmv")){
			name = tempNo.split("wmv");
		}
		else{
			name = tempNo.split("ts");
		}
		int no = Integer.parseInt(name[name.length-1]);
		float min = 0;
		int sec = 0;
		if(no>59){
			min = no/60;

		}
		sec = no%60;
		int minInt = (int)min;
		String minString = ""+minInt;
		if(minString.length()==1){
			minString = "0".concat(minString);
		}
		String secString = ""+sec;
		if(secString.length()==1){
			secString = "0".concat(secString);
		}

		String secStringAfter = ""+(sec+1);
		if(secStringAfter.length()==1){
			secStringAfter = "0".concat(secStringAfter);
		}
		String minStringAfter = minString;
		if(secStringAfter.equals("60")){
			secStringAfter="00";
			minStringAfter = (Integer.parseInt(minStringAfter)+1)+"";
			if(minStringAfter.length()==1){
				minStringAfter = "0".concat(minStringAfter);
			}
		}
		
		//		SRT protocol
		//		2
		//		00:00:13,000 --> 00:00:18,320
		//		- Olsen, hva' fanden laver du?
		
		String timeStamp= "00:"+minString+":"+secString+",000 --> 00:"+minStringAfter+":"+secStringAfter+",000";
		if(!validText(content, resources)){
			content = "";
		}
		SubtitleFragment sf = new SubtitleFragment(Integer.parseInt(name[name.length-1]),timeStamp,content);
		return sf;
	}

	/**
	 * populates the dictionary and checks every word, if 50% of content exists, the subtitle is asummed to be valid
	 * @param line
	 * @param properties
	 * @return
	 * @throws IOException
	 */
	private static boolean validText(String line, ResourceLinks resources) throws IOException{
		boolean valid = false;
		int total = 0;
		int validCount = 0;

		//populate dictionary
		if(dict==null){
			populateDic(resources);
		}

		String[] cc = line.trim().split(" ");
		total = cc.length;
		for(String s: cc){
			s.trim();
			validCount += searchWord(s);
		}

		if(total>1 && validCount>=(total/2)){
			valid = true;
		}

		return valid;
	}

	/**
	 * Using binary search to search a word
	 * @param properties
	 * @throws IOException
	 */
	private static void populateDic(ResourceLinks resources) throws IOException{
		if(dict==null){
			BufferedReader reader = null;
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(resources.getDict()), "UTF-8"));
			dict = new String[310114];
			String tmp = "";
			int i = 0;
			while ((tmp = reader.readLine()) != null)
			{
				if(i<dict.length){
					dict[i] = tmp;
					i++;
				}
			}
			reader.close();
		}
	}

	private static int searchWord(String word){
		int result = 0;
		int low = 0;
		int high = dict.length -1;
		int mid;
		while (low <= high) {
			mid = low + (high - low) / 2;
			if (dict[mid].compareTo(word)>0) {
				high = mid - 1;
			} else if (dict[mid].compareTo(word)<0) {
				low = mid + 1;
			} else {
				result = 1;
				break;
			}
		}
		return result;
	}
}