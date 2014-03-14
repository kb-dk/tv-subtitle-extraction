package subtitleProject.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import subtitleProject.hardCodedSubs.SubtitleFragmentFactory;
import dk.statsbiblioteket.util.console.ProcessRunner;

public class OCR {
	/**
	 * Image manipulation using ImageMagick, and ocr using Tesseract
	 * @param file image to manipulate
	 * @param properties
	 * @return subtitlefragment with ocr result
	 * @throws IOException if no ocr result from tesseract (tesseract error)
	 */
	public static SubtitleFragment ocrFrame(File file, ResourceLinks resources) throws IOException{

		ProcessRunner pr;
		editFrame(file, resources);
		String commandline = resources.getTesseract()+" "+file.getAbsolutePath()+" "+ file.getAbsolutePath()+" -l dan "+resources.getTessConfig();
		//log.debug("Running commandline: {}",commandline);
		pr = new ProcessRunner("bash","-c",commandline);
		pr.run();
		File ocrTxt = new File(file.getAbsolutePath()+".txt");

		String line ="";
		String content ="";

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ocrTxt), "UTF-8"))){
			while ((line = reader.readLine()) != null)
			{
				line = line.replaceAll("\\s+", " ");
				content += line.trim().toLowerCase()+"\n";		
			}
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
		int no = getNoFromFile(file);
		return SubtitleFragmentFactory.createSubtitleFragment(no, content, resources);
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
		String commandline = resources.getConvert()+" "+file.getAbsolutePath()+" -black-threshold "+thresholdvalue+" "+file.getAbsolutePath();
		//log.debug("Running commandline: {}",commandline);
		ProcessRunner pr = new ProcessRunner("bash","-c",commandline);
		pr.run();
		commandline =resources.getConvert()+" "+file.getAbsolutePath()+" -contrast -contrast "+file.getAbsolutePath();
		//Reduce Contrast to make darker
		//log.debug("Running commandline: {}",commandline);
		pr = new ProcessRunner("bash","-c",commandline);
		pr.run();
	}	
	
	/**
	 * Extracts the number in imagefilename
	 * @param file image to extract number from
	 * @return number 
	 * @throws NumberFormatException if parse of number 
	 */
	private static int getNoFromFile(File file) throws NumberFormatException {
		String tempNo = file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-4);
		String[] name;
		name = tempNo.split("srt");

		int no = Integer.parseInt(name[name.length-1]);
		return no;
	}
	
	/**
	 * Image manipulation using ImageMagick, and the Ocr using Tesseract
	 * @param file image to ocr
	 * @param properties 
	 * @return SubtitleFragment with ocr result and timestamp
	 */
	public static SubtitleFragment ocrFrameSon(File file, ResourceLinks resources, String timeStamp, int number){
		String commandline;
		editSon(file, resources);

		commandline = resources.getTesseract()+" "+file.getAbsolutePath()+" "+ file.getAbsolutePath()+" -l dan "+resources.getTessConfig();
		//log.debug("Running commandline: {}",commandline);
		ProcessRunner pr = new ProcessRunner("bash","-c",commandline);
		pr.run();
		//String StringOutput = pr.getProcessOutputAsString();
		//String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);
		File ocrTxt = new File(file.getAbsolutePath()+".txt");

		String line ="";
		String content ="";
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ocrTxt), "UTF-8"))){

			while ((line = reader.readLine()) != null)
			{			
				content += line.trim().toLowerCase()+"\n";
			}

			ocrTxt.delete();
			file.delete();
		}
		catch(Exception e){
			throw new RuntimeException(e.getMessage());
		}

		SubtitleFragment sf = new SubtitleFragment(number,timeStamp,content);
		return sf;
	}

	/**
	 * Turn as many non-white pixels as possible black to make ocr result as good as possible
	 * @param file Image to edit
	 * @param resources
	 */
	private static void editSon(File file, ResourceLinks resources) {
		String commandline = resources.getConvert()+" -black-threshold 70% "+file.getAbsolutePath();
		//log.debug("Running commandline: {}",commandline);
		ProcessRunner pr1 = new ProcessRunner("bash","-c",commandline);
		pr1.run(); 

		//String StringOutput1 = pr1.getProcessOutputAsString();
		//String StringError1 = pr1.getProcessErrorAsString();
		//log.debug(StringOutput1);
		//log.debug(StringError1);
	}
}
