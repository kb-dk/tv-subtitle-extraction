package dk.statsbiblioteket.subtitleProject;

import dk.statsbiblioteket.subtitleProject.common.ResourceLinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Main class of SubtitleProject. gather external paths and starts the srt generator
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
		log.debug("resourcepaths: {}",resources.toString());
		int returnCode = 0;
		try {
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
		String temp="";
		String dict = "";
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
			prop.load(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("SubtitleProject.properties")));
		}
		else{
			prop.load(new InputStreamReader(new FileInputStream(properties)));
		}

		if(input==null||input.equalsIgnoreCase("")){
			input = prop.getProperty("inputFile");
		}

		if(output==null||output.equalsIgnoreCase("")){
			output = prop.getProperty("outPutDirectory","output/");
		}
		if(temp==null||temp.equalsIgnoreCase("")){
			temp = prop.getProperty("tempDirectory","/tmp/");
		}


		dict = prop.getProperty("dict",Thread.currentThread().getContextClassLoader().getResource("dictv2.txt").getPath());

		teleIndex = prop.getProperty("teleTextIndexPath",Thread.currentThread().getContextClassLoader().getResource("TeletextIndexes.xml").getPath());

		tessConfig = prop.getProperty("tesseractConfigPath",Thread.currentThread().getContextClassLoader().getResource("Tesseractconfigfile.txt").getPath());

		projectXconfig=prop.getProperty("projectXIniPath",Thread.currentThread().getContextClassLoader().getResource("X.ini").getPath());

		terminationTime=prop.getProperty("HoursBeforeTermination","1000");
		if(terminationTime==null||terminationTime.equalsIgnoreCase("")){
			terminationTime="1000";
		}

		ccextractor=prop.getProperty("ccextractorPath");
		if(terminationTime.isEmpty()){
			log.error("ccextractorPath is not defined");
		}

		ffmpeg=prop.getProperty("ffmpegPath");
		if(terminationTime.isEmpty()){
			log.error("ffmpegPath is not defined");
		}

		ffprobe=prop.getProperty("ffprobePath");
		if(terminationTime.isEmpty()){
			log.error("ffprobePath is not defined");
		}

		tesseract=prop.getProperty("tesseract");
		if(terminationTime.isEmpty()){
			log.error("tesseract is not defined");
		}

		convert=prop.getProperty("convertPath");
		if(terminationTime.isEmpty()){
			log.error("convertPath is not defined");
		}

		projectx=prop.getProperty("projectXPath");
		if(terminationTime.isEmpty()){
			log.error("projectXPath is not defined");
		}

		return new ResourceLinks(input, output, temp, ccextractor, ffprobe,
								 ffmpeg, tesseract, projectx, convert,
								 dict, teleIndex, tessConfig,
								 projectXconfig, terminationTime);
	}

	/**
	 * Calls SRTGenerator with the individual paths
	 * @param resources paths and configs
	 * @throws InterruptedException if executerservice has run to long
	 * @throws ExecutionException if executorService isn't done
	 */
	@SuppressWarnings("unused")
	private static void startSrtGenerator(final ResourceLinks resources) throws InterruptedException, ExecutionException, TimeoutException {
		final File file = new File(resources.getInput());
		if (file == null){
			log.error("No file found");
		}

		final ExecutorService mainThread = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());;

		log.debug("Generates srt files");
		Future<Integer> result = mainThread.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				SRTGenerator generator = new SRTGenerator(resources,mainThread);
				return generator.generateFile(file);
			}
		});
		Integer numberOfSrtFiles = result.get(Integer.parseInt(resources.getTerminationTime()), TimeUnit.HOURS);
		mainThread.shutdownNow();

		log.info("Total srt files generated: {}",numberOfSrtFiles);
	}
}
