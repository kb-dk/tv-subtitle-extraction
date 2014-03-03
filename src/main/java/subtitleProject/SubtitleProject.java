package subtitleProject;

import java.io.File;
import java.io.FileInputStream;
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

public class SubtitleProject {
	private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);

	public static void main(String[] args) throws IOException, Exception {
		//TODO be able to print log-file
		String properties ="";
		String input ="";
		String output="";
		String dict="";
		String teleIndex="";
		String tessConfig="";
		String projectXconfig="";
		String terminationTime="";
		
		for(int i = 0;i<args.length; i++){
			if(args[i].equalsIgnoreCase("-i")){
				i++;
				input = args[i];
			}
			if(args[i].equalsIgnoreCase("-o")){
				i++;
				output = args[i];
			}
			if(args[i].equalsIgnoreCase("-dict")){
				i++;
				dict = args[i];
			}
			if(args[i].equalsIgnoreCase("-teleIndex")){
				i++;
				teleIndex = args[i];
			}
			if(args[i].equalsIgnoreCase("-tessconfig")){
				i++;
				tessConfig = args[i];
			}
			if(args[i].equalsIgnoreCase("-p")){
				i++;
				properties = args[i];
			}
			if(args[i].equalsIgnoreCase("-projectXconfig")){
				i++;
				projectXconfig = args[i];
			}
			if(args[i].equalsIgnoreCase("-terminationTime")){
				i++;
				terminationTime = args[i];
			}
		}
		
		Properties prop = new Properties();
		if(properties==null||properties.equals("")){
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
		
		if(dict==null||dict.equalsIgnoreCase("")){
			prop.getProperty("dict");
			if(dict==null||dict.equalsIgnoreCase("")){
				dict="var/dictv2.txt";
			}
		}
		
		if(teleIndex==null||teleIndex.equalsIgnoreCase("")){
			prop.getProperty("teleTextIndexPath");
			if(teleIndex==null||teleIndex.equalsIgnoreCase("")){
				teleIndex="var/TeletextIndexes.xml";
			}
		}
		
		if(tessConfig==null||tessConfig.equalsIgnoreCase("")){
			prop.getProperty("tesseractConfigPath");
			if(tessConfig==null||tessConfig.equalsIgnoreCase("")){
				tessConfig="var/Tesseractconfigfile.txt";
			}
		}
		
		if(projectXconfig==null||projectXconfig.equalsIgnoreCase("")){
			prop.getProperty("projectXIniPath");
			if(projectXconfig==null||projectXconfig.equalsIgnoreCase("")){
				projectXconfig="var/Project-X_0.91.0/X.ini";
			}
		}
		
		if(terminationTime==null||terminationTime.equalsIgnoreCase("")){
			prop.getProperty("HoursBeforeTermination");
			if(terminationTime==null||terminationTime.equalsIgnoreCase("")){
				terminationTime="1000";
			}
		}
		
		//TODO create resourceLinks with new variables
		ResourceLinks resources = new ResourceLinks(input, output, dict, teleIndex, tessConfig, projectXconfig, terminationTime);
		log.debug("resourcepaths: "+resources.toString());
		int returnCode = 0;
		try {
//			Properties properties = new Properties();
//			properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("SubtitleProject.properties"));

			searchInDirectory(resources);
		} catch (Throwable t){
			log.error("Caught Exception while working",t);
			returnCode  = 1;
		} finally {
			log.debug("Returning with return code {}",returnCode);
			System.exit(returnCode);
		}
	}

	/**
	 * Gather all of the transtportStream files in the directory and calls SRTGenerator with the individual paths
	 * @param properties
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException 
	 */
	@SuppressWarnings("unused")
	private static void searchInDirectory(ResourceLinks resources) throws IOException, InterruptedException, ExecutionException {
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
			
		}
		log.info("Total srt files generated: {}",result.get());
	}
}
