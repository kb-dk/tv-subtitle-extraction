package dk.statsbiblioteket.subtitleProject.teletext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses through xml file via SAXParser and makes a list with teletext subtitlepages and timestamps for each list
 */
public class TeletextIndexes extends DefaultHandler {

	private List<TimePeriod> timePeriods;
	private String xmlPath;

	private TimePeriod timePtmp; 
	String tmpValue;

	public TeletextIndexes(String xmlPath) {
		super();
		timePeriods = new ArrayList<TimePeriod>();
		this.xmlPath = xmlPath;
		parseDocument();
		Collections.sort(timePeriods);
	}

	public List<TimePeriod> getTimePeriods(){
		return timePeriods;
	}

	/**
	 * Method to start parsing xml
	 */
	private void parseDocument() {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser parser = factory.newSAXParser();
			parser.parse(xmlPath, this);
		} catch (ParserConfigurationException e) {
			System.out.println("ParserConfig error");
		} catch (SAXException e) {
			System.out.println("SAXException : xml not well formed");
		} catch (IOException e) {
			System.out.println("IO error");
		}
	}

	/**
	 * Creates a new timePeriod to temp if a startTag of timePeriod is met
	 */
	@Override
	public void startElement(String s, String s1, String elementName, Attributes attributes) throws SAXException {
		if (elementName.equalsIgnoreCase("timePeriod")) {
			timePtmp = new TimePeriod();
		}
	}

	/**
	 * Based on reached tag, put value where it belongs 
	 */
	@Override
	public void endElement(String s, String s1, String element) throws SAXException {
		if (element.equals("timePeriod")) {
			timePeriods.add(timePtmp);
		}
		if (element.equalsIgnoreCase("airDay")) {
			timePtmp.setAirDay(tmpValue);
		}
		if (!element.equalsIgnoreCase("teletextIndexes")||!element.equalsIgnoreCase("/timePeriod")||!element.equalsIgnoreCase("/teletextIndexes")||!element.equalsIgnoreCase("timePeriod")||!element.equalsIgnoreCase("airDay")){
			timePtmp.getIndexes().put(element, tmpValue);
		}
	}

	/**
	 * creates string based on value in xml
	 */
	@Override
	public void characters(char[] ac, int i, int j) throws SAXException {
		tmpValue = new String(ac, i, j);
	}
}


