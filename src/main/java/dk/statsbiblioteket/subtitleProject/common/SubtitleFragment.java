package dk.statsbiblioteket.subtitleProject.common;

/**
 * Class to contain ocr result of single frame and a timestamp
 */
public class SubtitleFragment implements Comparable<SubtitleFragment>{
	int no;
	String timestamp;
	String content;

	public SubtitleFragment(int no, String timestamp, String content) {
		this.no = no;
		this.timestamp = timestamp;
		this.content = content;
	}
	public int getNo() {
		return no;
	}
	public void setNo(int no) {
		this.no = no;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}

	public String toString(){
		String outPut = timestamp + "\n" +clean(content)+"\n\n";
		return outPut;
	}

	private String clean(String content) {
		return content.trim().replaceAll("(^|\n) *. *($|\n)","").replaceAll("\n{2,}","\n").trim();
	}

	public boolean haveContent(){
		return !clean(content).replaceAll("\\s*","").isEmpty();
	}

	public int compareTo(SubtitleFragment o) {
		return this.no-o.no;
	}
}
