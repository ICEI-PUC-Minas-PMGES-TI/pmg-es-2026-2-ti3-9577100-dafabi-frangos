package com.dafabi.suppliers;

import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import com.dafabi.suppliers.application.SupplierService;
import com.dafabi.suppliers.domain.Supplier;
import com.dafabi.suppliers.dto.CreateSupplierRequest;
import com.dafabi.suppliers.dto.SupplierResponse;
import com.dafabi.suppliers.dto.UpdateSupplierRequest;
import com.dafabi.suppliers.mapper.SupplierMapper;
import com.dafabi.suppliers.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Spy
    private SupplierMapper supplierMapper = new SupplierMapper();

    @InjectMocks
    private SupplierService supplierService;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        storeId = Supplier.DEFAULT_STORE_ID;
    }

    @Test
    @DisplayName("Deve cadastrar fornecedor com sucesso")
    void shouldCreateSupplierSuccessfully() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "Granja Santa Clara",
                "(31) 99812-4400",
                "Contato@GranjaSantaClara.com.br",
                "Entregas às terças e sextas.",
                true
        );

        when(supplierRepository.existsByStoreIdAndEmailIgnoreCase(storeId, "contato@granjasantaclara.com.br"))
                .thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierResponse response = supplierService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Granja Santa Clara");
        assertThat(response.phone()).isEqualTo("(31) 99812-4400");
        assertThat(response.email()).isEqualTo("contato@granjasantaclara.com.br");
        assertThat(response.notes()).isEqualTo("Entregas às terças e sextas.");
        assertThat(response.active()).isTrue();

        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    @DisplayName("Deve falhar ao cadastrar fornecedor com e-mail duplicado na mesma loja")
    void shouldFailWhenEmailAlreadyExists() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "Distribuidora Bom Sabor",
                "(31) 3344-7788",
                "vendas@bomsabor.com.br",
                null,
                true
        );

        when(supplierRepository.existsByStoreIdAndEmailIgnoreCase(storeId, "vendas@bomsabor.com.br"))
                .thenReturn(true);

        assertThatThrownBy(() -> supplierService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um fornecedor cadastrado com este e-mail.");

        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar fornecedor por ID com sucesso")
    void shouldFindSupplierByIdSuccessfully() {
        UUID supplierId = UUID.randomUUID();
        Supplier supplier = new Supplier(
                supplierId,
                storeId,
                "Granja Santa Clara",
                "(31) 99812-4400",
                "contato@granjasantaclara.com.br",
                "Entregas regulares",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(supplier));

        SupplierResponse response = supplierService.findById(supplierId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(supplierId);
        assertThat(response.name()).isEqualTo("Granja Santa Clara");
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar fornecedor inexistente")
    void shouldThrowExceptionWhenSupplierNotFound() {
        UUID supplierId = UUID.randomUUID();
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.findById(supplierId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fornecedor");
    }

    @Test
    @DisplayName("Deve atualizar fornecedor com sucesso")
    void shouldUpdateSupplierSuccessfully() {
        UUID supplierId = UUID.randomUUID();
        Supplier existing = new Supplier(
                supplierId,
                storeId,
                "Granja Antiga",
                "(31) 3333-0000",
                "antigo@granja.com.br",
                null,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        UpdateSupplierRequest request = new UpdateSupplierRequest(
                "Granja Nova Era",
                "(31) 99999-1111",
                "novo@granja.com.br",
                "Atendimento matutino",
                false
        );

        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByStoreIdAndEmailIgnoreCaseAndIdNot(storeId, "novo@granja.com.br", supplierId))
                .thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierResponse response = supplierService.update(supplierId, request);

        assertThat(response.name()).isEqualTo("Granja Nova Era");
        assertThat(response.phone()).isEqualTo("(31) 99999-1111");
        assertThat(response.email()).isEqualTo("novo@granja.com.br");
        assertThat(response.notes()).isEqualTo("Atendimento matutino");
        assertThat(response.active()).isFalse();
    }

    @Test
    @DisplayName("Deve falhar ao atualizar fornecedor com e-mail pertencente a outro fornecedor")
    void shouldFailUpdateWhenEmailBelongsToAnotherSupplier() {
        UUID supplierId = UUID.randomUUID();
        Supplier existing = new Supplier(
                supplierId,
                storeId,
                "Granja A",
                "(31) 3333-0000",
                "a@granja.com.br",
                null,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        UpdateSupplierRequest request = new UpdateSupplierRequest(
                "Granja A",
                "(31) 3333-0000",
                "b@granja.com.br",
                null,
                true
        );

        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByStoreIdAndEmailIgnoreCaseAndIdNot(storeId, "b@granja.com.br", supplierId))
                .thenReturn(true);

        assertThatThrownBy(() -> supplierService.update(supplierId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um fornecedor cadastrado com este e-mail.");

        verify(supplierRepository, never()).save(any());
    }
}
