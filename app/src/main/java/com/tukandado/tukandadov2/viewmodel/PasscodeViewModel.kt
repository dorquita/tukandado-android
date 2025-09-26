package com.tukandado.tukandadov2.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tukandado.tukandadov2.BuildConfig
import com.tukandado.tukandadov2.api.CreateBookingPasscodeRequest
import com.tukandado.tukandadov2.api.CreatePasscodeRequest
import com.tukandado.tukandadov2.api.PasscodeApi
import com.tukandado.tukandadov2.api.PasscodeDto
import com.tukandado.tukandadov2.api.RetrofitInstance
import com.tukandado.tukandadov2.api.UpdatePasscodeRequest
import com.tukandado.tukandadov2.ttlock.TTLockManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

sealed class PasscodeUiModal {
    data object None : PasscodeUiModal()
    data class Success(val message: String) : PasscodeUiModal()
    data class Error(val message: String) : PasscodeUiModal()
}

data class PasscodeState(
    val isLoading: Boolean = false,
    val modal: PasscodeUiModal = PasscodeUiModal.None
)

class PasscodeViewModel : ViewModel() {
    private val _state = MutableStateFlow(PasscodeState())
    val state: StateFlow<PasscodeState> = _state

    private val _passcodes = MutableStateFlow<List<PasscodeDto>>(emptyList())
    val passcodes: StateFlow<List<PasscodeDto>> = _passcodes

    private val _selectedPasscode = MutableStateFlow<PasscodeDto?>(null)
    val selectedPasscode: StateFlow<PasscodeDto?> = _selectedPasscode

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Paginación
    private val _page = MutableStateFlow(1)
    val page: StateFlow<Int> = _page

    private val _pages = MutableStateFlow(1)
    val pages: StateFlow<Int> = _pages

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total

    private fun api(context: Context): PasscodeApi = RetrofitInstance.getPasscodeApi(context)

    /* -------- utilidades -------- */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun alignToMinute(millis: Long): Long =
        Instant.ofEpochMilli(millis).truncatedTo(ChronoUnit.MINUTES).toEpochMilli()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isoToMillisOrNull(iso: String?): Long? = try {
        if (iso.isNullOrBlank()) null else Instant.parse(iso).toEpochMilli()
    } catch (_: Throwable) { null }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun computeStartEndMillis(type: String, validFromIso: String?, validToIso: String?): Pair<Long, Long> {
        return when (type) {
            "permanent" -> 0L to 0L // TTLock: 0/0 = sin vigencia
            "timebound" -> {
                val start = alignToMinute(isoToMillisOrNull(validFromIso) ?: System.currentTimeMillis())
                val end = alignToMinute(isoToMillisOrNull(validToIso) ?: (System.currentTimeMillis() + 365L*24*3600*1000))
                start to end
            }
            "one_time" -> {
                val start = alignToMinute(System.currentTimeMillis())
                val end = alignToMinute(start + 24*3600*1000) // ventana 24h
                start to end
            }
            else -> {
                val start = alignToMinute(System.currentTimeMillis())
                val end = alignToMinute(start + 365L*24*3600*1000)
                start to end
            }
        }
    }

    fun setSelectedPasscode(dto: PasscodeDto) {
        _selectedPasscode.value = dto
    }

