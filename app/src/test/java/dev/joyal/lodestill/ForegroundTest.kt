package dev.joyal.lodestill

import dev.joyal.lodestill.core.Foreground
import dev.joyal.lodestill.core.Foreground.Kind.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundTest {
    @Test fun appOpenedBeforeSessionIsStillForeground() =
        assertEquals("com.insta", Foreground.resolve(listOf(FOREGROUND to "com.insta")))
    @Test fun appLeftIsNotForeground() =
        assertNull(Foreground.resolve(listOf(FOREGROUND to "com.insta", BACKGROUND to "com.insta")))
    @Test fun switchingAppsReportsLatest() = assertEquals("com.b", Foreground.resolve(listOf(
        FOREGROUND to "com.a", BACKGROUND to "com.a", FOREGROUND to "com.b")))
    @Test fun staleBackgroundOfOtherAppDoesNotClearCurrent() = assertEquals("com.b", Foreground.resolve(listOf(
        FOREGROUND to "com.a", FOREGROUND to "com.b", BACKGROUND to "com.a")))
    @Test fun noEventsMeansNoForeground() = assertNull(Foreground.resolve(emptyList()))
}
