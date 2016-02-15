package dk.statsbiblioteket.subtitleProject;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * Created by abr on 15-02-16.
 */
public class SRTGeneratorTest {

    @org.junit.Test
    public void testSmall() throws Exception {
        SubtitleProject.main(new String[]{"-i","tv3_yousee.1397253600-2014-04-12-00.00.00_1397257200-2014-04-12-01.00.00_yousee2.ts"});
    }
}