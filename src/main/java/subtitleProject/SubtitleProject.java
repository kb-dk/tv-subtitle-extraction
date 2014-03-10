package subtitleProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class of SubtitleProject. gather external paths and starts the srt generator
 * @author Jacob
 *
 */
public class SubtitleProject {
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);

	/**
	 * The Main Method of SubtitleProject, gather resources and starts srt generator
	 * @param args arguments from commandline
	 * @throws IOException if properties isn't found
	 * @throws Exception if properties file isn't found 
	 */
	public static void main(String[] args) throws IOException, Exception {
		ResourceLinks resources = generateResources(args);
		log.debug("resourcepaths: "+resources.toString());
		int returnCode = 0;
		try {
			//			Properties properties = new Properties();
			//			properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("SubtitleProject.properties"));

			startSrtGenerator(resources);
		} catch (Throwable t){
			log.error("Caught Exception while working",t);
			returnCode  = 1;
		} finally {
			log.debug("Returning with return code {}",returnCode);
			System.exit(returnCode);
		}
	}

	/**
	 * Method to extract arguments and properties from SubtitleProject.properties. Variables used to generated resourcesLink file
	 * @param args from commandline
	 * @return ResourceLinks with path to all external tools and files
	 * @throws IOException if properties doens't exists
	 * @throws FileNotFoundException if properties doens't exists
	 */
	private static ResourceLinks generateResources(String[] args)
			throws IOException, FileNotFoundException {
		String properties ="";
		String input ="";
		String output="";
		String dict="";
		String teleIndex="";
		String tessConfig="";
		String projectXconfig="";
		String terminationTime="";
		String ccextractor ="";
		String ffprobe="";
		String ffmpeg="";
		String tesseract="";
		String projectx="";
		String convert="";

		for(int i = 0;i<args.length; i++){
			if(args[i].equalsIgnoreCase("-i")){
				i++;
				input = args[i];
			}
			if(args[i].equalsIgnoreCase("-o")){
				i++;
				output = args[i];
			}
			if(args[i].equalsIgnoreCase("-p")){
				i++;
				properties = args[i];
			}
		}

		Properties prop = new Properties();
		if(properties==null||properties.equals("")){
			//prop.load(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("SubtitleProject.properties")));
			prop.load(new InputStreamReader(new FileInputStream("var/SubtitleProject.properties")));
		}
		else{
			prop.load(new InputStreamReader(new FileInputStream(properties)));
		}

		if(input==null||input.equalsIgnoreCase("")){
			input = prop.getProperty("inputFile");
		}

		if(output==null||output.equalsIgnoreCase("")){
			output = prop.getProperty("outPutDirectory");
			if(output==null||output.equalsIgnoreCase("")){
				output = "output/";				
			}
		}

		dict = prop.getProperty("dict");
		if(dict==null||dict.equalsIgnoreCase("")){
			dict="var/dictv2.txt";
		}

		teleIndex = prop.getProperty("teleTextIndexPath");
		if(teleIndex==null||teleIndex.equalsIgnoreCase("")){
			teleIndex="var/TeletextIndexes.xml";
		}

		tessConfig = prop.getProperty("tesseractConfigPath");
		if(tessConfig==null||tessConfig.equalsIgnoreCase("")){
			tessConfig="var/Tesseractconfigfile.txt";
		}

		projectXconfig=prop.getProperty("projectXIniPath");
		if(projectXconfig==null||projectXconfig.equalsIgnoreCase("")){
			projectXconfig="var/X.ini";
		}

		terminationTime=prop.getProperty("HoursBeforeTermination");
		if(terminationTime==null||terminationTime.equalsIgnoreCase("")){
			terminationTime="1000";
		}
		
		ccextractor=prop.getProperty("ccextractorPath");
		if(terminationTime==null||terminationTime.equalsIgnoreCase("")){
			log.error("ccextractorPath is not defined");
		}
		
		ffmpeg=prop.getProperty("ffmpegPath");
		if(terminationTime==null||terminationTime.equalsIgnoreCase("")){
			log.error("ffmpegPath is not defined");
		}
		
		ffprobe=prop.getProperty("ffprobePath");
		if(terminationTime==null||terminationTime.equalsIgnoreCase("")){
			log.error("ffprobePath is not defined");
		}
		
		tesseract=prop.getProperty("tesseract");
		if(terminationTime==null||terminationTime.equalsIgnoreCase("")){
			log.error("tesseract is not defined");
		}
				
		convert=prop.getProperty("convertPath");
		if(terminationTime==null||terminationTime.equalsIgnoreCase("")){
			log.error("convertPath is not defined");
		}
		
		projectx=prop.getProperty("projectXPath");
		if(terminationTime==null||terminationTime.equalsIgnoreCase("")){
			log.error("projectXPath is not defined");
		}

		ResourceLinks resources = new ResourceLinks(input.trim(), output.trim(), ccextractor.trim(), ffprobe.trim(), ffmpeg.trim(), tesseract.trim(), projectx.trim(), convert.trim(), dict.trim(), teleIndex.trim(), tessConfig.trim(), projectXconfig.trim(), terminationTime.trim());
		return resources;
	}

	/**
	 * Calls SRTGenerator with the individual paths
	 * @param properties
	 * @throws InterruptedException if executerservice has run to long
	 * @throws ExecutionException if executorService isn't done
	 */
	@SuppressWarnings("unused")
	private static void startSrtGenerator(ResourceLinks resources) throws InterruptedException, ExecutionException {
		File file = new File(resources.getInput());
		if (file == null){
			log.error("No file found");
		}

		ExecutorService executorService = Executors.newFixedThreadPool(2);

		log.debug("Generates srt files");
		Future<Integer> result = executorService.submit(new SRTGenerator(file, resources));

		executorService.shutdown();
		executorService.awaitTermination(Integer.parseInt(resources.getTerminationTime()), TimeUnit.HOURS);

		while(!result.isDone()){
			//do nothing?
		}
		log.info("Total srt files generated: {}",result.get());
	}
}
