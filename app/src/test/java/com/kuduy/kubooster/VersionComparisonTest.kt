package com.kuduy.kubooster

import com.kuduy.kubooster.data.compatibility.VersionComparison
import org.junit.Assert.assertEquals
import org.junit.Test

class VersionComparisonTest {

    @Test
    fun `equal versions compare zero`() {
        assertEquals(0, VersionComparison.compareVersionNames("4.7.0", "4.7.0"))
    }

    @Test
    fun `numeric segments compare numerically not lexicographically`() {
        // 4.10.0 > 4.7.0 lexicographically would be false; numerically true.
        assert(VersionComparison.compareVersionNames("4.10.0", "4.7.0") > 0)
        assert(VersionComparison.compareVersionNames("4.7.0", "4.10.0") < 0)
    }

    @Test
    fun `shorter version equals padded version - same release`() {
        // "4.7" and "4.7.0" are the same release; treating them as equal is the
        // correct behavior for a compatibility check.
        assertEquals(0, VersionComparison.compareVersionNames("4.7", "4.7.0"))
    }

    @Test
    fun `non numeric suffixes are ignored`() {
        assertEquals(0, VersionComparison.compareVersionNames("4.7.0-beta", "4.7.0"))
    }

    @Test
    fun `major segment dominates`() {
        assert(VersionComparison.compareVersionNames("5.0.0", "4.99.99") > 0)
    }
}
