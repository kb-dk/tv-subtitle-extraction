package dk.statsbiblioteket.subtitleProject.teletext;

import dk.statsbiblioteket.subtitleProject.SRTGenerator;
import dk.statsbiblioteket.subtitleProject.common.ResourceLinks;
import dk.statsbiblioteket.subtitleProject.transportStream.TransportStreamInfo;
import dk.statsbiblioteket.util.console.ProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by abr on 15-02-16.
 */
public class TeleTextExtractor {

    private static Logger log = LoggerFactory.getLogger(TeleTextExtractor.class);

    private TeletextIndexes teletextIndexes;
    private ResourceLinks resources;
    private String outputFolder;
    private final static String executableDest = " -out=srt -o ";
    private final static String executabletelxPage = " -utf8 --nofontcolor -tpage ";


    public TeleTextExtractor(ResourceLinks resources) {
        this.resources = resources;
        this.outputFolder = resources.getOutput();
    }

    /**
     * Uses ccExtractor to detect subtitles from teletext, based on service name and programno in transportStream
     *
     *
     * @param videoFile
     * @param srtTeleTextPath to write to
     * @param transportStreamInfo  info about current stream
     * @param programNo       to process
     * @return 1 if teletextsubtitles is detected else 0
     * @throws Exception if no xmlpage is found
     */
    public int extract(File videoFile, File srtTeleTextPath, TransportStreamInfo transportStreamInfo, String programNo) throws Exception {

        // If no outputDestination descriped in config file, inputdest will be used
        String dest = executableDest;
        dest += srtTeleTextPath.getAbsolutePath();
        if (outputFolder.equals(videoFile.getParent()) || outputFolder.equals("")) {
            dest = "";
        }

        // Sets the teltextpage based on properties
        String telNo;
        try {
            telNo = getTeletextPage(transportStreamInfo, videoFile);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        String commandline =
                resources.getCcextractor() + " " + programNo + videoFile.getAbsolutePath() + dest + executabletelxPage +
                telNo;
        log.debug("Running commandline: {}", commandline);
        ProcessRunner pr = new ProcessRunner("bash", "-c", commandline);
        pr.run();
        if (pr.getReturnCode() != 0){
            throw new IOException("Failed to run '" + commandline + "', got '" + pr.getProcessErrorAsString());
        }

        //String StringOutput = pr.getProcessOutputAsString();
        //String StringError = pr.getProcessErrorAsString();
        //log.debug(StringOutput);
        //log.debug(StringError);
        int content = SRTGenerator.haveContent(srtTeleTextPath);
        return content;
    }

    /**
     * Gets teletext page based on service_name or filename
     *
     * @param streamInfo info about current stream
     * @param file       videofile to extract from
     * @return teletextPage to search on
     * @throws Exception if no xml file is found
     */
    private String getTeletextPage(TransportStreamInfo streamInfo, File file) throws Exception {
        if (teletextIndexes == null) {
            try {
                teletextIndexes = new TeletextIndexes(resources.getTeleIndex());
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }

        //Default page - Denmark
        String page = "399";
        String[] splitName = file.getName().split("-");

        String yy = splitName[1];
        String mm = splitName[2];
        String dd = splitName[3];

        boolean foundTimePeriod = false;
        int i = 0;
        TimePeriod currentTP = null;

        while (!foundTimePeriod && i < teletextIndexes.getTimePeriods().size()) {
            if (teletextIndexes.getTimePeriods().get(i).compareTo(new String[]{yy, mm, dd}) > 0) {
                foundTimePeriod = true;
                if (i != 0) {
                    i--;
                }
                currentTP = teletextIndexes.getTimePeriods().get(i);
            } else {
                i++;
            }
        }

        if (!foundTimePeriod) {
            currentTP = teletextIndexes.getTimePeriods().get(teletextIndexes.getTimePeriods().size() - 1);
        }

        Iterator<String> channels = currentTP.getIndexes().keySet().iterator();
        boolean found = false;
        String serviceNameString = streamInfo.getServiceName().replace(" ", "");

        while (!found && channels.hasNext()) {
            String tmpChannel = channels.next();
            if (tmpChannel.toLowerCase().contains(serviceNameString.toLowerCase())) {

                page = currentTP.getIndexes().get(tmpChannel);
                found = true;
            }
        }
        return page;
    }
}
