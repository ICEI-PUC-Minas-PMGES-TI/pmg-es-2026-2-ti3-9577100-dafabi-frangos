package com.dafabi.suppliers;

import com.dafabi.suppliers.dto.CreateSupplierRequest;
import com.dafabi.suppliers.dto.UpdateSupplierRequest;
import com.dafabi.suppliers.dto.UpdateSupplierStatusRequest;
import com.dafabi.suppliers.repository.SupplierRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupplierControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupplierRepository supplierRepository;

    @BeforeEach
    void setUp() {
        supplierRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /suppliers deve cadastrar fornecedor e retornar 201 Created")
    void shouldCreateSupplierSuccessfully() throws Exception {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "Granja Santa Clara",
                "(31) 99812-4400",
                "contato@granjasantaclara.com.br",
                "Entregas às terças e sextas.",
                true
        );

        mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name", is("Granja Santa Clara")))
                .andExpect(jsonPath("$.phone", is("(31) 99812-4400")))
                .andExpect(jsonPath("$.email", is("contato@granjasantaclara.com.br")))
                .andExpect(jsonPath("$.notes", is("Entregas às terças e sextas.")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("POST /suppliers deve retornar 409 Conflict quando e-mail já estiver cadastrado")
    void shouldReturn409WhenEmailIsDuplicated() throws Exception {
        CreateSupplierRequest first = new CreateSupplierRequest(
                "Granja Santa Clara",
                "(31) 99812-4400",
                "contato@granjasantaclara.com.br",
                null,
                true
        );

        mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        CreateSupplierRequest duplicate = new CreateSupplierRequest(
                "Outra Granja",
                "(31) 98888-0000",
                "CONTATO@GRANJASANTACLARA.COM.BR",
                null,
                true
        );

        mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_EMAIL")));
    }

    @Test
    @DisplayName("POST /suppliers deve retornar 400 Bad Request para dados inválidos")
    void shouldReturn400WhenRequestIsInvalid() throws Exception {
        CreateSupplierRequest invalid = new CreateSupplierRequest(
                "AB",
                "",
                "email-invalido",
                null,
                true
        );

        mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_DATA")));
    }

    @Test
    @DisplayName("GET /suppliers deve listar fornecedores e permitir filtro por query e status ativo")
    void shouldListAndFilterSuppliers() throws Exception {
        CreateSupplierRequest s1 = new CreateSupplierRequest(
                "Distribuidora Bom Sabor",
                "(31) 3344-7788",
                "vendas@bomsabor.com.br",
                null,
                true
        );
        CreateSupplierRequest s2 = new CreateSupplierRequest(
                "Granja Santa Clara",
                "(31) 99812-4400",
                "contato@granjasantaclara.com.br",
                "Entregas às terças e sextas.",
                false
        );

        mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(s1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(s2)))
                .andExpect(status().isCreated());

        // Listagem completa ordenada por nome
        mockMvc.perform(get("/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Distribuidora Bom Sabor")))
                .andExpect(jsonPath("$[1].name", is("Granja Santa Clara")));

        // Filtro por active=true
        mockMvc.perform(get("/suppliers").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Distribuidora Bom Sabor")));

        // Filtro por query
        mockMvc.perform(get("/suppliers").param("query", "santa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Granja Santa Clara")));
    }

    @Test
    @DisplayName("GET /suppliers/{id} e PUT /suppliers/{id} devem consultar e editar fornecedor com sucesso")
    void shouldGetAndUpdateSupplier() throws Exception {
        CreateSupplierRequest createReq = new CreateSupplierRequest(
                "Granja Santa Clara",
                "(31) 99812-4400",
                "contato@granjasantaclara.com.br",
                "Nota inicial",
                true
        );

        String responseJson = mockMvc.perform(post("/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseJson).get("id").asText();

        mockMvc.perform(get("/suppliers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.name", is("Granja Santa Clara")));

        UpdateSupplierRequest updateReq = new UpdateSupplierRequest(
                "Granja Santa Clara Ltda",
                "(31) 99812-5555",
                "financeiro@granjasantaclara.com.br",
                "Entregas de segunda a sexta.",
                false
        );

        mockMvc.perform(put("/suppliers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.name", is("Granja Santa Clara Ltda")))
                .andExpect(jsonPath("$.phone", is("(31) 99812-5555")))
                .andExpect(jsonPath("$.email", is("financeiro@granjasantaclara.com.br")))
                .andExpect(jsonPath("$.notes", is("Entregas de segunda a sexta.")))
                .andExpect(jsonPath("$.active", is(false)));

        UpdateSupplierStatusRequest statusReq = new UpdateSupplierStatusRequest(true);
        mockMvc.perform(patch("/suppliers/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("GET /suppliers/{id} deve retornar 404 Not Found quando fornecedor não existir")
    void shouldReturn404WhenSupplierDoesNotExist() throws Exception {
        mockMvc.perform(get("/suppliers/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }
}
