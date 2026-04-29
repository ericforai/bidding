package com.xiyu.bid.security;

import com.xiyu.bid.enums.DataScopeType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DataScopeContextTest {

    @Test
    void testDataScopeContextExtension() {
        DataScopeContext context = DataScopeContext.builder()
                .scopeType(DataScopeType.SELF)
                .collaboratedProjectIds(List.of(100L, 200L))
                .crmAuthorizedCustomerIds(List.of("C001", "C002"))
                .build();
        
        assertEquals(2, context.getCollaboratedProjectIds().size());
        assertEquals(2, context.getCrmAuthorizedCustomerIds().size());
    }
}
