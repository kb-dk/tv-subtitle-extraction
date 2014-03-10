package subtitleProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;

/**
 * Class to extract subtitles by ocr of frames taken every second.
 * @author Jacob
 *
 */
public class HardCodedSubs {
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);
	private static String[] dict;

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

		List<SubtitleFragment> subtitleFragments = new ArrayList<SubtitleFragment>();

		for(int i = 0; i<files.length;i++){
			subtitleFragments.add(ocrFrame(files[i], resources));
		}

		int succes = 0;
		try {
			String srtContent = formatSubtitles(subtitleFragments);
			succes = writeSRTFile(srtPath, srtContent);
		} catch (NoSubsException e){
			log.info(srtPath.getAbsolutePath()+" (No hardcoded subs detected)");
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
		int yFrameSize = yOffset;
		yOffset = (yOffset/100)*80;
		yFrameSize = Integer.parseInt(frameSizeSplit[1])-yOffset;
		frameSize = frameSizeSplit[0]+"x"+yFrameSize;
		log.debug("Running OCR on: "+frameSize+" frames, yOffset: "+yOffset+" (total framsize: "+frameSizeSplit[0]+"x"+frameSizeSplit[1]+")");
		String recordedFrames = "3600";
		Pattern p = Pattern.compile("\\#(.*?)\\[");
		Matcher m = p.matcher(localtsContent.getVideoStreamDetails());
		String pid ="";
		while (m.find()) {
			pid = m.group(1);
		}
		String commandline = resources.getFfmpeg()+" -i "+file.getAbsolutePath()+" -r 1 -s "+frameSize +" -vf crop="+frameSizeSplit[0]+":"+yFrameSize+":0:"+yOffset+" -an -y -vframes "+recordedFrames+" -map "+pid + " "+resources.getOutput()+srtPath.getName()+"%d.png";
		log.debug("Running commandline: "+commandline);
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
		String duration = getDuration(localtsContent);
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
		log.debug("Running OCR on: "+frameSize+" frames, yOffset: "+yOffset+" (total framsize: "+frameSizeSplit[0]+"x"+frameSizeSplit[1]+")");
		log.debug("Running commandline: "+ commandLine);
		ProcessRunner pr = new ProcessRunner("bash","-c",commandLine);
		pr.run();
		//String StringOutput = pr.getProcessOutputAsString();
		//String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);
	}

	/**
	 * Remove duplicate subtitles and format them as SRT
	 * @param subtitleFragments the subtitle fragments
	 * @return the SRT formatted subtitles
	 * @throws NoSubsException if no subtitles was found
	 */
	private static String formatSubtitles(
			List<SubtitleFragment> subtitleFragments)
					throws NoSubsException {
		Collections.sort(subtitleFragments);
		removeDuplicates(subtitleFragments);
		int counter = 1;
		String srtContent ="";
		for(int i =0;i<subtitleFragments.size();i++){
			if(subtitleFragments.get(i).haveContent()){
				srtContent+=counter+"\n";
				srtContent+=subtitleFragments.get(i).toString();
				counter++;
			}
		}
		if (counter == 1){
			throw new NoSubsException();
		}
		return srtContent;
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
		log.info(srtPath.getAbsolutePath()+" - (hardcoded subs detected... file generated)");
		return 1;
	}

	/**
	 * Calculates the duration in seconds, based on MpegWmvStreamInfo
	 * @param localtsContent the extracted info
	 * @return duration of the videofile in seconds
	 */
	private static String getDuration(MpegWmvStreamInfo localtsContent) {
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
		return duration;
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

	/**
	 * Image manipulation using ImageMagick, and ocr using Tesseract
	 * @param file image to manipulate
	 * @param properties
	 * @return subtitlefragment with ocr result
	 * @throws IOException if no ocr result from tesseract (tesseract error)
	 */
	private static SubtitleFragment ocrFrame(File file, ResourceLinks resources) throws IOException{

		ProcessRunner pr;
		editFrame(file, resources);

		//log.debug("Running commandline: "+resources.getTesseract()+" "+file.getAbsolutePath()+" "+ file.getAbsolutePath()+" -l dan "+resources.getTessConfig());
		pr = new ProcessRunner("bash","-c",resources.getTesseract()+" "+file.getAbsolutePath()+" "+ file.getAbsolutePath()+" -l dan "+resources.getTessConfig());
		pr.run();
		File ocrTxt = new File(file.getAbsolutePath()+".txt");

		//BufferedReader reader = null;
		String line ="";
		String content ="";

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ocrTxt), "UTF-8"))){

			while ((line = reader.readLine()) != null)
			{
				line = line.replaceAll("\\s+", " ");
				content += line.trim().toLowerCase()+"\n";		
			}

			//reader.close();
		}
		ocrTxt.delete();
		file.delete();

		SubtitleFragment sf = formatToSRTProtocol(file, resources, content);
		return sf;
	}

	/**
	 * Generates a SubtitleFragment from ocr result and name Convention 
	 * @param file image from videofile
	 * @param resources
	 * @param content from tesseract result
	 * @return Subtitlefragment with information from single second in videofile
	 * @throws NumberFormatException if Integer parse fails
	 * @throws IOException if dictionary doesn't exists
	 */
	private static SubtitleFragment formatToSRTProtocol(File file,
			ResourceLinks resources, String content)
					throws NumberFormatException, IOException {
		String tempNo = file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-4);
		String[] name;
		name = tempNo.split("srt");
		//		if(tempNo.toLowerCase().contains("mpeg")){
		//			name = tempNo.split("mpeg");
		//		}
		//		else if(tempNo.toLowerCase().contains("wmv")){
		//			name = tempNo.split("wmv");
		//		}
		//		else{
		//			name = tempNo.split("ts");
		//		}
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
	 * Turn as many non-white pixels as possible black, and lower the contrast to make ocr result as good as possible
	 * @param file Image to edit
	 * @param resources
	 */
	private static void editFrame(File file, ResourceLinks resources) {
		//Turn non-light pixels in picture to black
		//Sensitivity based on video format
		String thresholdvalue = "70%";
		if(file.getName().contains(".ts")){
			thresholdvalue = "90%";
		}
		//log.debug("Running commandline: "+resources.getConvert()+" "+file.getAbsolutePath()+" -black-threshold "+thresholdvalue+" "+file.getAbsolutePath());
		ProcessRunner pr = new ProcessRunner("bash","-c",resources.getConvert()+" "+file.getAbsolutePath()+" -black-threshold "+thresholdvalue+" "+file.getAbsolutePath());
		pr.run();

		//Reduce Contrast to make darker
		//log.debug("Running commandline: "+resources.getConvert()+" "+file.getAbsolutePath()+" -contrast -contrast "+file.getAbsolutePath());
		pr = new ProcessRunner("bash","-c",resources.getConvert()+" "+file.getAbsolutePath()+" -contrast -contrast "+file.getAbsolutePath());
		pr.run();
	}

	/**
	 * populates the dictionary and checks every word, if 50% of content exists, the subtitle is assumed to be valid
	 * @param line to check content of
	 * @param properties
	 * @return true if 50% of line is in dictionary, else false
	 * @throws IOException if no dictionary file is found
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
	 * @throws IOException if no dictionary file is found
	 */
	private static void populateDic(ResourceLinks resources) throws IOException{
		if(dict==null){

			try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(resources.getDict()), "UTF-8"))){
				dict = new String[310111];
				String tmp = "";
				int i = 0;
				while ((tmp = reader.readLine()) != null)
				{
					if(i<dict.length){
						dict[i] = tmp;
						i++;
					}
				}
			}
			//reader.close();
		}
	}

	/**
	 * search through dictionary for word, Binary style!
	 * @param word to search for
	 * @return 1 if found else 0
	 */
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

	/**
	 * Checks if the content of the subsequent subtitlefragments is identical based on the levenshteinsDistance and moderated to include the length difference
	 * Updates the first subtitleFragments timestamp, and sets every identical subsequent content to "" 
	 * @param subtitleFragments
	 */
	private static void removeDuplicates(List<SubtitleFragment> subtitleFragments){
		for(int i = 0; i<subtitleFragments.size();i++){
			if(subtitleFragments.get(i).haveContent()){
				String temp = subtitleFragments.get(i).getContent();
				boolean isSame = true;
				int j = i+1;
				String endTime ="";
				while(isSame && j<subtitleFragments.size()){
					if(subtitleFragments.get(j).haveContent()){
						if(isSame(temp, subtitleFragments.get(j).getContent())){
							endTime = subtitleFragments.get(j).getTimestamp().split(" ")[2];
							subtitleFragments.get(j).setContent("");
						}
						else{
							if(!endTime.equals("")){
								String[] timeSplit = subtitleFragments.get(i).getTimestamp().split(" ");
								subtitleFragments.get(i).setTimestamp(timeSplit[0]+" "+timeSplit[1]+" "+endTime);
							}
							i=j-1;
							isSame=false;
						}
					}
					j++;
				}
			}
		}
	}

	/**
	 * Based on the two ocr results, isSame() get the levenshteins Distance between these, however, this result isn't dynamic when we take the length difference in to account. 
	 * The words "sitting" and "kitten" is two very different words with the levenshteins difference: 3, but the sentences "hello world, this is subtitleProject calling" and 
	 * "hello wurld, this is subtitleProiect callxng" also have a levenshteins distance of 3 but will be assumed identical by a person. Because of the uncertainty of the ocr 
	 * result these variations can and will happen. Therefore the minimum length of the two compared strings is divided by the levenshtein Distance. Based on tests i have used the 
	 * number 2.5 as pivot for the assumption of identical strings. The higher the result from this method the more identical the strings are..
	 * @param s
	 * @param t
	 * @return levenshteins distance / min length > 2.5
	 */
	public static boolean isSame(String s, String t){
		boolean same = false;
		double minLength = s.length();
		if(minLength>t.length()){
			minLength = t.length();
		}
		double result = minLength/levenshteinDistance(s.toCharArray(), t.toCharArray());
		if(result>2.5){
			same = true;
		}
		return same;
	}

	/**
	 * Algorithm used to compare two strings, (http://en.wikipedia.org/wiki/Levenshtein_distance)
	 * @param s
	 * @param t
	 * @return the levenshteins distance
	 */
	static int levenshteinDistance(char s[], char t[])
	{
		// for all i and j, d[i,j] will hold the Levenshtein distance between
		// the first i characters of s and the first j characters of t;
		// note that d has (m+1)*(n+1) values
		int[][] d = new int[s.length+1][t.length+1];
		//clear all elements in d // set each element to zero

		// source prefixes can be transformed into empty string by
		// dropping all characters
		for(int i = 1; i<=s.length;i++) 
		{
			d[i][0] = i;
		}
		// target prefixes can be reached from empty source prefix
		// by inserting every characters
		for(int j=1;j<=t.length;j++)
		{
			d[0][j] = j;

		}
		for(int j = 1; j< t.length;j++)
		{
			for(int i =1; i<s.length;i++)
			{
				if( s[i-1] == t[j-1]){ 
					d[i][j] = d[i-1][j-1];
				}// no operation required
				else{
					int temp1 = d[i-1][j] + 1; // a deletion
					int temp2 = d[i][j-1] + 1; // an insertion
					int temp3 = d[i-1][j-1] + 1; // a substitution
					int min = temp1;
					if(min>temp2){
						min = temp2;
					}
					if(min>temp3){
						min = temp3;
					}
					d[i][j] = min;
				}
			}
		}
		return d[s.length-1][t.length-1];
	}
}