    /* -------- listado -------- */
    fun listPasscodes(
        context: Context,
        page: Int = 1,
        limit: Int = 20,
        lockId: String? = null,
        status: String? = null,
        q: String? = null,
        activeOnly: Boolean? = null,
        clubId: String? = null
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true
                Log.d("PasscodeVM", "➡️ GET /passcodes/list page=$page limit=$limit lockId=$lockId status=$status activeOnly=$activeOnly q=$q clubId=$clubId")
                val response = api(context).listPasscodes(page, limit, lockId, status, q, activeOnly, clubId)
                Log.d("PasscodeVM", "⬅️ code=${response.code()} ok=${response.isSuccessful}")
                if (response.isSuccessful) {
                    val body = response.body()
                    _passcodes.value = body?.items ?: emptyList()
                    _page.value = body?.page ?: page
                    _pages.value = body?.pages ?: 1
                    _total.value = body?.total ?: 0
                    _error.value = null
                } else {
                    _error.value = "Error al obtener passcodes: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Excepción al obtener passcodes: ${e.message}"
                Log.e("PasscodeVM", "💥 list", e)
            } finally { _loading.value = false }
        }
    }

    /* -------- detalle (RETORNA Result) -------- */
    suspend fun getPasscodeById(
        context: Context,
        passcodeId: String
    ): Result<PasscodeDto> {
        return try {
            _loading.value = true
            Log.d("PasscodeVM", "➡️ GET /passcodes/$passcodeId")
            val response = api(context).getPasscodeById(passcodeId)
            Log.d("PasscodeVM", "respuesta get passcode=${response.code()} ok=${response.isSuccessful}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d("PasscodeVM", body.toString())
                    _selectedPasscode.value = body
                    _error.value = null
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Respuesta vacía del backend"))
                }
            } else {
                val e = RuntimeException("Error al obtener passcode: HTTP ${response.code()}")
                _error.value = e.message
                Result.failure(e)
            }
        } catch (e: Exception) {
            _error.value = "Excepción al obtener passcode: ${e.message}"
            Log.e("PasscodeVM", "💥 getById", e)
            Result.failure(e)
        } finally {
            _loading.value = false
        }
    }

    /* -------- crear (BT → backend) (RETORNA Result) -------- */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createPasscode(
        context: Context,
        request: CreatePasscodeRequest,
        refreshAfterCreate: Boolean = true
    ): Result<PasscodeDto> {
        _state.value = _state.value.copy(isLoading = true, modal = PasscodeUiModal.None)
        return try {
            // 1) Bluetooth (si tenemos lockData y lockMac)
            val canBt = !request.lockData.isNullOrBlank() && !request.lockMac.isNullOrBlank() && !BuildConfig.BYPASS_LOGIN
            if (canBt) {
                val (startMs, endMs) = computeStartEndMillis(request.type, request.validFrom, request.validTo)
                Log.d("PasscodeVM", "🔐 TTLock.create pass=${request.code.length}d start=$startMs end=$endMs mac=${request.lockMac}")
                TTLockManager(context).createCustomPasscode(
                    passcode = request.code,
                    startDate = startMs,
                    endDate = endMs,
                    lockDataJson = request.lockData!!,
                    lockMac = request.lockMac!!
                ).getOrThrow()
                Log.d("PasscodeVM", "✅ BT creado")
            } else {
                Log.w("PasscodeVM", "⚠️ Sin lockData/lockMac → salto BT, solo backend")
            }

            // 2) Backend (persistir en BD)
            Log.d("PasscodeVM", "➡️ POST /passcodes/createPasscode body=$request")
            val response = api(context).createPasscode(body = request)
            Log.d("PasscodeVM", "⬅️ code=${response.code()} ok=${response.isSuccessful}")

            if (response.isSuccessful) {
                val created = response.body() ?: return Result.failure(IllegalStateException("Respuesta vacía del backend"))
                _selectedPasscode.value = created
                _passcodes.value = listOf(created) + _passcodes.value
                Log.d("PasscodeVM", "✅ backend id=${created._id}")
                if (refreshAfterCreate) listPasscodes(context, page = _page.value)
                Result.success(created)
            } else {
                Result.failure(RuntimeException("Error al crear passcode: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("PasscodeVM", "💥 create", e)
            Result.failure(e)
        } finally {
            _state.value = _state.value.copy(isLoading = false)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createPasscodeClient(
        context: Context,
        request: CreatePasscodeRequest,
        clientPasscodeVm: PasscodeViewModel,     // 👈 VM que tiene createBookingPasscode(...)
        refreshAfterCreate: Boolean = true
    ): Result<PasscodeDto> {
        _state.value = _state.value.copy(isLoading = true, modal = PasscodeUiModal.None)
        return try {
            // 1) Bluetooth (si tenemos lockData y lockMac)
            val canBt = !request.lockData.isNullOrBlank() && !request.lockMac.isNullOrBlank() && !BuildConfig.BYPASS_LOGIN
            if (canBt) {
                // Para cliente tratamos como "permanent" (ventana amplia)
                val (startMs, endMs) = computeStartEndMillis("permanent", null, null)
                Log.d("PasscodeVM", "🔐 TTLock.create (CLIENT) pass=${request.code.length}d start=$startMs end=$endMs mac=${request.lockMac}")
                TTLockManager(context).createCustomPasscode(
                    passcode = request.code,
                    startDate = startMs,
                    endDate = endMs,
                    lockDataJson = request.lockData!!,
                    lockMac = request.lockMac!!
                ).getOrThrow()
                Log.d("PasscodeVM", "✅ BT creado (CLIENT)")
            } else {
                Log.w("PasscodeVM", "⚠️ (CLIENT) Sin lockData/lockMac → salto BT, solo backend")
            }

            // 2) Backend (asociar a la reserva del usuario autenticado)
            val nameOrNull = request.name?.ifBlank { null }
            Log.d("PasscodeVM", "➡️ (CLIENT) POST /passcodes/booking { code,name }")
            val result = clientPasscodeVm.createBookingPasscode(
                context = context,
                code = request.code,
                name = nameOrNull
            )

            if (result.isSuccess) {
                val created = result.getOrNull()!!
                _selectedPasscode.value = created
                _passcodes.value = listOf(created) + _passcodes.value
                Log.d("PasscodeVM", "✅ (CLIENT) backend id=${created._id}")
                if (refreshAfterCreate) listPasscodes(context, page = _page.value)
                Result.success(created)
            } else {
                val ex = result.exceptionOrNull() ?: RuntimeException("Error desconocido creando passcode (cliente)")
                Result.failure(ex)
            }
        } catch (e: Exception) {
            Log.e("PasscodeVM", "💥 create (CLIENT)", e)
            Result.failure(e)
        } finally {
            _state.value = _state.value.copy(isLoading = false)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun resetPasscodes(
        context: Context,
        lockDataJson: String?,
        lockMac: String?,
        lockIdForBackend: String? = null  // TTLock lockId (num o string) para el backend
    ): Result<Unit> {
        Log.d("Lo que llega lockDataJson",lockDataJson.toString())
        Log.d("Lo que llega lockMac",lockMac.toString())
        Log.d("Lo que llega lockIdForBackend",lockIdForBackend.toString())
        _state.value = _state.value.copy(isLoading = true, modal = PasscodeUiModal.None)
        return try {
            val canBt = !lockDataJson.isNullOrBlank() && !lockMac.isNullOrBlank() && !BuildConfig.BYPASS_LOGIN

            if (canBt) {
                Log.d("PasscodeVM", "🔁 Reset passcodes (BLE) mac=$lockMac")
                TTLockManager(context)
                    .resetAllPasscodes(lockDataJson, lockMac)
                    .getOrThrow()
                Log.d("PasscodeVM", "✅ BT reset ok")
            } else {
                Log.w("PasscodeVM", "⚠️ Sin lockData/lockMac o BYPASS_LOGIN activo → salto BT, solo backend")
            }

            // ➡️ Backend: marcar/revocar todos los passcodes del lock (si tenemos lockId)
            if (!lockIdForBackend.isNullOrBlank()) {
                Log.d("PasscodeVM", "➡️ POST /locks/{lockId}/passcodes/reset lockId=$lockIdForBackend")
                val resp = api(context).resetPasscodesByLock(lockIdForBackend)

                // Ajusta esta condición si tu endpoint devuelve 204 sin body:
                if (!resp.isSuccessful /* && resp.code() != 204 */) {
                    throw RuntimeException("Backend reset HTTP ${resp.code()}")
                }
                Log.d("PasscodeVM", "⬅️ backend reset ok code=${resp.code()}")
            } else {
                Log.w("PasscodeVM", "⚠️ Sin lockIdForBackend → no se notificará el reseteo global al servidor")
            }

            // Refresco del listado si procede
            runCatching { listPasscodes(context, page = _page.value) }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PasscodeVM", "❌ Reset", e)
            Result.failure(e)
        } finally {
            _state.value = _state.value.copy(isLoading = false)
        }
    }


    /* -------- revocar (acortar endDate + backend) (RETORNA Result<PasscodeDto>) -------- */
    suspend fun revokePasscode(
        context: Context,
        passcodeId: String,
        originalCode: String?,     // si no lo tienes, solo backend
        lockDataJson: String?,
        lockMac: String?,
        marginMs: Long = 60_000L
    ): Result<PasscodeDto> {
        _state.value = _state.value.copy(isLoading = true, modal = PasscodeUiModal.None)
        return try {
            val canBt = !originalCode.isNullOrBlank() && !lockDataJson.isNullOrBlank() && !lockMac.isNullOrBlank() && !BuildConfig.BYPASS_LOGIN
            if (canBt) {
                val endNow = System.currentTimeMillis() - marginMs
                Log.d("PasscodeVM", "🔒 Revoking (BLE) endDate=$endNow mac=$lockMac")
                TTLockManager(context).modifyPasscodeSuspend(
                    originalCode = originalCode!!,
                    newCode = null,
                    startDate = null,
                    endDate = endNow,
                    lockDataJson = lockDataJson!!,
                    lockMac = lockMac!!
                ).getOrThrow()
            } else {
                Log.w("PasscodeVM", "⚠️ Revocar sin BLE (solo backend)")
            }

            Log.d("PasscodeVM", "➡️ POST /passcodes/$passcodeId/revoke")
            val resp = api(context).revokePasscode(passcodeId)
            if (!resp.isSuccessful) return Result.failure(RuntimeException("Backend revoke HTTP ${resp.code()}"))

            val updated = resp.body() ?: return Result.failure(IllegalStateException("Respuesta vacía del backend"))
            // opcional: refresca lista/seleccionado
            _selectedPasscode.value = updated
            runCatching { listPasscodes(context, page = _page.value) }

            Result.success(updated)
        } catch (e: Exception) {
            Log.e("PasscodeVM", "❌ revoke", e)
            Result.failure(e)
        } finally {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    /* -------- eliminar (borra/revoca en lock + soft delete) (RETORNA Result<Unit>) -------- */
    suspend fun deletePasscode(
        context: Context,
        passcodeId: String,
        originalCode: String?,   // si no lo tienes, intenta solo backend (mejor revocar antes)
        lockDataJson: String?,
        lockMac: String?
    ): Result<Unit> {
        Log.d(
            "PasscodeVM",
            "deletePasscode inputs → passcodeId=$passcodeId, originalCode=$originalCode, lockDataJson=$lockDataJson, lockMac=$lockMac, BYPASS_LOGIN=${BuildConfig.BYPASS_LOGIN}"
        )
        val TAG = "PasscodeVM"
        fun mask(s: String?, keep: Int = 4) = if (s.isNullOrBlank()) "null" else s.take(keep) + "…"
        val macMasked = mask(lockMac, 5)

        _state.value = _state.value.copy(isLoading = true, modal = PasscodeUiModal.None)
        Log.d(TAG, "DELETE-START id=$passcodeId mac=$macMasked bleInputs=" +
                "code=${!originalCode.isNullOrBlank()} data=${!lockDataJson.isNullOrBlank()} mac=${!lockMac.isNullOrBlank()}")

        return try {
            val originalOk = !originalCode.isNullOrBlank()
            val dataOk     = !lockDataJson.isNullOrBlank()
            val macOk      = !lockMac.isNullOrBlank()
            val bypass     = BuildConfig.BYPASS_LOGIN
            val canBt      = originalOk && dataOk && macOk && !bypass

            Log.d(TAG, "CANBT-CHECK " +
                    "originalOk=$originalOk(${mask(originalCode)}), " +
                    "dataOk=$dataOk(${mask(lockDataJson, 8)}), " +
                    "macOk=$macOk(${mask(lockMac, 5)}), " +
                    "BYPASS_LOGIN=$bypass -> canBt=$canBt")

            // --- 1) Intento BLE (TTLock) ---
            if (canBt) {
                Log.d(TAG, "BLE-DELETE-TRY mac=$macMasked")
                val del = runCatching {
                    TTLockManager(context).deletePasscodeSuspend(
                        passcode = originalCode!!,
                        lockDataJson = lockDataJson!!,
                        lockMac = lockMac!!
                    )
                }.fold(
                    onSuccess = { it },
                    onFailure = { err ->
                        Log.w(TAG, "BLE-DELETE-ERR mac=$macMasked reason=${err.message}")
                        Result.failure(err)
                    }
                )

                del.fold(
                    onSuccess = {
                        Log.i(TAG, "BLE-DELETE-OK mac=$macMasked")
                    },
                    onFailure = { err ->
                        // Si falla delete, intento acortar vigencia (revoke por fecha fin)
                        val endNow = System.currentTimeMillis() - 60_000L
                        Log.w(TAG, "BLE-MODIFY-TRY endNow=$endNow mac=$macMasked")
                        runCatching {
                            TTLockManager(context).modifyPasscodeSuspend(
                                originalCode = originalCode,
                                newCode = null,
                                startDate = null,
                                endDate = endNow,
                                lockDataJson = lockDataJson,
                                lockMac = lockMac
                            )
                        }.fold(
                            onSuccess = {
                                Log.i(TAG, "BLE-MODIFY-OK mac=$macMasked")
                            },
                            onFailure = { modErr ->
                                Log.e(TAG, "BLE-MODIFY-ERR mac=$macMasked reason=${modErr.message}", modErr)
                            }
                        )
                    }
                )
            } else {
                Log.w(TAG, "BLE-SKIP (sin datos suficientes o BYPASS_LOGIN=true)")
            }

            // --- 2) Llamada a backend (soft delete) ---
            Log.d(TAG, "HTTP-REQ DELETE /passcodes/$passcodeId mac=$macMasked")
            val resp = runCatching { api(context).softDeletePasscode(passcodeId) }
                .onFailure { t ->
                    Log.e(TAG, "HTTP-ERR transport=${t::class.simpleName} msg=${t.message}", t)
                }
                .getOrElse { throw it }

            val code = resp.code()
            val ok = resp.isSuccessful || code == 204
            val errBody = if (!ok) {
                // ojo: .string() solo una vez; no lo vuelvas a leer fuera
                runCatching { resp.errorBody()?.string() }.getOrNull()
            } else null

            Log.d(TAG, "HTTP-RESP code=$code isSuccessful=${resp.isSuccessful} hasBody=${resp.body()!=null} hasErr=${errBody!=null}")
            if (!ok) {
                Log.e(TAG, "HTTP-FAIL code=$code err=$errBody")
                return Result.failure(RuntimeException("Backend delete HTTP $code"))
            }

            // --- 3) Refresco silencioso del listado (no rompemos si falla) ---
            runCatching { listPasscodes(context, page = _page.value) }
                .onFailure { t -> Log.w(TAG, "LIST-REFRESH-ERR ${t.message}") }

            Log.i(TAG, "DELETE-DONE id=$passcodeId mac=$macMasked")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PasscodeVM", "DELETE-UNCAUGHT ${e::class.simpleName}: ${e.message}", e)
            Result.failure(e)
        } finally {
            _state.value = _state.value.copy(isLoading = false)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun modifyPasscode(
        context: Context,
        passcodeId: String,
        originalCode: String,               // BLE requiere el código actual
        newCode: String?,                   // null para mantener
        newValidFromIso: String?,           // null para mantener
        newValidToIso: String?,             // null para mantener
        lockDataJson: String?,
        lockMac: String?
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, modal = PasscodeUiModal.None)
        try {
            val canBt = !lockDataJson.isNullOrBlank() && !lockMac.isNullOrBlank() && !BuildConfig.BYPASS_LOGIN
            if (canBt) {
                val sd = isoToMillisOrNull(newValidFromIso)
                val ed = isoToMillisOrNull(newValidToIso)
                Log.d("PasscodeVM", "✏️ Modify (BLE) start=$sd end=$ed mac=$lockMac")
                TTLockManager(context).modifyPasscodeSuspend(
                    originalCode = originalCode,
                    newCode = newCode,
                    startDate = sd,
                    endDate = ed,
                    lockDataJson = lockDataJson,
                    lockMac = lockMac
                ).getOrThrow()
            } else {
                Log.w("PasscodeVM", "⚠️ Modify sin BLE — solo backend")
            }

            val body = UpdatePasscodeRequest(
                name = null,
                validFrom = newValidFromIso,
                validTo = newValidToIso,
                receiverUserId = null
            )
            Log.d("PasscodeVM", "➡️ PATCH /passcodes/{id} id=$passcodeId body=$body")
            val resp = api(context).updatePasscode(passcodeId, body)
            if (!resp.isSuccessful) throw RuntimeException("Backend update HTTP ${resp.code()}")

            runCatching { listPasscodes(context, page = _page.value) }
            _state.value = _state.value.copy(isLoading = false, modal = PasscodeUiModal.Success("Código modificado"))
        } catch (e: Exception) {
            Log.e("PasscodeVM", "❌ modify", e)
            _state.value = _state.value.copy(isLoading = false, modal = PasscodeUiModal.Error("Error al modificar: ${e.message}"))
        }
    }

    suspend fun createBookingPasscode(
        context: Context,
        code: String,
        name: String? = null
    ): Result<PasscodeDto> {
        return try {
            val api: PasscodeApi = RetrofitInstance.getPasscodeApi(context)
            val resp = api.createBookingPasscode(CreateBookingPasscodeRequest(code, name))
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Respuesta vacía"))
            } else {
                Result.failure(Exception("HTTP ${resp.code()} ${resp.errorBody()?.string().orEmpty()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todos los passcodes creados en una reserva.
     * GET /passcodes/booking/:bookingId?includeDeleted={true|false}
     */
    suspend fun getBookingPasscodes(
        context: Context,
        bookingId: String,
        includeDeleted: Boolean = false
    ): Result<List<PasscodeDto>> {
        return try {
            val api: PasscodeApi = RetrofitInstance.getPasscodeApi(context)
            val resp = api.getPasscodesForBooking(bookingId, includeDeleted)
            if (resp.isSuccessful) {
                Result.success(resp.body().orEmpty())
            } else {
                Result.failure(Exception("HTTP ${resp.code()} ${resp.errorBody()?.string().orEmpty()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun dismissModal() {
        _state.value = _state.value.copy(modal = PasscodeUiModal.None)
    }

    /* -------- paginación -------- */
    fun nextPage(context: Context, limit: Int = 20, lockId: String? = null, status: String? = null, q: String? = null, activeOnly: Boolean? = null, clubId: String? = null) {
        val current = _page.value; val max = _pages.value
        if (current < max) listPasscodes(context, page = current + 1, limit = limit, lockId = lockId, status = status, q = q, activeOnly = activeOnly, clubId = clubId)
    }
    fun prevPage(context: Context, limit: Int = 20, lockId: String? = null, status: String? = null, q: String? = null, activeOnly: Boolean? = null, clubId: String? = null) {
        val current = _page.value
        if (current > 1) listPasscodes(context, page = current - 1, limit = limit, lockId = lockId, status = status, q = q, activeOnly = activeOnly, clubId = clubId)
    }
}