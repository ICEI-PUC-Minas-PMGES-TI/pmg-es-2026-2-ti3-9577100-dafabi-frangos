@Test
@DisplayName("Deve listar histórico de vendas com paginação e filtros")
void shouldListSalesWithPaginationAndFilters() {
    // Arrange
    OffsetDateTime startDate = OffsetDateTime.now().minusDays(1);
    OffsetDateTime endDate = OffsetDateTime.now();
    Pageable pageable = PageRequest.of(0, 10);

    Sale mockSale = new Sale();
    // Configure mockSale properties se necessário

    Page<Sale> salesPage = new PageImpl<>(List.of(mockSale), pageable, 1);

    when(saleRepository.findAllWithFilters(
            eq(startDate),
            eq(endDate),
            eq(SaleStatus.COMPLETED),
            eq(PaymentMethod.PIX),
            eq(operatorId),
            any(Pageable.class)
    )).thenReturn(salesPage);

    // Act
    Page<SaleResponse> result = saleService.findAll(
            startDate,
            endDate,
            SaleStatus.COMPLETED,
            PaymentMethod.PIX,
            operatorId,
            pageable
    );

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent()).hasSize(1);
    verify(saleRepository).findAllWithFilters(startDate, endDate, SaleStatus.COMPLETED, PaymentMethod.PIX, operatorId, pageable);
}
