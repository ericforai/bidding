# Fee Management Module (费用管理模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

处理投标项目的所有费用相关操作，包括投标保证金、服务费、文档费、差旅费、公证费等。

## Package Structure
```
com.xiyu.bid.fees/
├── entity/
│   └── Fee.java              # Fee entity with JPA mappings
├── repository/
│   └── FeeRepository.java    # Data access layer
├── dto/
│   ├── FeeDTO.java           # Data transfer object
│   ├── FeeCreateRequest.java # Create request DTO
│   ├── FeeUpdateRequest.java # Update request DTO
│   └── FeeStatisticsDTO.java # Statistics DTO
├── service/
│   └── FeeService.java       # Business logic layer
└── controller/
    └── FeeController.java    # REST API endpoints
```

## Entity: Fee

### Fields
- `id` - Primary key
- `projectId` - Associated project ID
- `feeType` - Fee type (BID_BOND, SERVICE_FEE, DOCUMENT_FEE, TRAVEL_FEE, NOTARY_FEE, OTHER_FEE)
- `amount` - Fee amount (BigDecimal)
- `feeDate` - Fee date
- `status` - Fee status (PENDING, PAID, RETURNED, CANCELLED)
- `paymentDate` - Payment date
- `returnDate` - Return date
- `paidBy` - Payer identifier
- `returnTo` - Return account identifier
- `remarks` - Additional notes
- `createdAt` - Creation timestamp
- `updatedAt` - Last update timestamp

### Database Indexes
- `idx_fee_project` - on projectId
- `idx_fee_status` - on status
- `idx_fee_type` - on feeType
- `idx_fee_project_status` - composite on projectId and status

## API Endpoints

### Create Fee
```
POST /api/fees
Roles: ADMIN, MANAGER
```

### Get All Fees (Paginated)
```
GET /api/fees?page=0&size=10&sortBy=createdAt&sortDir=desc
Roles: ADMIN, MANAGER, STAFF
```

### Get Fee by ID
```
GET /api/fees/{id}
Roles: ADMIN, MANAGER, STAFF
```

### Get Fees by Project ID
```
GET /api/fees/project/{projectId}
Roles: ADMIN, MANAGER, STAFF
```

### Get Fees by Status
```
GET /api/fees/status/{status}
Roles: ADMIN, MANAGER, STAFF
```

### Update Fee
```
PUT /api/fees/{id}
Roles: ADMIN, MANAGER
```

### Delete Fee
```
DELETE /api/fees/{id}
Roles: ADMIN
```

### Mark Fee as Paid
```
POST /api/fees/{id}/pay?paidBy={userId}
Roles: ADMIN, MANAGER
```

### Mark Fee as Returned
```
POST /api/fees/{id}/return?returnTo={accountId}
Roles: ADMIN, MANAGER
```

### Cancel Fee
```
POST /api/fees/{id}/cancel
Roles: ADMIN, MANAGER
```

### Get Fee Statistics
```
GET /api/fees/statistics?projectId={projectId}
Roles: ADMIN, MANAGER
```

## Status Transitions

```
PENDING → PAID       (markAsPaid)
PENDING → CANCELLED  (cancelFee)
PAID    → RETURNED   (markAsReturned)
```

## Validation Rules

### Create Fee
- `projectId` - Required
- `feeType` - Required
- `amount` - Required, must be > 0
- `feeDate` - Required
- `remarks` - Optional, max 1000 characters

### Update Fee
- `amount` - Optional, must be > 0 if provided
- `feeDate` - Optional
- `remarks` - Optional, max 1000 characters

### Business Rules
- Only PENDING or CANCELLED fees can be updated
- Only PENDING or CANCELLED fees can be deleted
- Only PENDING fees can be marked as paid
- Only PAID fees can be marked as returned
- Only PENDING fees can be cancelled

## Audit Logging

All fee operations are automatically logged via the `@Auditable` annotation:
- CREATE - When a new fee is created
- UPDATE - When a fee is updated
- DELETE - When a fee is deleted
- PAY - When a fee is marked as paid
- RETURN - When a fee is marked as returned
- CANCEL - When a fee is cancelled

## Statistics

The statistics endpoint provides:
- Total pending amount
- Total paid amount
- Total returned amount
- Total cancelled amount
- Grand total

## Testing

### Unit Tests
- `FeeServiceTest` - Service layer tests with mocked dependencies
- Tests cover:
  - CRUD operations
  - Status transitions
  - Validation
  - Business rules
  - Edge cases (null values, negative amounts, invalid transitions)

### Integration Tests
- `FeeControllerTest` - Controller layer tests
- Tests cover:
  - HTTP request/response
  - Authorization/role-based access
  - Request validation
  - Error handling

## Dependencies

### Internal
- `com.xiyu.bid.dto.ApiResponse` - API response wrapper
- `com.xiyu.bid.annotation.Auditable` - Audit logging annotation
- `com.xiyu.bid.service.AuditLogService` - Audit logging service
- `com.xiyu.bid.exception.ResourceNotFoundException` - Exception handling

### External
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- Lombok
- Jakarta Persistence API
- Jakarta Validation API

## Database Schema

```sql
CREATE TABLE fees (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    fee_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    fee_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_date TIMESTAMP,
    return_date TIMESTAMP,
    paid_by VARCHAR(200),
    return_to VARCHAR(200),
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_fee_project ON fees(project_id);
CREATE INDEX idx_fee_status ON fees(status);
CREATE INDEX idx_fee_type ON fees(fee_type);
CREATE INDEX idx_fee_project_status ON fees(project_id, status);
```

## Usage Example

```java
// Create a new fee
FeeCreateRequest request = FeeCreateRequest.builder()
    .projectId(100L)
    .feeType(Fee.FeeType.BID_BOND)
    .amount(new BigDecimal("50000.00"))
    .feeDate(LocalDateTime.now())
    .remarks("Bid bond for project XYZ")
    .build();

FeeDTO createdFee = feeService.createFee(request);

// Mark fee as paid
FeeDTO paidFee = feeService.markAsPaid(createdFee.getId(), "user123");

// Get statistics
FeeStatisticsDTO stats = feeService.getStatistics(100L);
System.out.println("Total Paid: " + stats.getTotalPaid());
```

## Future Enhancements

1. **Fee Reminders** - Automatic notifications for pending fees
2. **Fee Templates** - Pre-defined fee configurations
3. **Multi-currency Support** - Handle fees in different currencies
4. **Fee Approvals** - Workflow for fee approval
5. **Export to Excel** - Generate fee reports
6. **Fee History** - Track all fee changes over time
7. **Bulk Operations** - Create/update multiple fees at once
8. **Fee Categories** - Additional categorization and reporting
