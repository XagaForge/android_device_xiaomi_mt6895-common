/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.utils

import android.util.Log
import java.io.File

private const val TAG = "FileUtils"

/*
 * Reads the first line of text from the given file.
 *
 * @return the read line contents, or null on failure
 */
fun readOneLine(fileName: String): String? =
    runCatching { File(fileName).useLines { it.firstOrNull() } }
        .onFailure { e -> Log.e(TAG, "Could not read from file $fileName", e) }
        .getOrNull()

/*
 * Writes the given value into the given file
 *
 * @return true on success, false on failure
 */
fun writeLine(fileName: String, value: String): Boolean =
    runCatching { File(fileName).writeText(value) }
        .onFailure { e -> Log.e(TAG, "Could not write to file $fileName", e) }
        .isSuccess

/*
 * Checks whether the given file exists
 *
 * @return true if exists, false if not
 */
fun fileExists(fileName: String): Boolean = File(fileName).exists()

/*
 * Checks whether the given file is readable
 *
 * @return true if readable, false if not
 */
fun isFileReadable(fileName: String): Boolean {
    val file = File(fileName)
    return file.exists() && file.canRead()
}

/*
 * Checks whether the given file is writable
 *
 * @return true if writable, false if not
 */
fun isFileWritable(fileName: String): Boolean {
    val file = File(fileName)
    return file.exists() && file.canWrite()
}

/*
 * Deletes an existing file
 *
 * @return true if the delete was successful, false if not
 */
fun delete(fileName: String): Boolean =
    runCatching { File(fileName).delete() }
        .onFailure { e -> Log.w(TAG, "Failed to delete $fileName", e) }
        .getOrDefault(false)

/*
 * Renames an existing file
 *
 * @return true if the rename was successful, false if not
 */
fun rename(srcPath: String, dstPath: String): Boolean =
    runCatching { File(srcPath).renameTo(File(dstPath)) }
        .onFailure { e -> Log.w(TAG, "Failed to rename $srcPath to $dstPath", e) }
        .getOrDefault(false)
