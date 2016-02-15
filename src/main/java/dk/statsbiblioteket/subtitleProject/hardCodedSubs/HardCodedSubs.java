package dk.statsbiblioteket.subtitleProject.hardCodedSubs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.statsbiblioteket.subtitleProject.common.SubtitleFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.subtitleProject.SubtitleProject;
import dk.statsbiblioteket.subtitleProject.common.NoSubsException;
import dk.statsbiblioteket.subtitleProject.common.OCR;
import dk.statsbiblioteket.subtitleProject.common.ResourceLinks;
import dk.statsbiblioteket.subtitleProject.nonStreamed.MpegWmvStreamInfo;
import dk.statsbiblioteket.subtitleProject.transportStream.TransportStreamInfo;
import dk.statsbiblioteket.util.console.ProcessRunner;

/**
 * Class to extract subtitles by ocr of frames taken every second.
 */
public class HardCodedSubs {
	private static Logger log = LoggerFactory.getLogger(HardCodedSubs.class);
    private ResourceLinks resources;
    private ExecutorService executorService;
    //private static Set<String> dict;


    public HardCodedSubs(ResourceLinks resources, ExecutorService executorService) {
        this.resources = resources;
        this.executorService = executorService;
    }

    /**
	 * Gather png files associated to the srtpath and run ocr. Result sent to srt.
	 * @param srtPath to get the right png files.
	 * @return 1 if subtitles is detected, else 0
	 * @throws IOException if no images is found
	 * @throws FileNotFoundException if srtfile doesn't exist
	 * @throws UnsupportedEncodingException if UTF-8 isn't supported
	 */
	private int pngToSRT(File srtPath)
            throws IOException, FileNotFoundException,
            UnsupportedEncodingException {
		File[] files = new File(resources.getTemp()).listFiles(getPngFilter(srtPath));


		if (files == null){
			throw new IOException("No images found");
		} else {
			Arrays.sort(files);
		}

		Subtitle subtitleFragments = new Subtitle();

        Set<Future<SubtitleFragment>> fragments = new LinkedHashSet<>();
 		for (final File file : files) {
            fragments.add(executorService.submit(new Callable<SubtitleFragment>() {
                @Override
                public SubtitleFragment call() throws Exception {
                    Thread.currentThread().setName("OCR-"+file.getName());
                    return OCR.ocrFrame(file, resources);
                }
            }));
		}
        for (Future<SubtitleFragment> fragment : fragments) {
            try {
                subtitleFragments.add(fragment.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException(e.getCause());
            }
        }

		int succes = 0;
		try {
			String srtContent = subtitleFragments.format();
			succes = writeSRTFile(srtPath, srtContent);
		} catch (NoSubsException e){
			log.info("{} (No hardcoded subs detected)",srtPath.getAbsolutePath());
		}

		return succes;
	}

	/**
	 * Based on streamInfo instance, the recording framesize is calculated (buttom 20%), and then recorded. 3600 frames will be recorded, one for each second in a hour.
	 * @param file ts-file to search for hardcoded subtitle
	 * @param srtPath srt file to contain upcoming subtitles
	 * @param localtsContent info of current program
	 * @throws NumberFormatException if filename convention has changed since testing, and trying to parse letters to numbers
	 */
	private void extractPngFilesFromTS(File file, File srtPath,
			TransportStreamInfo localtsContent)
            throws NumberFormatException, IOException {
		String[] videoInfo = localtsContent.getVideoStreamDetails().trim().split(" ");
		String frameSize = "";
		for (String aVideoInfo : videoInfo) {
			if (Pattern.matches("^\\d{2,}x\\d{2,}$", aVideoInfo)) {
				frameSize = aVideoInfo;
			}
		}
		String[] frameSizeSplit = frameSize.split("x");
		int yOffset = Integer.parseInt(frameSizeSplit[frameSizeSplit.length-1]);
		//variable to contain there to record from on y
		int yFrameSize = yOffset;
		yOffset = (yOffset/100)*80;
		yFrameSize = Integer.parseInt(frameSizeSplit[1])-yOffset;
		frameSize = frameSizeSplit[0]+"x"+yFrameSize;
		String recordedFrames = "3600";
		Pattern p = Pattern.compile("\\#(.*?)\\[");
		Matcher m = p.matcher(localtsContent.getVideoStreamDetails());
		String pid ="";
		while (m.find()) {
			pid = m.group(1);
		}
        log.info("Extracting 1 frame per second, at most {} frames, as png in {}",recordedFrames,resources.getTemp());
        log.debug("Extracting frames of {}, yOffset: {} (total framsize: {}x{})",frameSize,yOffset,frameSizeSplit[0],frameSizeSplit[1]);
        String commandline = resources.getFfmpeg()+" -i "+file.getAbsolutePath()+" -r 1 -s "+frameSize +" -vf crop="+frameSizeSplit[0]+":"+yFrameSize+":0:"+yOffset+" -an -y -vframes "+recordedFrames+" -map "+pid + " "+resources.getTemp()+srtPath.getName()+"%04d.png";
		log.debug("Running commandline: {}",commandline);
		ProcessRunner pr = new ProcessRunner("bash","-c",commandline);
		pr.run();
        if (pr.getReturnCode() != 0){
            throw new IOException("Failed to run '"+commandline+"', got '"+pr.getProcessErrorAsString());
        }

        //String StringOutput = pr.getProcessOutputAsString();
		//String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);
	}

	/**
	 * Based on streamInfo instance, the recording framesize is calculated (buttom 20%), and then recorded. the frameamount is calculated based on inputStreamn.
	 * @param file ts-file to search for hardcoded subtitle
	 * @param srtPath srt file to contain upcoming subtitles
	 * @param localtsContent info of current program
	 * @throws NumberFormatException if filename convention has changed since testing, and trying to parse letters to numbers
	 */
	private void extractPngFromNonTS(File file, File srtPath,
			MpegWmvStreamInfo localtsContent)
            throws NumberFormatException, IOException {
		//Calculation amount of frames to record
		String duration = localtsContent.getDuration();
		//Calculation framsize to record
		String[] videoStreamDetails = localtsContent.getVideoStreamDetails().trim().split(" ");
		String frameSize = "";
        for (String videoStreamDetail : videoStreamDetails) {
            if (Pattern.matches("^\\d{2,}x\\d{2,}$", videoStreamDetail)) {
                frameSize = videoStreamDetail;
            }
        }
		String[] frameSizeSplit = frameSize.split("x");
		int yOffset = Integer.parseInt(frameSizeSplit[frameSizeSplit.length-1]);
		int yFrameSize = yOffset;
		yOffset = (yOffset/100)*80;
		yFrameSize = Integer.parseInt(frameSizeSplit[1])-yOffset;
		frameSize = frameSizeSplit[0]+"x"+yFrameSize;
		String commandLine = resources.getFfmpeg()+" -i "+file.getAbsolutePath()+" -r 1 -s "+frameSize +" -vf crop="+frameSizeSplit[0]+":"+yFrameSize+":0:"+yOffset+" -an -y -vframes "+duration+" "+resources.getTemp()+srtPath.getName()+"%d.png";
		log.debug("Running OCR on: {} frames, yOffset: {} (total framsize: {}x{})",frameSize,yOffset,frameSizeSplit[0],frameSizeSplit[1]);
		log.debug("Running commandline: {}", commandLine);
		ProcessRunner pr = new ProcessRunner("bash","-c",commandLine);
		pr.run();
        if (pr.getReturnCode() != 0){
            throw new IOException("Failed to run '"+commandLine+"', got '"+pr.getProcessErrorAsString());
        }

        //String StringOutput = pr.getProcessOutputAsString();
		//String StringError = pr.getProcessErrorAsString();
		//log.debug(StringOutput);
		//log.debug(StringError);
	}

	/**
	 * Writes the formatted subtitle to the srtFile
	 * @param srtPath the srtfile
	 * @param srtContent the subtitles
	 * @return 1 the srt generated count
	 * @throws FileNotFoundException if srtFile doesn't exists
	 * @throws UnsupportedEncodingException if UTF-8 isn't supported
	 */
	private static int writeSRTFile(File srtPath, String srtContent)
			throws FileNotFoundException, UnsupportedEncodingException {
		try (PrintWriter writer = new PrintWriter(srtPath.getAbsolutePath(), "UTF-8");){
			writer.write(srtContent);
		}
		log.info("{} - (hardcoded subs detected... file generated)",srtPath.getAbsolutePath());
		return 1;
	}

	/**
	 * Extracts every png image associated with the name of the current srtFile
	 * @param file srtFile
	 * @return FilenameFilter with the accepted images
	 */
	private static FilenameFilter getPngFilter(final File file){
		return new  FilenameFilter() {
			public boolean accept(File directory, String filename) {
				return filename.endsWith(".png") && filename.contains(file.getName());
			}
		};
	}

    /**
     * Try to detect subtitles in picture using ocr
     *
     * @param path                   to videofile
     * @param srthardcodedSubsPath   file to write to
     * @param localtsContent         info of current stream
     * @return Number of srt files generated
     * @throws IOException                  if srt file doens't exist
     * @throws FileNotFoundException        if srt file doens't exist
     * @throws UnsupportedEncodingException if UTF-8 isn't supported
     */
    public int extract(File path,
                       File srthardcodedSubsPath,
                       TransportStreamInfo localtsContent) throws IOException,
            FileNotFoundException, UnsupportedEncodingException, ExecutionException, InterruptedException {
        extractPngFilesFromTS(path, srthardcodedSubsPath, localtsContent);

        int tempint = pngToSRT(srthardcodedSubsPath);
        if (tempint == 0) {
            log.info("{} (didn't detect enough valid text in content)", srthardcodedSubsPath.getAbsolutePath());
            srthardcodedSubsPath.delete();
        }
        return tempint;
    }

    /**
     * Try to detect subtitles in picture using ocr
     *
     * @param path                   to videofile
     * @param srthardcodedSubsPath   file to write to
     * @param localtsContent         info of current stream
     * @return Number of srt files generated
     * @throws IOException                  if srt file doens't exist
     * @throws FileNotFoundException        if srt file doens't exist
     * @throws UnsupportedEncodingException if UTF-8 isn't supported
     */
    public int extract(File path,
                       File srthardcodedSubsPath,
                       MpegWmvStreamInfo localtsContent) throws IOException,
            FileNotFoundException, UnsupportedEncodingException {
        extractPngFromNonTS(path, srthardcodedSubsPath, localtsContent);

        int tempint = pngToSRT(srthardcodedSubsPath);
        if (tempint == 0) {
            log.info("{} (didn't detect enough valid text in content)", srthardcodedSubsPath.getAbsolutePath());
            srthardcodedSubsPath.delete();
        }
        return tempint;
    }

}