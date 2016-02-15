package dk.statsbiblioteket.subtitleProject;

import dk.statsbiblioteket.subtitleProject.common.ResourceLinks;
import dk.statsbiblioteket.subtitleProject.dvbsub.DvbSub;
import dk.statsbiblioteket.subtitleProject.hardCodedSubs.HardCodedSubs;
import dk.statsbiblioteket.subtitleProject.nonStreamed.MpegWmvStreamInfo;
import dk.statsbiblioteket.subtitleProject.teletext.TeleTextExtractor;
import dk.statsbiblioteket.subtitleProject.transportStream.TransportStreamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.type.UnknownTypeException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Generates srt-files. Deletes srt if no content is found.
 */
public class SRTGenerator {
    private static Logger log = LoggerFactory.getLogger(SubtitleProject.class);

    private final String executableProgramNo = " --program-number ";

    private final String outputFolder;
    private final ResourceLinks resources;
    private final DvbSub dvbSub;
    private final TeleTextExtractor teletext;
    private final ExecutorService executorService;
    private HardCodedSubs hardCodedSubs;


    public SRTGenerator(ResourceLinks resources, ExecutorService executorService) {
        this.executorService = executorService;
        this.outputFolder = resources.getOutput();
        this.resources = resources;
        dvbSub = new DvbSub(resources);
        teletext = new TeleTextExtractor(resources);
        hardCodedSubs = new HardCodedSubs(resources,executorService);
    }

    /**
     * Generate srt files for every program in transportStream
     *
     * @param videoFile to extract srt files from
     * @return The number of srt files there has been generated based on the single path
     * @throws Exception
     */
    public int generateFile(final File videoFile) throws Exception {
        List<Future<Integer>> numberOfSrt = new ArrayList<>();
        boolean transportStream = videoFile.getName().endsWith(".ts");

        if (transportStream) {
            log.info("TransportStream Detected");
            log.debug("Analyzing '{}'", videoFile.getAbsolutePath());
            final List<TransportStreamInfo> transportStreamContent = TransportStreamInfo.analyze(
                    videoFile.getAbsolutePath(),
                    resources);
            log.debug("ProgramCount: {}", transportStreamContent.size());

            String srtEnd = ".srt";

            for (int i = 0; i < transportStreamContent.size(); i++) {
                final TransportStreamInfo transportStreamInfo = transportStreamContent.get(i);
                String[] temp = transportStreamInfo.getProgramNo().split(" ");
                final String programNo = executableProgramNo + temp[temp.length - 1] + " ";

                String program = "_";
                for (String aTemp : temp) {
                    program += aTemp;
                }
                // More than one program in stream, have to generate multiple srt-files. Srt filenames based on programNo
                srtEnd = program;


                final File srtTeleTextPath = new File(outputFolder,
                                                      videoFile.getName().replaceFirst("\\.ts$",
                                                                                       srtEnd + "_teleText.srt"));


                final Future<Integer> teleTextCount = executorService.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        Thread.currentThread().setName("Teletext");
                        return teletext.extract(videoFile, srtTeleTextPath,
                                                transportStreamInfo,
                                                programNo);
                    }
                });
                numberOfSrt.add(teleTextCount);


                final File srthardcodedSubsPath = new File(outputFolder,
                                                           videoFile.getName().replaceFirst("\\.ts$", srtEnd +
                                                                                                      "_hardcodedSubs.srt"));
                Future<Integer> hardCodedCount = executorService.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        Thread.currentThread().setName("Hardcoded_Subs");

                        if (!transportStreamInfo.getVideoStreamDetails().contains("No Video Stream")) {
                            return hardCodedSubs.extract(videoFile,
                                                         srthardcodedSubsPath, transportStreamInfo);
                        } else {
                            srthardcodedSubsPath.delete();
                            return 0;
                        }
                    }
                });
                numberOfSrt.add(hardCodedCount);


                final File srtdvbSubPath = new File(outputFolder,
                                                    videoFile.getName().replaceFirst("\\.ts$",
                                                                                     srtEnd + "_dvbSub.srt"));
                Future<Integer> dvbSubCount = executorService.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        Thread.currentThread().setName("DVB_Subs");
                        if (!transportStreamInfo.getVideoStreamDetails().contains("No Video Stream")) {
                            int dvbSubCount = 0;
                            if (!transportStreamContent.isEmpty()) {
                                dvbSubCount = dvbSub.extract(videoFile, srtdvbSubPath,
                                                             transportStreamInfo);
                            } else {
                                log.info("{} has no dvb_substream", srtdvbSubPath.getAbsolutePath());
                                srtdvbSubPath.delete();
                            }
                            return dvbSubCount;
                        } else {
                            srtdvbSubPath.delete();
                            return 0;
                        }

                    }
                });
                numberOfSrt.add(dvbSubCount);

            }
        } else {
            numberOfSrt.add(executorService.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    Thread.currentThread().setName("NonTransportStream");
                    return generateSRTFromNonTs(videoFile);
                }
            }));
        }
        int sum = 0;
        for (Future<Integer> integerFuture : numberOfSrt) {
            sum += integerFuture.get();
        };
        return sum;

    }

    /**
     * If videofiletype is mpeg or wmv, hardcoded subs will try to be detected
     *
     * @param videoFile        to videofile
     * @return 1 if content detected and srt has been generated, else 0
     * @throws IOException
     * @throws UnknownTypeException
     */
    private int generateSRTFromNonTs(File videoFile)
            throws IOException, UnknownTypeException {
        String[] fileName = videoFile.getName().split("\\.");
        String type = fileName[fileName.length - 1];
        log.info("{} Detected", type);
        int numberOfSrt = 0;
        if (type.equalsIgnoreCase("wmv") || type.equalsIgnoreCase("mpeg")) {
            log.debug("Analyzing {}", videoFile.getAbsolutePath());
            MpegWmvStreamInfo MpegWmvStreamContent = MpegWmvStreamInfo.analyze(videoFile.getAbsolutePath(), resources);
            File srthardcodedSubsPath;
            if (videoFile.getName().endsWith(".mpeg")) {
                srthardcodedSubsPath = new File(outputFolder,
                                                videoFile.getName().replaceFirst("\\.mpeg$", "_hardcodedSubs.srt"));
            } else {
                srthardcodedSubsPath = new File(outputFolder,
                                                videoFile.getName().replaceFirst("\\.wmv$", "_hardcodedSubs.srt"));
            }
            int tempValue = hardCodedSubs.extract(videoFile,srthardcodedSubsPath, MpegWmvStreamContent);
            numberOfSrt += tempValue;
        } else {
            log.error("{} is not supported", type);
            throw new UnknownTypeException(null, null);
        }
        return numberOfSrt;
    }

    /**
     * Checks if there is content in the new srt file, if not the file is deleted
     *
     * @param srtFile srt file to parse
     * @return 1 if content is found, 0 if empty
     * @throws IOException if no srt file exists
     */
    public static int haveContent(File srtFile) throws IOException {
        int lineCount = 0;
        int emptyLineCount = 0;
        String deleteNote = "";
        int content = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(srtFile), "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] srtContent = line.toLowerCase().trim().split(" ");
                lineCount++;
                if (srtContent.length <= 1) {
                    emptyLineCount++;
                }

            }
        }

        if (emptyLineCount == lineCount) {
            deleteNote = " (no Content...)";
            srtFile.delete();
        } else {
            deleteNote = " (Content Detected)";
            content = 1;
        }

        log.info("{} {}", srtFile.getAbsolutePath(), deleteNote);
        return content;
    }
}

