package subtitleProject;

import java.util.ArrayList;

public class TransportStreamInfo extends StreamInfo{
	private String programNo;
	private String serviceName;
	private ArrayList<String> subtitleStreams;
	
	public TransportStreamInfo(String programNo, String service_name, String videoStreamInfo, ArrayList<String>subStreams) {
		super(videoStreamInfo);
		this.programNo = programNo;
		this.serviceName = service_name;
		this.subtitleStreams = subStreams;
	}
	
	public String getProgramNo() {
		return programNo;
	}
	public void setProgramNo(String programNo) {
		this.programNo = programNo;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public void addSubtitleStream(String subtitleStream) {
		subtitleStreams.add(subtitleStream);
	}
	public ArrayList<String> getSubtitleStreams() {
		return subtitleStreams;
	}
	
	@Override
	public String toString() {
		String dvdStreamInfo = "contains no subtitleStream";
		if(!subtitleStreams.isEmpty()){
			dvdStreamInfo = "";
			for(String s: subtitleStreams){
				dvdStreamInfo += s+"\n";
			}
		}
		return "StreamInfo: ProgramNo = " + programNo + "\n serviceName = "
				+ serviceName + "\n videoStreamDetails = " + super.getVideoStreamDetails()
				+ "dvbSubStream = " + dvdStreamInfo+"\n";
	}
}