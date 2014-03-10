package subtitleProject;

/**
 * abstract class to contain info about streams. Used by TransportStreamInfo and MpegWmvStreamInfo
 * @author Jacob
 *
 */
public abstract class StreamInfo {
	private String videoStreamDetails;

	public StreamInfo(String videoStreamDetails) {
		super();
		this.videoStreamDetails = videoStreamDetails;
	}

	public String getVideoStreamDetails() {
		return videoStreamDetails;
	}
	public void setVideoStreamDetails(String videoStreamDetails) {
		this.videoStreamDetails = videoStreamDetails;
	}
}
