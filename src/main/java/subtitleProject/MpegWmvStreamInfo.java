package subtitleProject;

/**
 * Extended StreamInfo class to handle additional mpeg or wmv info 
 */
public class MpegWmvStreamInfo extends StreamInfo{

	private String duration;

	public MpegWmvStreamInfo(String videoStreamDetails, String duration) {
		super(videoStreamDetails);
		this.duration = duration;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}
}
