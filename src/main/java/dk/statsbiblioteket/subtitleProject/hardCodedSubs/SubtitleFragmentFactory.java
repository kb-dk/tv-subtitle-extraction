package dk.statsbiblioteket.subtitleProject.hardCodedSubs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import dk.statsbiblioteket.subtitleProject.common.ResourceLinks;
import dk.statsbiblioteket.subtitleProject.common.SubtitleFragment;

public class SubtitleFragmentFactory {
	
	private static Set<String> dict;

	public static SubtitleFragment createSubtitleFragment(int no, String content ,ResourceLinks resources) throws IOException{
		String timeStamp = timestampFromNo(no);
		if(!validText(content, resources)){
			content = "";
		}
		SubtitleFragment sf = new SubtitleFragment(no,timeStamp,content);
		return sf;
	}
	
	/**
	 * Using binary search to search a word
	 * @param resources resources
	 * @throws IOException if no dictionary file is found
	 */
	private static void populateDic(ResourceLinks resources) throws IOException{
		if(dict==null){
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(resources.getDict())),"UTF-8"))){
				dict = new HashSet<>();
				String tmp = "";
				while ((tmp = reader.readLine()) != null)
				{
					dict.add(tmp.trim());
				}
			}
		}
	}
	
	/**
	 * populates the dictionary and checks every word, if 50% of content exists, the subtitle is assumed to be valid
	 * @param line to check content of
	 * @param resources resources
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
			s=s.trim();
			validCount += (dict.contains(s) ? 1 : 0 );
		}

		if(total>1 && validCount>=(total/2)){
			valid = true;
		}

		return valid;
	}
	
	/**
	 * Generates a srt timestamp based on number (second)
	 * @param no Frame number, starting from 1, when played with 1 frame per second
	 * @return timestamp ex 00:00:00,000 --> 00:00:001,000
	 * @throws NumberFormatException
	 */
	private static String timestampFromNo(int no) throws NumberFormatException {
		no = no -1;
		int min = 0;
		int sec = 0;
		int hour = 0;
		//If more than 3600 seconds generates hours
		if(no>3600){
			hour = no/3600;
			//Remove hours in seconds in no-variable
			no = no - (hour*3600);
		}
		//If more than 60 seconds generates minutes
		if(no>59){
			min = no/60;
			//Remove minutes in seconds in no-variable
			no = no - (min*60);
		}
		sec = no;
		//int minInt = (int)min;
		//Converts to string. If only one char, a zero is added in front so protocol is kept
		String hourString = ""+hour;
		if(hourString.length()==1){
			hourString = "0".concat(hourString);
		}
		String minString = ""+min;
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

		String timeStamp= hourString+":"+minString+":"+secString+",000 --> "+hourString+":"+minStringAfter+":"+secStringAfter+",000";
		return timeStamp;
	}
}
