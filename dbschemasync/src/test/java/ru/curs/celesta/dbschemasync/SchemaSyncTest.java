package ru.curs.celesta.dbschemasync;

import org.junit.jupiter.api.Test;
import ru.curs.celesta.CelestaException;
import ru.curs.celesta.score.Score;
import ru.curs.celesta.score.discovery.PyScoreDiscovery;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaSyncTest {
    @Test
    void celestaToDbs() throws Exception {
        String scorePath = getScorePath();
        Score s = new Score.ScoreBuilder()
                .path(scorePath)
                .scoreDiscovery(new PyScoreDiscovery())
                .build();
        File tmp = File.createTempFile("sst", "tmp");
        tmp.delete();
        try {
            Celesta2DBSchema.scoreToDBS(s, tmp);
            try (
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            new FileInputStream(tmp), StandardCharsets.UTF_8))) {
                assertTrue(br.readLine().contains("xml"));
                assertTrue(br.readLine().contains("project"));
            }
        } finally {
            tmp.delete();
        }
    }

    private String getScorePath() {
        return SchemaSyncTest.class
                    .getClassLoader().getResource("score").getFile();
    }

    @Test
    void dbsToCelesta() throws Exception {
        String dbs = SchemaSyncTest.class
                .getClassLoader().getResource("test.dbs").getFile();
        String scorePath = getScorePath();
        File adoc = new File(scorePath, "../Layout_.adoc");
        adoc.delete();
        assertFalse(adoc.exists());
        Score s = new Score.ScoreBuilder()
                .path(scorePath)
                .scoreDiscovery(new PyScoreDiscovery())
                .build();
        DBSchema2Celesta.dBSToScore(new File(dbs), s, true);
        assertTrue(adoc.exists());
    }

    @Test
    void bothWays() throws Exception {
        String scorePath = getScorePath();
        Score s = new Score.ScoreBuilder()
                .path(scorePath)
                .scoreDiscovery(new PyScoreDiscovery())
                .build();
        StringWriter old = new StringWriter();
        s.getGrain("logs").save(new PrintWriter(old));
        System.out.println(old);

        File tmp = File.createTempFile("sst", "tmp");
        tmp.delete();
        try {
            Celesta2DBSchema.scoreToDBS(s, tmp);
            DBSchema2Celesta.dBSToScore(tmp, s, false);
        } finally {
            tmp.delete();
        }
    }

}
