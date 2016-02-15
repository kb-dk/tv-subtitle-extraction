package dk.statsbiblioteket.subtitleProject.teletext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jfree.util.Log;

/**
 * Class to contain teletext pages
 */
public class TimePeriod implements  Comparable<TimePeriod>{

	String airDay;
	Map<String, String> indexes;

	public TimePeriod() {
		super();
		indexes = new HashMap<String, String>();
	}

	public Map<String, String> getIndexes(){
		return indexes;
	}

	public String getAirDay() {
		return airDay;
	}

	public void setAirDay(String airDay) {
		this.airDay = airDay;
	}

	@Override
	public String toString(){
		String s = airDay.toString()+"\n";
		Iterator<String> channels = indexes.keySet().iterator();
		while(channels.hasNext()){
			String tmp = channels.next();
			s += tmp +": "+indexes.get(tmp)+"\n";
		}
		return s;
	}

	/**
	 * Compare method used for transportstream namesegment
	 * @param date String[] to compare
	 * @return comparison
	 */
	public int compareTo(String[] date) {
		String[] thisDate = this.airDay.split("-");
		String[] comDate = date;
		int i = 0;
		if(thisDate[0].compareTo(comDate[0])==0){
			if(thisDate[1].compareTo(comDate[1])==0){
				if(thisDate[2].compareTo(comDate[2])==0){
					Log.error("xml has timeperiods with equal stamps");
				}
				else if(thisDate[2].compareTo(comDate[2])<0){
					i=-1;
				}
				else{
					i=1;
				}
			}
			else if(thisDate[1].compareTo(comDate[1])<0){
				i=-1;
			}
			else{
				i=1;
			}
		}
		else if(thisDate[0].compareTo(comDate[0])<0){
			i=-1;
		}
		else{
			i=1;
		}
		return i;
	}

	/**
	 * Same as above but with TimePeriod as argument
	 * @param arg0 timePeriod to compare with
	 * @return comparison
	 */
	public int compareTo(TimePeriod arg0) {

		String[] thisDate = this.airDay.split("-");
		String[] comDate = arg0.getAirDay().split("-");
		int i = 0;
		if(thisDate[0].compareTo(comDate[0])==0){
			if(thisDate[1].compareTo(comDate[1])==0){
				if(thisDate[2].compareTo(comDate[2])==0){
					Log.error("xml has timeperiods with equal stamps");
				}
				else if(thisDate[2].compareTo(comDate[2])<0){
					i=-1;
				}
				else{
					i=1;
				}
			}
			else if(thisDate[1].compareTo(comDate[1])<0){
				i=-1;
			}
			else{
				i=1;
			}
		}
		else if(thisDate[0].compareTo(comDate[0])<0){
			i=-1;
		}
		else{
			i=1;
		}
		return i;
	}
}