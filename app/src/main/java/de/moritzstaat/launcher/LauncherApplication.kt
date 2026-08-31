package de.moritzstaat.launcher

import android.app.Application

/**
 * Holds the process wide singletons. Kept deliberately thin: everything the launcher needs
 * is created lazily so that a cold start does no work it can avoid.
 */
class LauncherApplication : Application()
