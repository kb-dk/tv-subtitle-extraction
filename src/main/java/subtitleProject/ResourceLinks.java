package subtitleProject;

public class ResourceLinks {

	private String input;
	private String output;
	private String dict;
	private String teleIndex;
	private String tessConfig;
	private String projectXconfig;
	private String terminationTime;

	public ResourceLinks(String input, String output, String dict,
			String teleIndex, String tessConfig, String projectXconfig, 
			String terminationTime) {
		super();
		this.input = input;
		this.output = output;
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
	
	public String toString(){
		return "inputfile: "+input+"\n"+"outpuntDest: "+output+"\n"+"DictionaryPath: "+dict+"\n"+"TeletextIndexPath: "+teleIndex+"\n"+"tessConfigfilepath: "+tessConfig+"\n"+"projectXconfig: "+projectXconfig+"\n"+"terminationtime: "+terminationTime+"\n";
	}
}
