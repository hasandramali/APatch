package com.valvesoftware.aq

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import dalvik.annotation.optimization.FastNative
import kotlinx.parcelize.Parcelize

object Natives {
    init {
        System.loadLibrary("apjni")
    }

    @Immutable
    @Parcelize
    @Keep
    data class Profile(
        var uid: Int = 0,
        var toUid: Int = 0,
        var scontext: String = APApplication.DEFAULT_SCONTEXT,
    ) : Parcelable

    @Keep
    class KPMCtlRes {
        var rc: Long = 0
        var outMsg: String? = null

        constructor()

        constructor(rc: Long, outMsg: String?) {
            this.rc = rc
            this.outMsg = outMsg
        }
    }


    @FastNative
    private external fun nativeSu(superKey: String, toUid: Int, scontext: String?): Long

    fun su(toUid: Int, scontext: String?): Boolean {
        return nativeSu(APApplication.superKey, toUid, scontext) == 0L
    }

    fun su(): Boolean {
        return su(0, "")
    }

    @FastNative
    external fun nativeReady(superKey: String): Boolean

    @FastNative
    private external fun nativeSuPath(superKey: String): String

    fun suPath(): String {
        return nativeSuPath(APApplication.superKey)
    }

    @FastNative
    private external fun nativeSuUids(superKey: String): IntArray

    fun suUids(): IntArray {
        return nativeSuUids(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchVersion(superKey: String): Long
    fun kernelPatchVersion(): Long {
        return nativeKernelPatchVersion(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchBuildTime(superKey: String): String
    fun kernelPatchBuildTime(): String {
        return nativeKernelPatchBuildTime(APApplication.superKey)
    }

    private external fun nativeLoadKernelPatchModule(
        superKey: String, modulePath: String, args: String
    ): Long

    fun loadKernelPatchModule(modulePath: String, args: String): Long {
        return nativeLoadKernelPatchModule(APApplication.superKey, modulePath, args)
    }

    private external fun nativeUnloadKernelPatchModule(superKey: String, moduleName: String): Long
    fun unloadKernelPatchModule(moduleName: String): Long {
        return nativeUnloadKernelPatchModule(APApplication.superKey, moduleName)
    }

    @FastNative
    private external fun nativeKernelPatchModuleNum(superKey: String): Long

    fun kernelPatchModuleNum(): Long {
        return nativeKernelPatchModuleNum(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchModuleList(superKey: String): String
    fun kernelPatchModuleList(): String {
        return nativeKernelPatchModuleList(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchModuleInfo(superKey: String, moduleName: String): String
    fun kernelPatchModuleInfo(moduleName: String): String {
        return nativeKernelPatchModuleInfo(APApplication.superKey, moduleName)
    }

    private external fun nativeControlKernelPatchModule(
        superKey: String, modName: String, jctlargs: String
    ): KPMCtlRes

    fun kernelPatchModuleControl(moduleName: String, controlArg: String): KPMCtlRes {
        return nativeControlKernelPatchModule(APApplication.superKey, moduleName, controlArg)
    }

    @FastNative
    private external fun nativeGrantSu(
        superKey: String, uid: Int, toUid: Int, scontext: String?
    ): Long

    fun grantSu(uid: Int, toUid: Int, scontext: String?): Long {
        return nativeGrantSu(APApplication.superKey, uid, toUid, scontext)
    }

    @FastNative
    private external fun nativeRevokeSu(superKey: String, uid: Int): Long
    fun revokeSu(uid: Int): Long {
        return nativeRevokeSu(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeSetUidExclude(superKey: String, uid: Int, exclude: Int): Int
    fun setUidExclude(uid: Int, exclude: Int): Int {
        return nativeSetUidExclude(APApplication.superKey, uid, exclude)
    }

    @FastNative
    private external fun nativeGetUidExclude(superKey: String, uid: Int): Int
    fun isUidExcluded(uid: Int): Int {
        return nativeGetUidExclude(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeSuProfile(superKey: String, uid: Int): Profile
    fun suProfile(uid: Int): Profile {
        return nativeSuProfile(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeResetSuPath(superKey: String, path: String): Boolean
    fun resetSuPath(path: String): Boolean {
        return nativeResetSuPath(APApplication.superKey, path)
    }
}
