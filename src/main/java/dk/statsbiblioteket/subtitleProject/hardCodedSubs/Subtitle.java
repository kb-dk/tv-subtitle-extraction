package dk.statsbiblioteket.subtitleProject.hardCodedSubs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dk.statsbiblioteket.subtitleProject.common.NoSubsException;
import dk.statsbiblioteket.subtitleProject.common.SubtitleFragment;

public class Subtitle {

	List<SubtitleFragment> subtitleFragments;

	public Subtitle(){
		subtitleFragments = new ArrayList<>();
	}

	public void add(SubtitleFragment fragment){
		subtitleFragments.add(fragment);
	}

	/**
	 * Remove duplicate subtitles and format them as SRT
	 * @return the SRT formatted subtitles
	 * @throws NoSubsException if no subtitles was found
	 */
	public String format() throws NoSubsException{
		List<SubtitleFragment> fragments = new ArrayList<>(subtitleFragments);
		Collections.sort(fragments);
		removeDuplicates(fragments);
		int counter = 1;
		StringBuilder srt = new StringBuilder();
		//String srtContent ="";
		for(int i =0;i<fragments.size();i++){
			if(fragments.get(i).haveContent()){
				srt.append(counter+"\n");
				srt.append(fragments.get(i).toString());
				counter++;
			}
		}
		if (counter == 1){
			throw new NoSubsException();
		}
		return srt.toString();
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
				}
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
