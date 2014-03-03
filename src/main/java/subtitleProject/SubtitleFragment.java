package subtitleProject;

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
	@Override
	public int compareTo(SubtitleFragment arg0) {
		return this.no-arg0.no;
	}
	public String toString(){
		String outPut = timestamp + "\n" +content;
		return outPut;
	}
	public boolean haveContent(){
		boolean content = false;
		String[] temp = this.content.split("\n");
		for(int i = 0; i<temp.length&&!content;i++){
			temp[i].trim();
			if(!temp[i].equals("")){
				content = true;
			}
		}
		return content;
	}
}
