package subtitleProject;

import dk.statsbiblioteket.util.console.ProcessRunner;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new TeletextIndexes("C:/Users/jje/workspace/test1/src/main/resources/TeletextIndexes.xml");
		ProcessRunner pr = new ProcessRunner("cmd.exe","/C", "C:/Users/jje/Downloads/ffmpeg-20140115-git-785dc14-win64-static/bin/ffprobe.exe C:/Users/jje/Downloads/DigitaleFiler/Short/3SAT-20140116_160000.ts");
		pr.run();
		System.out.println(pr.getProcessErrorAsString());
		System.out.println(pr.getProcessOutputAsString());
	}

}
