package dk.statsbiblioteket.subtitleProject;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * Created by abr on 15-02-16.
 */
public class SRTGeneratorTest {

    @org.junit.Test
    public void testSmall() throws Exception {
        SubtitleProject.main(new String[]{"-i","small.ts"});
    }
}