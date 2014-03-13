package subtitleProject;

public class Test {

	public static void main(String[] args) {
		isSame("den hjælpe de forbandede danskere er her igen","hzælm de forbandede danskere er her igen",true);
		isSame("z âal emernsxnet ligesom sidste an æ i","skål vñjernsxnet ligesom sidste åü", true);
		isSame("ul i s ja a§le er igen kommet s til deres fødselsdag miss sophy i e i å","ul i s ja alle er igen kommet › til deres fødselsdag miss sophy i e t i ä", true);
		isSame("ä mr pommeroy har e placeret her","og min kære vgå mr winterbottomâ", false);
		isSame("tak m§s sophy y f det hele står klar","æ i tak miss åoæ det hele står klar", true);
		isSame("følger vi samme fremgangsmåde e j s] som sidste åü r i ei c §","samme fremgangsmåde som hved år james ja", false);
		isSame("der gør at det er nemt at finde med ultraviolet lys","skorpioner lever kun i de tørreste egne pa jorden", false);
		isSame("z i i w de kalklare gg i over et år uden føde eller vand i","å l de kan klare sig i over et år uden føde eller van« i", true);
		isSame("Olsen, hva' fanden laver du?","Jeg spiller atonalt.", false);
		isSame("skorpionen er så følsom 	    § 	    ix", "á   t skorplonen er så følsom",true);
		isSame("hello world","hello         world",true);

	}

	public static boolean isSame(String s, String t, boolean expected){
		boolean same = false;
		s = s.replaceAll("\\s+", " ");
		t = t.replaceAll("\\s+", " ");
		double minLength = s.length();
		if(minLength>t.length()){
			minLength = t.length();
		}
		double result = minLength/levenshteinDistance(s.toCharArray(), t.toCharArray());
		if(result>2.5){
			same = true;
		}
		System.out.println("'"+s+"' vs. '"+t+"' ("+minLength+") calculated result = "+result+" assumed identical = "+same+" (expected: "+expected+")");
		return same;
	}

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
