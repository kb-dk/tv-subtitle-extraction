package subtitleProject;

public class MpegWmvStreamInfo extends StreamInfo{

	private String duration;
	
	public MpegWmvStreamInfo(String videoStreamDetails, String duration) {
		super(videoStreamDetails);
		// TODO Auto-generated constructor stub
		this.duration = duration;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}
}
