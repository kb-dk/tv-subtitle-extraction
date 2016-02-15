package dk.statsbiblioteket.subtitleProject.dvbsub;

import dk.statsbiblioteket.subtitleProject.common.ResourceLinks;
import dk.statsbiblioteket.subtitleProject.common.SubtitleFragment;
import dk.statsbiblioteket.subtitleProject.subtitleStream.SONHandler;
import dk.statsbiblioteket.subtitleProject.transportStream.TransportStreamInfo;
import dk.statsbiblioteket.util.console.ProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by abr on 15-02-16.
 */
public class DvbSub {

    boolean demuxed;
    private Map<String, List<SubtitleFragment>> dvbSubstreamMap;
    private ResourceLinks resources;
    private static Logger log = LoggerFactory.getLogger(DvbSub.class);


    public DvbSub(ResourceLinks resources) {
        this.resources = resources;
        dvbSubstreamMap = new HashMap<>();
        // bool to make sure projectX isn't called twice on same file..
        demuxed = false;

    }

    /**
     * Using ProjectX to demux transportStream so .son files will be generated
     * @param file to demux
     * @return a list of SubtitleFragments linked to the associated pid
     * @throws IOException if sonfiles doesn't exist
     */
    public Map<String, List<SubtitleFragment>> demuxFile(File file) throws IOException{
        Map<String, File> sonFiles = new HashMap<String, File>();
        String commandeLine = resources.getProjectx()+" -ini "+resources.getProjectXconfig() + " -log -demux "+file.getAbsolutePath();
        log.debug("Running commandline: {}", commandeLine);
        ProcessRunner pr = new ProcessRunner("bash", "-c", commandeLine);
        pr.run();
        if (pr.getReturnCode() != 0){
            throw new IOException("Failed to run '"+commandeLine+"', got '"+pr.getProcessErrorAsString());
        }
        //String StringOutput = pr.getProcessOutputAsString();
        //String StringError = pr.getProcessErrorAsString();
        //log.debug(StringOutput);
        //log.debug(StringError);
        detectPids(file, sonFiles);

        Map<String, List<SubtitleFragment>> subsToPids = SONHandler.sonHandler(sonFiles, resources);
        for (String s : sonFiles.keySet()) {
            removeUselessFiles(sonFiles.get(s));
        }
        return subsToPids;
    }

    /**
     * Iterate through log file to detect if there is valid output
     * @param file there has been demuxed
     * @param sonFiles projectedX has generated
     * @throws UnsupportedEncodingException if UTF-8 is unsupported
     * @throws FileNotFoundException if log file hsn't been generated
     * @throws IOException if log file hsn't been generated
     */
    private void detectPids(File file,
            Map<String, File> sonFiles)
                    throws UnsupportedEncodingException, FileNotFoundException,
                    IOException {
        String iniName = file.getName().replaceFirst("\\.ts$", "_log.txt");
        File projectXLog = new File(resources.getOutput() +iniName);
        //log.debug("ProjectX log: "+projectXLog.getAbsoluteFile());

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(projectXLog), "UTF-8"))){
            List<String> pids = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null)
            {
                if(line.contains("Subpict.:")){
                    line = reader.readLine();
                    while(line.startsWith("PID")){
                        String[] splitsplit = line.split(" ");
                        String temp = splitsplit[1];
                        splitsplit =temp.split("x");
                        temp = splitsplit[1];
                        Pattern p = Pattern.compile("^(.*?)\\(");
                        Matcher m = p.matcher(line);
                        while (m.find()) {
                            temp = m.group(1);
                        }
                        while(!temp.startsWith("0")){
                            temp = temp.substring(1);
                        }
                        pids.add(temp);
                        line = reader.readLine();
                    }
                    int i = 0;
                    while(i<pids.size()){
                        line = reader.readLine();
                        if(line.endsWith(".son")){
                            String[] temp = line.split(" ");
                            sonFiles.put(pids.get(i) ,new File(temp[temp.length-1]));
                            i++;
                            log.debug("found .son: {}", temp[temp.length - 1]);
                            //found = true;
                        }
                    }
                }
            }
        }
        projectXLog.delete();
    }

    /**
     * Removes files which is generated by ProjectX
     * @param son
     */
    private static void removeUselessFiles(File son){
        String supName = son.getAbsolutePath().replaceFirst("\\.son$", ".sup");
        String spfName = son.getAbsolutePath().replaceFirst("\\.son$", ".spf");
        String ifoName = son.getAbsolutePath().replaceFirst("\\.son$", ".sup.IFO");
        File f = new File(supName);
        log.debug("" + f.delete());
        f = new File(spfName);
        log.debug("" + f.delete());
        f = new File(ifoName);
        log.debug("" + f.delete());
        son.delete();
    }

    /**
     * Demux the file if not demuxed yet, and gathers the pids. If current stream contains extracted demuxed pid, srt file is generated
     *
     * @param path           to videofile
     * @param srtdvbSubPath  to write to
     * @param localtsContent info of current stream
     * @return number of generated srt files
     * @throws IOException                  if no srt-file or videofile exists
     * @throws FileNotFoundException        if no srt file exists
     * @throws UnsupportedEncodingException if UTF-8 isn't supported
     */
    public int extract(File path, File srtdvbSubPath,
                       TransportStreamInfo localtsContent)
            throws IOException, FileNotFoundException,
            UnsupportedEncodingException {
        if (!demuxed) {
            demuxed = true;
            log.info("Extracting subPictures from transportstream using ProjectX");
            dvbSubstreamMap = demuxFile(path);
            log.debug("amount of dvb_subtitle registred: {}", dvbSubstreamMap.keySet().size());
        }
        Iterator<String> it = dvbSubstreamMap.keySet().iterator();
        boolean match = false;
        int content = 0;
        while (it.hasNext() && !match) {
            String pid = it.next();
            for (String subtitleStreams : localtsContent.getSubtitleStreams()) {
                if (subtitleStreams.contains(pid)) {
                    match = true;
                    log.info("{} (content detected.. running ocr)", srtdvbSubPath.getAbsolutePath());
                    content += generateSrtFromSubtitleFragments(srtdvbSubPath, dvbSubstreamMap.get(pid));
                }
            }
        }
        if (!match) {
            log.info("{} (no content...)", srtdvbSubPath.getAbsolutePath());
            srtdvbSubPath.delete();
        }
        return content;
    }

    /**
     * Writes subtitleFragments to srt file according to the SRT protocol
     *
     * @param srtPath           to write to
     * @param subtitleFragments to write to srt
     * @return 1
     * @throws FileNotFoundException        if srtfile don't exists
     * @throws UnsupportedEncodingException if UTF-8 dont exists
     */
    private static int generateSrtFromSubtitleFragments(File srtPath, List<SubtitleFragment> subtitleFragments) throws FileNotFoundException, UnsupportedEncodingException {
        String srtContent = "";
        Collections.sort(subtitleFragments);
        for (SubtitleFragment subtitleFragment : subtitleFragments) {
            if (subtitleFragment.haveContent()) {
                srtContent += subtitleFragment.getNo() + "\n";
                srtContent += subtitleFragment.toString();
            }
        }
        try (PrintWriter writer = new PrintWriter(srtPath.getAbsolutePath(), "UTF-8")) {
            writer.write(srtContent);
        }
        log.info("{} - (substream subs detected... file generated)", srtPath.getAbsolutePath());
        return 1;
    }

}
