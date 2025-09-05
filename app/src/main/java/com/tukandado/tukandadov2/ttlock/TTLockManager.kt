package com.tukandado.tukandadov2.ttlock

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ttlock.bl.sdk.api.ExtendedBluetoothDevice
import com.ttlock.bl.sdk.api.TTLockClient
import com.ttlock.bl.sdk.constant.ControlAction
import com.ttlock.bl.sdk.entity.ControlLockResult
import com.ttlock.bl.sdk.entity.LockError
import com.ttlock.bl.sdk.callback.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

class TTLockManager(private val context: Context) {
    private val scannedLocks = mutableListOf<ExtendedBluetoothDevice>()

    init {
        TTLockClient.getDefault().prepareBTService(context)
    }

    fun startScan() {
        scannedLocks.clear()
        TTLockClient.getDefault().startScanLock(object: ScanLockCallback {
            override fun onScanLockSuccess(device: ExtendedBluetoothDevice?) {
                if (device != null) {
                    scannedLocks.add(device)
                    Log.d("TukandadoManager", "Candado encontrado: ${device.name} (${device.address})")
                }
            }

            override fun onFail(error: LockError?) {
                Log.e("TukandadoManager", "Error al escanear: ${error?.errorMsg}")
            }
        })
    }

    fun stopScan() {
        TTLockClient.getDefault().stopScanLock()
    }

