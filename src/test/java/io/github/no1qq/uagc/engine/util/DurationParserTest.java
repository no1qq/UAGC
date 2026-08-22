package io.github.no1qq.uagc.engine.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DurationParserTest {

    @Test
    void parsesCompoundDurations() {
        assertEquals(Duration.ofMinutes(90L), DurationParser.parse("1h30m", null));
        assertEquals(Duration.ofDays(7L), DurationParser.parse("1w", null));
        assertEquals(Duration.ofSeconds(45L), DurationParser.parse("45s", null));
    }

    @Test
    void treatsPermanentAsNull() {
        assertNull(DurationParser.parse("permanent", Duration.ofDays(1L)));
        assertNull(DurationParser.parse("perm", Duration.ofDays(1L)));
    }

    @Test
    void fallsBackWhenUnparseable() {
        assertEquals(Duration.ofHours(2L), DurationParser.parse("nonsense", Duration.ofHours(2L)));
        assertEquals(Duration.ofHours(2L), DurationParser.parse("", Duration.ofHours(2L)));
        assertEquals(Duration.ofHours(2L), DurationParser.parse(null, Duration.ofHours(2L)));
    }

    @Test
    void formatsDurationsReadably() {
        assertEquals("permanent", DurationParser.format(null));
        assertEquals("5m", DurationParser.format(Duration.ofMinutes(5L)));
        assertEquals("2d 3h", DurationParser.format(Duration.ofDays(2L).plusHours(3L)));
        assertEquals("until revoked", DurationParser.formatTicks(-1L));
    }
}
