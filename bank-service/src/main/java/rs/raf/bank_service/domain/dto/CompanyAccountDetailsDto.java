package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.AccountType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyAccountDetailsDto extends AccountDetailsDto {

    private String companyName;
    private String registrationNumber;
    private String taxId;
    private String address;
    private AuthorizedPersonelDto authorizedPerson;

    public CompanyAccountDetailsDto(String name, String accountNumber, AccountType accountType, BigDecimal availableBalance,
                                    BigDecimal reservedFunds, BigDecimal balance, AuthorizedPersonelDto authorizedPerson, String currencyCode) {
        super(name, accountNumber, accountType, availableBalance, reservedFunds, balance, currencyCode);
        this.authorizedPerson = authorizedPerson;
    }
}