    fun initLock(device: ExtendedBluetoothDevice) {
        TTLockClient.getDefault().initLock(device, object : InitLockCallback {
            override fun onInitLockSuccess(lockData: String?) {
                Log.d("TukandadoManager", "LockData obtenido: $lockData")
            }

            override fun onFail(error: LockError?) {
                Log.e("TukandadoManager", "Fallo al inicializar: ${error?.errorMsg}")
            }
        })
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun controlLock(
        lockDataJson: String,
        lockMac: String,
        isOpen: Boolean
    ): Result<Unit> = suspendCancellableCoroutine { cont ->
        val action = if (isOpen) ControlAction.UNLOCK else ControlAction.LOCK

        TTLockClient.getDefault().controlLock(
            action,
            lockDataJson,
            lockMac,
            object : ControlLockCallback {
                override fun onControlLockSuccess(result: ControlLockResult?) {
                    if (!cont.isCompleted) cont.resume(Result.success(Unit)) {}
                }
                override fun onFail(error: LockError?) {
                    val msg = "TukandadoManager error: ${error?.name ?: "UNKNOWN"} (0x${error?.errorCode?.toString() ?: "??"})"
                    if (cont.isCompleted) return
                    cont.resume(Result.failure(Exception(msg))) {}
                }
            }
        )

        cont.invokeOnCancellation {
            // TTLock no expone cancelación; no-op
        }
    }

    fun stopBTService() {
        TTLockClient.getDefault().stopBTService()
    }

    fun getScannedLocks(): List<ExtendedBluetoothDevice> {
        return scannedLocks.toList()
    }

    //PASSCODES

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun createCustomPasscode(
        passcode: String,
        startDate: Long,
        endDate: Long,
        lockDataJson: String,
        lockMac: String
    ): Result<String> = suspendCancellableCoroutine { cont ->
        TTLockClient.getDefault().createCustomPasscode(
            passcode,
            startDate,
            endDate,
            lockDataJson,
            lockMac,
            object : CreateCustomPasscodeCallback {
                override fun onCreateCustomPasscodeSuccess(serverPasscodeId: String?) {
                    cont.resume(Result.success(serverPasscodeId ?: "OK")) { }
                }

                override fun onFail(error: LockError?) {
                    cont.resumeWith(
                        Result.failure(
                            Exception("TukandadoManager error: ${error?.name ?: "UNKNOWN"} (${error?.errorCode ?: -1})")
                        )
                    )
                }
            }
        )

        cont.invokeOnCancellation {
            // Si quieres cancelar la llamada al SDK aquí, aunque TTLock no expone cancelación
        }
    }

    // TTLockManager.kt
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun deletePasscodeSuspend(
        passcode: String,
        lockDataJson: String,
        lockMac: String
    ): Result<Unit> = suspendCancellableCoroutine { cont ->
        TTLockClient.getDefault().deletePasscode(
            passcode,
            lockDataJson,
            lockMac,
            object : DeletePasscodeCallback {
                override fun onDeletePasscodeSuccess() {
                    cont.resume(Result.success(Unit)) { }
                }
                override fun onFail(error: LockError?) {
                    cont.resumeWith(
                        Result.failure(
                            Exception("TukandadoManager delete error: ${error?.name ?: "UNKNOWN"} (${error?.errorCode ?: -1})")
                        )
                    )
                }
            }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun modifyPasscodeSuspend(
        originalCode: String,
        newCode: String?,           // si null -> mantenemos original
        startDate: Long?,           // si null -> 0L (sin cambio)
        endDate: Long?,             // si null -> 0L (sin cambio)
        lockDataJson: String,
        lockMac: String
    ): Result<Unit> = suspendCancellableCoroutine { cont ->
        val nc = newCode ?: originalCode
        val sd = startDate ?: 0L
        val ed = endDate ?: 0L
        TTLockClient.getDefault().modifyPasscode(
            originalCode,
            nc,
            sd,
            ed,
            lockDataJson,
            lockMac,
            object : ModifyPasscodeCallback {
                override fun onModifyPasscodeSuccess() {
                    cont.resume(Result.success(Unit)) { }
                }
                override fun onFail(error: LockError?) {
                    cont.resumeWith(
                        Result.failure(
                            Exception("TTLock modify error: ${error?.name ?: "UNKNOWN"} (${error?.errorCode ?: -1})")
                        )
                    )
                }
            }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getAllValidPasscodesSuspend(
        lockDataJson: String,
        lockMac: String
    ): Result<String> = suspendCancellableCoroutine { cont ->
        TTLockClient.getDefault().getAllValidPasscodes(
            lockDataJson,
            lockMac,
            object : GetAllValidPasscodeCallback {
                override fun onGetAllValidPasscodeSuccess(json: String?) {
                    cont.resume(Result.success(json ?: "[]")) { }
                }
                override fun onFail(error: LockError?) {
                    cont.resumeWith(
                        Result.failure(
                            Exception("TTLock list error: ${error?.name ?: "UNKNOWN"} (${error?.errorCode ?: -1})")
                        )
                    )
                }
            }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun resetAllPasscodes(
        lockDataJson: String,
        lockMac: String
    ): Result<Unit> = suspendCancellableCoroutine { cont ->
        Log.d("Los params que entran en el reset lockDataJson",lockDataJson)
        Log.d("Los params que entran en el reset lockMac",lockMac)
        TTLockClient.getDefault().resetPasscode(
            lockDataJson,
            lockMac,
            object : ResetPasscodeCallback {
                override fun onResetPasscodeSuccess(p0: String?) {
                    cont.resume(Result.success(Unit)) { /* no-op */ }
                }
                override fun onFail(err: LockError?) {
                    cont.resumeWith(
                        Result.failure(
                            Exception("TTLock error: ${err?.name ?: "UNKNOWN"} (${err?.errorCode ?: -1})")
                        )
                    )
                }
            }
        )
        cont.invokeOnCancellation { /* no-op */ }
    }


    fun modifyAdminPasscode(newPasscode: String, lockDataJson: String, lockMac: String) {
        TTLockClient.getDefault().modifyAdminPasscode(newPasscode, lockDataJson, lockMac, object: ModifyAdminPasscodeCallback     {
            override fun onModifyAdminPasscodeSuccess(p0: String?) {
                TODO("Not yet implemented")
            }

            override fun onFail(p0: LockError?) {
                TODO("Not yet implemented")
            }
        })
    }

    //IC CARDS

    fun addICCard(startDate: Long, endDate: Long, lockDataJson: String, lockMac: String) {
        TTLockClient.getDefault().addICCard(startDate, endDate, lockDataJson, lockMac, object: AddICCardCallback {
            override
            fun onEnterAddMode() {
                TODO("Not yet implemented")
            }

            override fun onAddICCardSuccess(p0: Long) {
                TODO("Not yet implemented")
            }

            override fun onFail(p0: LockError?) {
                TODO("Not yet implemented")
            }
        })
    }

    fun modifyICCardValidityPeriod(startDate: Long, endDate: Long, cardNum: String, lockDataJson: String, lockMac: String) {
        TTLockClient.getDefault().modifyICCardValidityPeriod(startDate, endDate, cardNum, lockDataJson, lockMac, object: ModifyICCardPeriodCallback {
            override fun onModifyICCardPeriodSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFail(p0: LockError?) {
                TODO("Not yet implemented")
            }
        })
    }

    fun getAllValidICCards(startDate: Long, endDate: Long, cardNum: String, lockDataJson: String, lockMac: String) {
        TTLockClient.getDefault().getAllValidICCards(lockDataJson, lockMac, object: GetAllValidICCardCallback {
            override fun onGetAllValidICCardSuccess(p0: String?) {
                TODO("Not yet implemented")
            }

            override fun onFail(p0: LockError?) {
                TODO("Not yet implemented")
            }
        })
    }

    fun deleteICCard(cardNum: String, lockDataJson: String, lockMac: String) {
        TTLockClient.getDefault().deleteICCard(cardNum, lockDataJson, lockMac, object: DeleteICCardCallback {
            override fun onDeleteICCardSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFail(p0: LockError?) {
                TODO("Not yet implemented")
            }
        })
    }

    fun clearAllICCard(lockDataJson: String, lockMac: String) {
        TTLockClient.getDefault().clearAllICCard(lockDataJson, lockMac, object: ClearAllICCardCallback  {
            override fun onClearAllICCardSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFail(p0: LockError?) {
                TODO("Not yet implemented")
            }
        })
    }

}