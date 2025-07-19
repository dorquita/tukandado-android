package com.tukandado.tukandadov2.ttlock

import android.content.Context
import android.util.Log
import com.ttlock.bl.sdk.api.ExtendedBluetoothDevice
import com.ttlock.bl.sdk.api.TTLockClient
import com.ttlock.bl.sdk.callback.ControlLockCallback
import com.ttlock.bl.sdk.callback.InitLockCallback
import com.ttlock.bl.sdk.callback.ScanLockCallback
import com.ttlock.bl.sdk.constant.ControlAction
import com.ttlock.bl.sdk.entity.ControlLockResult
import com.ttlock.bl.sdk.entity.LockData
import com.ttlock.bl.sdk.entity.LockError

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
                    Log.d("TTLock", "Candado encontrado: ${device.name} (${device.address})")
                }
            }

            override fun onFail(error: LockError?) {
                Log.e("TTLock", "Error al escanear: ${error?.errorMsg}")
            }
        })
    }

    fun stopScan() {
        TTLockClient.getDefault().stopScanLock()
    }

    fun initLock(device: ExtendedBluetoothDevice) {
        TTLockClient.getDefault().initLock(device, object : InitLockCallback {
            override fun onInitLockSuccess(lockData: String?) {
                Log.d("TTLock", "LockData obtenido: $lockData")
            }

            override fun onFail(error: LockError?) {
                Log.e("TTLock", "Fallo al inicializar: ${error?.errorMsg}")
            }
        })
    }


    fun controlLock(lockDataJson: String, lockMac: String, isOpen: Boolean) {
        Log.d("lockData", lockDataJson)
        Log.d("lockMac", lockMac)
        Log.d("isOpen", isOpen.toString())
        val action = if (isOpen) ControlAction.UNLOCK else ControlAction.LOCK

        Log.d("action", action.toString())

        TTLockClient.getDefault().controlLock(
            action,
            lockDataJson,
            lockMac,
            object: ControlLockCallback {
                override fun onControlLockSuccess(p0: ControlLockResult?) {
                    Log.d("TTLock", "Lock ${if (isOpen) "opened" else "closed"} successfully")
                }

                override fun onFail(error: LockError?) {
                    Log.e("TTLock", "Failed to control lock: ${error?.errorMsg} (${error?.errorCode})")
                }
            })
    }

    fun stopBTService() {
        TTLockClient.getDefault().stopBTService()
    }

    fun getScannedLocks(): List<ExtendedBluetoothDevice> {
        return scannedLocks.toList()
    }

}