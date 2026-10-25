package com.uniquindio.ecommerce.infrastructure.rest.controller;
import com.uniquindio.ecommerce.application.dto.request.*;
import com.uniquindio.ecommerce.application.dto.response.*;
import com.uniquindio.ecommerce.application.usecase.*;
import com.uniquindio.ecommerce.infrastructure.rest.mapper.UsuarioMapper;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping(value="/api/auth",produces=MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name="Autenticación")
public class AuthController {
    private final RegistrarUsuarioUseCase registrar;
    private final IniciarSesionUseCase login;
    private final UsuarioMapper mapper;
    @PostMapping("/register")
    @Operation(summary="Registrar comprador o vendedor",description="La cuenta genera su UUID. Ese ID es creadorId para VENDEDOR y clienteId para COMPRADOR. Contraseña de al menos 12 caracteres y máximo 72 bytes UTF-8.")
    @ApiResponses({
        @ApiResponse(responseCode="201",description="Usuario creado sin exponer contraseña ni hash",content=@Content(schema=@Schema(implementation=UsuarioResponse.class))),
        @ApiResponse(responseCode="400",description="Datos o rol inválidos",content=@Content(schema=@Schema(implementation=ErrorResponse.class))),
        @ApiResponse(responseCode="409",description="Correo duplicado",content=@Content(schema=@Schema(implementation=ErrorResponse.class)))})
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegistroRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(registrar.ejecutar(r.nombre(),r.correo(),r.password(),r.rol())));
    }
    @PostMapping("/login")
    @Operation(summary="Iniciar sesión",description="Devuelve JWT firmado, Bearer, expiración y usuario. Copia únicamente token en Authorize.")
    @ApiResponses({
        @ApiResponse(responseCode="200",description="Autenticación correcta",content=@Content(schema=@Schema(implementation=LoginResponse.class))),
        @ApiResponse(responseCode="400",description="Datos inválidos",content=@Content(schema=@Schema(implementation=ErrorResponse.class))),
        @ApiResponse(responseCode="401",description="Credenciales inválidas o cuenta inactiva",content=@Content(schema=@Schema(implementation=ErrorResponse.class)))})
    public LoginResponse login(@Valid @RequestBody LoginRequest r) { return login.ejecutar(r.correo(),r.password()); }
}
