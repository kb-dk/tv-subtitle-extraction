package subtitleProject;

/**
 * Class to hold given links either from properties file, or arguments (arguments will thrumpf properties) 
 * @author Jacob
 *
 */
public class ResourceLinks {

	private String input;
	private String output;
	private String ccextractor;
	private String ffprobe;
	private String ffmpeg;
	private String tesseract;
	private String projectx;
	private String convert;
	private String dict;
	private String teleIndex;
	private String tessConfig;
	private String projectXconfig;
	private String terminationTime;

	public ResourceLinks(String input, String output, String ccextractor,
			String ffprobe, String ffmpeg, String tesseract, String projectx,
			String convert, String dict, String teleIndex, String tessConfig,
			String projectXconfig, String terminationTime) {
		super();
		this.input = input;
		this.output = output;
		this.ccextractor = ccextractor;
		this.ffprobe = ffprobe;
		this.ffmpeg = ffmpeg;
		this.tesseract = tesseract;
		this.projectx = projectx;
		this.convert = convert;
		this.dict = dict;
		this.teleIndex = teleIndex;
		this.tessConfig = tessConfig;
		this.projectXconfig = projectXconfig;
		this.terminationTime = terminationTime;
	}
	public String getInput() {
		return input;
	}
	public void setInput(String input) {
		this.input = input;
	}
	public String getOutput() {
		return output;
	}
	public void setOutput(String output) {
		this.output = output;
	}
	public String getDict() {
		return dict;
	}
	public void setDict(String dict) {
		this.dict = dict;
	}
	public String getTeleIndex() {
		return teleIndex;
	}
	public void setTeleIndex(String teleIndex) {
		this.teleIndex = teleIndex;
	}
	public String getTessConfig() {
		return tessConfig;
	}
	public void setTessConfig(String tessConfig) {
		this.tessConfig = tessConfig;
	}
	public String getProjectXconfig() {
		return projectXconfig;
	}
	public void setProjectXconfig(String projectXconfig) {
		this.projectXconfig = projectXconfig;
	}
	public String getTerminationTime() {
		return terminationTime;
	}
	public void setTerminationTime(String terminationTime) {
		this.terminationTime = terminationTime;
	}	
	public String getCcextractor() {
		return ccextractor;
	}
	public void setCcextractor(String ccextractor) {
		this.ccextractor = ccextractor;
	}
	public String getFfprobe() {
		return ffprobe;
	}
	public void setFfprobe(String ffprobe) {
		this.ffprobe = ffprobe;
	}
	public String getFfmpeg() {
		return ffmpeg;
	}
	public void setFfmpeg(String ffmpeg) {
		this.ffmpeg = ffmpeg;
	}
	public String getTesseract() {
		return tesseract;
	}
	public void setTesseract(String tesseract) {
		this.tesseract = tesseract;
	}
	public String getProjectx() {
		return projectx;
	}
	public void setProjectx(String projectx) {
		this.projectx = projectx;
	}
	public String getConvert() {
		return convert;
	}
	public void setConvert(String convert) {
		this.convert = convert;
	}
	public String toString(){
		return "inputfile: "+input+"\n"+"outpuntDest: "+output+"\n"+"DictionaryPath: "+dict+"\n"+"TeletextIndexPath: "+teleIndex+"\n"+"tesseract: "+tesseract+"\ntessConfigfilepath: "+tessConfig+"\n"+"projectx: "+projectx+"\nprojectXconfig: "+projectXconfig+"\n"+"ffprobe:"+ffprobe+"\nffmpeg: "+ffmpeg+"\nconvert: "+convert+"\nccextractor: "+ccextractor+"\nterminationtime: "+terminationTime+"\n";
	}
}
