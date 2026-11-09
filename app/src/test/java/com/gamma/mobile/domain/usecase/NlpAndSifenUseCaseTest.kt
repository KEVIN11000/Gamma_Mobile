package com.gamma.mobile.domain.usecase

import com.gamma.mobile.data.remote.BaseFakeGammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.ParseMovementRequestDto
import com.gamma.mobile.data.remote.dto.ParsedMovementDto
import com.gamma.mobile.data.remote.dto.SifenRucRequestDto
import com.gamma.mobile.data.remote.dto.SifenRucResponseDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class NlpAndSifenUseCaseTest {

    private lateinit var fakeApi: FakeNlpSifenApi
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var parseUseCase: ParseMovimientoNlpUseCase
    private lateinit var sifenUseCase: ConsultarRucSifenUseCase

    @Before
    fun setup() {
        fakeApi = FakeNlpSifenApi()
        parseUseCase = ParseMovimientoNlpUseCase(
            api = fakeApi,
            dispatcher = testDispatcher
        )
        sifenUseCase = ConsultarRucSifenUseCase(
            api = fakeApi,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun `test parseMovimientoNlp with blank text returns error without api call`() = runTest(testDispatcher) {
        val result = parseUseCase("   ")
        assertTrue(result is NetworkResult.Error)
        assertEquals(400, (result as NetworkResult.Error).code)
        assertFalse(fakeApi.parseCalled)
    }

    @Test
    fun `test parseMovimientoNlp success maps DTO to domain correctly`() = runTest(testDispatcher) {
        fakeApi.parseResponse = Response.success(
            ParsedMovementDto(
                valido = true,
                monto = 45000L,
                moneda = "PYG",
                tipoMovimiento = "Gasto",
                categoria = "Combustible",
                proveedorCliente = "Shell",
                descripcion = "Carga nafta",
                confianza = 0.98
            )
        )

        val result = parseUseCase("Gasto 45000 nafta shell")
        assertTrue(result is NetworkResult.Success)
        val domain = (result as NetworkResult.Success).data
        assertTrue(domain.valido)
        assertEquals(45000L, domain.monto)
        assertEquals("Gasto", domain.tipoMovimiento)
        assertEquals("Combustible", domain.categoria)
        assertEquals("Shell", domain.proveedorCliente)
        assertEquals(0.98, domain.confianza, 0.001)
        assertEquals("Gasto 45000 nafta shell", fakeApi.lastParseRequest?.texto)
    }

    @Test
    fun `test parseMovimientoNlp error returns NetworkResult Error`() = runTest(testDispatcher) {
        fakeApi.parseResponse = Response.error(500, "Internal Server Error".toResponseBody(null))
        val result = parseUseCase("Algo que falla")
        assertTrue(result is NetworkResult.Error)
        assertEquals(500, (result as NetworkResult.Error).code)
    }

    @Test
    fun `test consultarRucSifen with blank ruc returns error without api call`() = runTest(testDispatcher) {
        val result = sifenUseCase("")
        assertTrue(result is NetworkResult.Error)
        assertEquals(400, (result as NetworkResult.Error).code)
        assertFalse(fakeApi.sifenCalled)
    }

    @Test
    fun `test consultarRucSifen success maps DTO to domain correctly`() = runTest(testDispatcher) {
        fakeApi.sifenResponse = Response.success(
            SifenRucResponseDto(
                ruc = "80012345-6",
                razonSocial = "PETROPAR S.A.",
                estado = "ACTIVO"
            )
        )

        val result = sifenUseCase("80012345-6")
        assertTrue(result is NetworkResult.Success)
        val domain = (result as NetworkResult.Success).data
        assertEquals("80012345-6", domain.ruc)
        assertEquals("PETROPAR S.A.", domain.razonSocial)
        assertEquals("ACTIVO", domain.estado)
        assertEquals("80012345-6", fakeApi.lastSifenRequest?.ruc)
    }

    @Test
    fun `test consultarRucSifen not found returns NetworkResult Error`() = runTest(testDispatcher) {
        fakeApi.sifenResponse = Response.error(404, "RUC no encontrado".toResponseBody(null))
        val result = sifenUseCase("99999999-9")
        assertTrue(result is NetworkResult.Error)
        assertEquals(404, (result as NetworkResult.Error).code)
    }

    private class FakeNlpSifenApi : BaseFakeGammaApi() {
        var parseResponse: Response<ParsedMovementDto>? = null
        var parseCalled = false
        var lastParseRequest: ParseMovementRequestDto? = null

        var sifenResponse: Response<SifenRucResponseDto>? = null
        var sifenCalled = false
        var lastSifenRequest: SifenRucRequestDto? = null

        override suspend fun parseMovimientoNlp(request: ParseMovementRequestDto): Response<ParsedMovementDto> {
            parseCalled = true
            lastParseRequest = request
            return parseResponse ?: throw IllegalStateException("parseResponse not configured")
        }

        override suspend fun consultarRucSifen(request: SifenRucRequestDto): Response<SifenRucResponseDto> {
            sifenCalled = true
            lastSifenRequest = request
            return sifenResponse ?: throw IllegalStateException("sifenResponse not configured")
        }
    }
}
