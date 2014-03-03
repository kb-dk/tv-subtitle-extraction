package subtitleProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Testclass to search in srt-files
 * @author jje
 *
 */
public class SrtSearch {
	public static void main(String[] args) throws IOException {
		Properties properties = new Properties();

		properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("CCExtractor.properties"));
		ArrayList<String> results = search(properties);
		System.out.println("Hits: "+results.size()+"\n");
		for(String result: results){
		System.out.println(result);
		}
	}

	/**
	 * Iterate over every srt file in inputdirectory and search for a given word in config file
	 * @param properties
	 * @return a list with file names and timestamps which have the given searchWord
	 * @throws IOException
	 */
	private static ArrayList<String> search(Properties properties) throws IOException{
		ArrayList<String> results = new ArrayList<String>();
		String searchWord = properties.getProperty("searchWord");
		File[] files = new File(properties.getProperty("srtLocation")).listFiles(new FilenameFilter() {

			public boolean accept(File directory, String filename) {
				return filename.endsWith(".srt");
			}
		});

		if (files == null){
			//throws something
			return null;
		}

		BufferedReader reader = null;

		for(int i =0;i<files.length;i++){
			try { 
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(files[i]), "UTF-8"));

				searchWord = searchWord.toLowerCase();
				ArrayList<String> srtContent = new ArrayList<String>();
				String line;
				while ((line = reader.readLine()) != null)
				{
					line = line.toLowerCase();
					srtContent.add(line); 
					if(line.contains(searchWord)){
						int j = srtContent.size()-2;
						String resultNote = files[i].getName();
						boolean foundTime =false;
						while(!foundTime && j>=0){
							if(srtContent.get(j).equals("") || srtContent.get(j).equals(" ")){
								resultNote += " ("+srtContent.get(j+2)+")";
								foundTime =true;
							}
							j--;
						}
						if(!foundTime){
							resultNote += " ("+srtContent.get(1)+")";
						}
						results.add(resultNote);
					}
				}
			}

			finally {
				if(reader!=null){
					reader.close();
				}
			}
		}
		return results;
	}
}
