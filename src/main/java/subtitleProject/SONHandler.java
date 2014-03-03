package subtitleProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;

public class SONHandler {
	private static String[] dict;
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);
	/**
	 * Iterates through .SON subtitlefile, and generates subtitleFragments based on loginfo and ocr result 
	 * @param sonFiles
	 * @param properties
	 * @return
	 * @throws IOException
	 */
	public static HashMap<String, ArrayList<SubtitleFragment>> sonHandler(HashMap<String, File> sonFiles, ResourceLinks resources) throws IOException{
		HashMap<String, ArrayList<SubtitleFragment>> subsToPids = new HashMap<String, ArrayList<SubtitleFragment>>();
		Iterator<String> it = sonFiles.keySet().iterator();
		while(it.hasNext()){
			BufferedReader reader = null;
			String currentPid = it.next();
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(sonFiles.get(currentPid)), "UTF-8"));
			String line;
			ArrayList<SubtitleFragment> subtitleFragments = new ArrayList<SubtitleFragment>();
			while ((line = reader.readLine()) != null)
			{
				if(line.contains("Display_Area")){
					line = reader.readLine();
					String[] temp = line.split("\t");
					int no = Integer.parseInt(temp[0]);
					String[] timeStamp1 = temp[2].split(":");
					String[] timeStamp2 = temp[3].split(":");
					String timeStamp = timeStamp1[0]+":"+timeStamp1[1]+":"+timeStamp1[2]+","+timeStamp1[3]+"0 --> "+timeStamp2[0]+":"+timeStamp2[1]+":"+timeStamp2[2]+","+timeStamp2[3]+"0";
					File bmpFile = new File(resources.getOutput()+temp[4]);
					String pngFileName = bmpFile.getAbsolutePath().replaceFirst("\\.bmp$", ".png");
					log.debug("Running commandline: "+"convert "+bmpFile.getAbsolutePath() + " "+ pngFileName);
					ProcessRunner pr = new ProcessRunner("bash","-c","convert "+bmpFile.getAbsolutePath() + " "+ pngFileName);
					pr.run();
					bmpFile.delete();
					subtitleFragments.add(ocrFrame(new File(pngFileName), resources, timeStamp, no));
				}
			}
			reader.close();
			subsToPids.put(currentPid, subtitleFragments);
		}
		return subsToPids;
	}

	/**
	 * Image manipulation using ImageMagick, and the Ocr using Tesseract
	 * @param file
	 * @param properties
	 * @return
	 * @throws IOException
	 */
	private static SubtitleFragment ocrFrame(File file, ResourceLinks resources, String timeStamp, int number) throws IOException{
		//log.debug("Running commandline: "+"convert -black-threshold 190 "+file.getAbsolutePath());
		ProcessRunner pr1 = new ProcessRunner("bash","-c","convert -black-threshold 190 "+file.getAbsolutePath());
		pr1.run(); 

		//String StringOutput1 = pr1.getProcessOutputAsString();
		//String StringError1 = pr1.getProcessErrorAsString();
		//log.debug(StringOutput1);
		//log.debug(StringError1);

		//log.debug("Running commandline: "+"tesseract "+file.getAbsolutePath()+" "+ file.getAbsolutePath()+" -l dan "+resources.getTessConfig());
		ProcessRunner pr = new ProcessRunner("bash","-c","tesseract "+file.getAbsolutePath()+" "+ file.getAbsolutePath()+" -l dan "+resources.getTessConfig());
		pr.run();
		//String StringOutput = pr.getProcessOutputAsString();
		//String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);
		File ocrTxt = new File(file.getAbsolutePath()+".txt");

		BufferedReader reader = null;
		String line ="";
		String content ="";
		try{
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(ocrTxt), "UTF-8"));

			while ((line = reader.readLine()) != null)
			{			
				content += line.trim().toLowerCase()+"\n";
			}

			reader.close();
			ocrTxt.delete();
			file.delete();
		}
		catch(Exception e){
			throw new RuntimeException(e.getMessage());
		}
		if(!validText(content, resources)){
			content = "";
		}

		SubtitleFragment sf = new SubtitleFragment(number,timeStamp,content);
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