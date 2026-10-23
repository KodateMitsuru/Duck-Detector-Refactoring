/*
 * Copyright 2026 Duck Apps Contributor
 * If you have any questions, suggestions, or other inquiries, please email Eltavine <me@eltavine.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eltavine.duckdetector.features.adbruntime.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class AdbInitSocketProbeTest {
    @Test
    fun presentInodeIsReportedWhenStatSucceeds() {
        assertEquals(
            AdbInitSocketObservation.PRESENT,
            classifyInitSocketStat(directoryErrno = 0, targetErrno = 0, controlErrno = AdbStatErrno.ENOENT),
        )
    }

    @Test
    fun deniedInodeIsPresentWhenOnlyTheSocketIsRefused() {
        for (errno in listOf(AdbStatErrno.EACCES, AdbStatErrno.EPERM)) {
            assertEquals(
                AdbInitSocketObservation.PRESENT_DENIED,
                classifyInitSocketStat(0, errno, AdbStatErrno.ENOENT),
            )
        }
    }

    @Test
    fun missingInodeNeedsAResolvableDirectory() {
        assertEquals(
            AdbInitSocketObservation.ABSENT,
            classifyInitSocketStat(0, AdbStatErrno.ENOENT, AdbStatErrno.ENOENT),
        )
        assertEquals(
            AdbInitSocketObservation.DIRECTORY_DENIED,
            classifyInitSocketStat(0, AdbStatErrno.ENOENT, AdbStatErrno.EACCES),
        )
        assertEquals(
            AdbInitSocketObservation.DIRECTORY_DENIED,
            classifyInitSocketStat(AdbStatErrno.EACCES, AdbStatErrno.EACCES, AdbStatErrno.EACCES),
        )
    }

    @Test
    fun unexpectedResultsAreUnknownRatherThanAbsent() {
        assertEquals(
            AdbInitSocketObservation.DIRECTORY_DENIED,
            classifyInitSocketStat(0, AdbStatErrno.ENOENT, 0),
        )
        assertEquals(
            AdbInitSocketObservation.UNKNOWN,
            // EIO: a failure that is neither absence nor a label denial.
            classifyInitSocketStat(0, 5, AdbStatErrno.ENOENT),
        )
        assertEquals(
            AdbInitSocketObservation.UNKNOWN,
            classifyInitSocketStat(null, 0, AdbStatErrno.ENOENT),
        )
        assertEquals(
            AdbInitSocketObservation.UNKNOWN,
            classifyInitSocketStat(0, null, AdbStatErrno.ENOENT),
        )
    }

    @Test
    fun listingTakesPrecedenceOverTheStatFallback() {
        assertEquals(
            AdbInitSocketObservation.PRESENT,
            resolveInitSocketPresence(setOf("adbd", "logd"), AdbInitSocketObservation.UNKNOWN),
        )
        assertEquals(
            AdbInitSocketObservation.ABSENT,
            resolveInitSocketPresence(setOf("logd", "mdns"), AdbInitSocketObservation.PRESENT_DENIED),
        )
        assertEquals(
            AdbInitSocketObservation.PRESENT_DENIED,
            resolveInitSocketPresence(null, AdbInitSocketObservation.PRESENT_DENIED),
        )
        assertEquals(
            AdbInitSocketObservation.ABSENT,
            resolveInitSocketPresence(null, AdbInitSocketObservation.ABSENT),
        )
    }

    @Test
    fun aPresentSocketWinsOverTheControlResult() {
        assertEquals(
            AdbInitSocketObservation.PRESENT,
            classifyInitSocketStat(0, 0, AdbStatErrno.EACCES),
        )
    }
}
